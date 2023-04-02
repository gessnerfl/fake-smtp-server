import React from "react";
import {Alert, AlertTitle, Card, CardHeader, IconButton} from "@mui/material";
import {NavLink, useParams} from "react-router-dom";
import {useGetEmailQuery} from "../stores/emails-api";
import {EmailCardContent} from "../components/email/email-card-content";
import CloseIcon from '@mui/icons-material/Close';

export const EmailPage = () => {
    const {id} = useParams<{ id: string }>();
    const {data} = useGetEmailQuery(id ? id : "-1")

    if (data) {
        return (
            <Card variant="outlined">
                <CardHeader title={`Email ${data.id}`} action={
                    <IconButton aria-label="close" component={NavLink} to={`/`}>
                        <CloseIcon />
                    </IconButton>
                }/>
                <EmailCardContent email={data} />
            </Card>
        );
    } else {
        return <Alert severity="error">
            <AlertTitle>Error</AlertTitle>
            Email with ID <strong>{id ? id : "undefined"}</strong> does not exist.
        </Alert>
    }

}
