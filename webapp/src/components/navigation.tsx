import React, { useEffect, useRef, useState } from "react";
import AppBar from '@mui/material/AppBar';
import Box from '@mui/material/Box';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';
import Tooltip from '@mui/material/Tooltip';
import CircleIcon from '@mui/icons-material/Circle';
import './navigation.scss'
import {useGetMetaDataQuery, useLogoutMutation} from "../store/rest-api";
import { useSelector, useDispatch } from 'react-redux';
import { RootState } from '../store/store';
import { clearAuthentication } from '../store/auth-slice';

type ConnectionStatus = 'connected' | 'disconnected' | 'reconnecting';
type EventType = 'ping' | 'email' | null;
const CONNECTION_POLL_INTERVAL_MS = 1000;

function Navigation() {
    const {data} = useGetMetaDataQuery();
    const { isAuthenticated } = useSelector((state: RootState) => state.auth);
    const dispatch = useDispatch();
    const [logout] = useLogoutMutation();
    const [connectionStatus, setConnectionStatus] = useState<ConnectionStatus>('disconnected');
    const [lastPingSeconds, setLastPingSeconds] = useState<number>(0);

    // Animation state
    const [isPulsing, setIsPulsing] = useState(false);
    const [lastEventType, setLastEventType] = useState<EventType>(null);
    const lastEventRef = useRef<number>(0);

    // Monitor connection status and ping timestamp
    useEffect(() => {
        const updateConnectionStatus = (nextStatus: ConnectionStatus) => {
            setConnectionStatus((previousStatus) => previousStatus === nextStatus ? previousStatus : nextStatus);
        };

        const updateLastPingSeconds = (nextLastPingSeconds: number) => {
            setLastPingSeconds((previousLastPingSeconds) => previousLastPingSeconds === nextLastPingSeconds
                ? previousLastPingSeconds
                : nextLastPingSeconds);
        };

        const interval = setInterval(() => {
            const eventSource = window.emailEventSource;
            if (!eventSource) {
                updateConnectionStatus('disconnected');
                updateLastPingSeconds(0);
                return;
            }

            // Check connection state
            if (eventSource.readyState === 1) { // OPEN
                updateConnectionStatus('connected');
            } else if (eventSource.readyState === 0) { // CONNECTING
                updateConnectionStatus('reconnecting');
            } else {
                updateConnectionStatus('disconnected');
            }

            // Calculate seconds since last ping from global timestamp
            const lastPingTimestamp = window.sseLastPingTimestamp;
            if (lastPingTimestamp) {
                const secondsSincePing = Math.floor((Date.now() - lastPingTimestamp) / 1000);
                updateLastPingSeconds(secondsSincePing);
            } else {
                updateLastPingSeconds(0);
            }

            // Check for new events to trigger animation
            const currentEventTimestamp = window.sseLastEventTimestamp;
            const currentEventType = window.sseLastEventType;

            if (currentEventTimestamp && currentEventTimestamp !== lastEventRef.current) {
                lastEventRef.current = currentEventTimestamp;
                setLastEventType(currentEventType || 'ping');
                setIsPulsing(true);

                // Reset animation after it completes
                const animationDuration = currentEventType === 'email' ? 800 : 400;
                setTimeout(() => {
                    setIsPulsing(false);
                }, animationDuration);
            }
        }, CONNECTION_POLL_INTERVAL_MS);

        return () => clearInterval(interval);
    }, []);

    const handleLogout = () => {
        logout()
            .unwrap()
            .catch(() => undefined)
            .finally(() => dispatch(clearAuthentication()));
    };

    const getStatusColor = () => {
        switch (connectionStatus) {
            case 'connected': return 'success';
            case 'reconnecting': return 'warning';
            case 'disconnected': return 'error';
        }
    };

    const getStatusText = () => {
        const eventLabel = lastEventType === 'email' ? ' (New email!)' : '';
        switch (connectionStatus) {
            case 'connected': return `Connected (last ping: ${lastPingSeconds}s ago)${eventLabel}`;
            case 'reconnecting': return 'Reconnecting...';
            case 'disconnected': return 'Disconnected';
        }
    };

    const showConnectionIndicator = data && (!data.authenticationEnabled || isAuthenticated);

    // Determine animation parameters based on event type
    const isEmailEvent = lastEventType === 'email';
    const animationName = isEmailEvent ? 'pulse-email' : 'pulse-ping';
    const animationDuration = isEmailEvent ? '0.8s' : '0.4s';

    return (
        <Box sx={{flexGrow: 1}}>
            <AppBar position="static">
                <Toolbar>
                    <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>FakeSMTPServer</Typography>

                    {showConnectionIndicator && (
                        <Tooltip title={getStatusText()}>
                            <Box sx={{ mr: 2, display: 'flex', alignItems: 'center' }}>
                                <CircleIcon
                                    color={getStatusColor()}
                                    sx={{
                                        fontSize: 12,
                                        color: connectionStatus === 'connected' ? '#39ff14' : undefined,
                                        animation: isPulsing && connectionStatus === 'connected'
                                            ? `${animationName} ${animationDuration} ease-in-out`
                                            : 'none',
                                        '@keyframes pulse-ping': {
                                            '0%': {
                                                transform: 'scale(1)',
                                                opacity: 1,
                                                filter: 'drop-shadow(0 0 0 rgba(0, 255, 0, 0.4))'
                                            },
                                            '50%': {
                                                transform: 'scale(1.4)',
                                                opacity: 0.7,
                                                filter: 'drop-shadow(0 0 16px rgba(0, 255, 0, 0.6))'
                                            },
                                            '100%': {
                                                transform: 'scale(1)',
                                                opacity: 1,
                                                filter: 'drop-shadow(0 0 0 rgba(0, 255, 0, 0.4))'
                                            },
                                        },
                                        '@keyframes pulse-email': {
                                            '0%': {
                                                transform: 'scale(1)',
                                                opacity: 1,
                                                filter: 'drop-shadow(0 0 0 rgba(0, 255, 0, 0.7))'
                                            },
                                            '50%': {
                                                transform: 'scale(2.2)',
                                                opacity: 0.6,
                                                filter: 'drop-shadow(0 0 40px rgba(0, 255, 0, 1))'
                                            },
                                            '100%': {
                                                transform: 'scale(1)',
                                                opacity: 1,
                                                filter: 'drop-shadow(0 0 0 rgba(0, 255, 0, 0.7))'
                                            },
                                        },
                                    }}
                                />
                            </Box>
                        </Tooltip>
                    )}

                    <Typography variant="overline" component="div" sx={{ mr: 2 }}><span>Version:&nbsp;</span><span>{data?.version}</span></Typography>
                    {data?.authenticationEnabled && isAuthenticated && (
                        <Button color="inherit" onClick={handleLogout}>Logout</Button>
                    )}
                </Toolbar>
            </AppBar>
        </Box>
    );
}

export default Navigation;
