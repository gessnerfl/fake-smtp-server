import React from 'react';
import './shell.scss';
import Navigation from "../components/navigation";
import {Outlet} from "react-router-dom";
import {Container, createTheme, CssBaseline, ThemeProvider} from "@mui/material";
import {lightBlue, orange, yellow} from "@mui/material/colors";

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
});

function Shell() {
    return (
        <ThemeProvider theme={lightTheme}>
            <CssBaseline/>
            <div className="Shell">
                <Navigation/>
                <Container className="content" maxWidth="xl">
                    <Outlet/>
                </Container>
            </div>
        </ThemeProvider>
    );
}

export default Shell;
