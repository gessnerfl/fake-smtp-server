import {Route, Routes} from "react-router-dom";
import React from "react";
import Shell from "./pages/shell";
import ErrorPage from "./pages/error-page";
import EmailListPage from "./pages/email-list-page";
import {EmailPage} from "./pages/email-page";
import EmailEventSourceManager from "./components/EmailEventSourceManager";

function App() {
    return (
        <>
            <EmailEventSourceManager />

            <Routes>
                <Route path={"/"} element={<Shell/>} errorElement={<ErrorPage/>}>
                    <Route index={true} element={<EmailListPage/>}/>
                    <Route path={"emails/:id"} element={<EmailPage/>}/>
                </Route>
            </Routes>
        </>
    );
}

export default App;
