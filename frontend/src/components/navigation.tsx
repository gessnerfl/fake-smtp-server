import React from "react";
import AppBar from '@mui/material/AppBar';
import Box from '@mui/material/Box';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';
import InboxIcon from '@mui/icons-material/Inbox';
import {NavLink as RouterNavLink} from 'react-router-dom'
import './navigation.scss'

function Navigation() {
    return (
        <Box sx={{flexGrow: 1}}>
            <AppBar position="static">
                <Toolbar>
                    <Typography variant="h6" component="div">FakeSMTPServer</Typography>
                    <Button component={RouterNavLink} to="/" color="secondary" startIcon={<InboxIcon />}>Inbox</Button>
                </Toolbar>
            </AppBar>
        </Box>
    );
}

export default Navigation;