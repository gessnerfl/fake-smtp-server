import React from "react";
import AppBar from '@mui/material/AppBar';
import Box from '@mui/material/Box';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import './navigation.scss'
import {useGetMetaDataQuery} from "../store/rest-api";

function Navigation() {
    const {data} = useGetMetaDataQuery()

    return (
        <Box sx={{flexGrow: 1}}>
            <AppBar position="static">
                <Toolbar>
                    <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>FakeSMTPServer</Typography>
                    <Typography variant="overline" component="div"><span>Version:&nbsp;</span><span>{data?.version}</span></Typography>
                </Toolbar>
            </AppBar>
        </Box>
    );
}

export default Navigation;