import React, {FunctionComponent} from "react";
import {Card, CardContent, Typography} from "@mui/material";
import {EmailDetailsProperties} from "./email-details-properties";
import {EmailHeaderPanel} from "./email-header-panel";
import {EmailContentPanel} from "./email-content-panel";

export const EmailCard: FunctionComponent<EmailDetailsProperties> = (props) => {
    return (
        <Card variant="outlined">
            <CardContent>
                <Typography sx={{fontSize: 14}} color="text.secondary" gutterBottom>Email {props.email.id}</Typography>
                <EmailHeaderPanel email={props.email}/>
                <EmailContentPanel email={props.email}/>
            </CardContent>
        </Card>
    );
}
