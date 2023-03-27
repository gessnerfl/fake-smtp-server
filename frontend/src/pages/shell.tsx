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
