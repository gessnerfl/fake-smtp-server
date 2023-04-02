import React from "react";
import AppBar from '@mui/material/AppBar';
import Box from '@mui/material/Box';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import './navigation.scss'

function Navigation() {
    return (
        <Box sx={{flexGrow: 1}}>
            <AppBar position="static">
                <Toolbar>
                    <Typography variant="h6" component="div">FakeSMTPServer</Typography>
                </Toolbar>
            </AppBar>
        </Box>
    );
}

export default Navigation;