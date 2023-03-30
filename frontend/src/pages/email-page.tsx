import React from "react";
import {Alert, AlertTitle, Card, CardContent, Typography} from "@mui/material";
import {EmailHeaderPanel} from "../components/email/email-header-panel";
import {EmailContentPanel} from "../components/email/email-content-panel";
import {useParams} from "react-router-dom";
import {useGetEmailQuery} from "../stores/emails-api";

export const EmailPage = () => {
    const {id} = useParams<{ id: string }>();
    const {data} = useGetEmailQuery(id ? id : "-1")

    if (data) {
        return (
            <Card variant="outlined">
                <CardContent>
                    <Typography sx={{fontSize: 14}} color="text.secondary" gutterBottom>Email {data.id}</Typography>
                    <EmailHeaderPanel email={data}/>
                    <EmailContentPanel email={data}/>
                </CardContent>
            </Card>
        );
    } else {
        return <Alert severity="error">
            <AlertTitle>Error</AlertTitle>
            Email with ID <strong>{id ? id : "undefined"}</strong> does not exist.
        </Alert>
    }

}
