import React from 'react';
import './shell.scss';
import Navigation from "../components/navigation";
import {Outlet} from "react-router-dom";
import {Alert, Button, Container, createTheme, CssBaseline, ThemeProvider} from "@mui/material";
import {lightBlue, orange, yellow} from "@mui/material/colors";
import { useDispatch, useSelector } from 'react-redux';
import { RootState } from '../store/store';
import Login from '../components/login';
import { useGetMetaDataQuery } from '../store/rest-api';
import { setAuthenticated, setAuthenticationRequired } from '../store/auth-slice';

const lightTheme = createTheme({
    palette: {
        mode: 'light',
        primary: {
            main: lightBlue[900],
            light: lightBlue[800],
            dark: lightBlue["A100"],
        },
        secondary: {
            main: yellow[800],
            light: yellow[600],
            dark: yellow[900],
        },
        warning: orange
    },
    breakpoints: {
        values: {
            xs: 0,
            sm: 600,
            md: 900,
            lg: 1200,
            xl: 1750,
        },
    },
});

function Shell() {
    const { isAuthenticated } = useSelector((state: RootState) => state.auth);
    const dispatch = useDispatch();
    const { data, isLoading, isError, refetch } = useGetMetaDataQuery();

    React.useEffect(() => {
        if (data) {
            dispatch(setAuthenticationRequired(data.authenticationEnabled));
            if (typeof data.authenticated === "boolean") {
                dispatch(setAuthenticated(data.authenticated));
            }
        }
    }, [data, dispatch]);

    const authenticationEnabled = data?.authenticationEnabled ?? false;
    const showLoading = isLoading;
    const showError = !showLoading && isError;
    const showLogin = !showLoading && !showError && authenticationEnabled && !isAuthenticated;
    const showContent = !showLoading && !showError && (!authenticationEnabled || isAuthenticated);

    return (
        <ThemeProvider theme={lightTheme}>
            <CssBaseline/>
            <div className="Shell">
                <Navigation/>
                {showLoading && (
                    <Container className="content" maxWidth="xl">
                        Loading...
                    </Container>
                )}
                {showLogin && <Login />}
                {showError && (
                    <Container className="content" maxWidth="xl">
                        <Alert
                            severity="error"
                            action={(
                                <Button color="inherit" size="small" onClick={() => refetch()}>
                                    Retry
                                </Button>
                            )}
                        >
                            Unable to load application state. Retry to continue.
                        </Alert>
                    </Container>
                )}
                {showContent && (
                    <Container className="content" maxWidth="xl">
                        <Outlet/>
                    </Container>
                )}
            </div>
        </ThemeProvider>
    );
}

export default Shell;
