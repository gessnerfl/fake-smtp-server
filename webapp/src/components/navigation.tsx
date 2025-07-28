import React from "react";
import AppBar from '@mui/material/AppBar';
import Box from '@mui/material/Box';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';
import './navigation.scss'
import {useGetMetaDataQuery} from "../store/rest-api";
import { useSelector, useDispatch } from 'react-redux';
import { RootState } from '../store/store';
import { clearCredentials } from '../store/auth-slice';

function Navigation() {
    const {data} = useGetMetaDataQuery();
    const { isAuthenticated } = useSelector((state: RootState) => state.auth);
    const dispatch = useDispatch();

    const handleLogout = () => {
        dispatch(clearCredentials());
    };

    return (
        <Box sx={{flexGrow: 1}}>
            <AppBar position="static">
                <Toolbar>
                    <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>FakeSMTPServer</Typography>
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
