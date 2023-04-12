import React, {FunctionComponent} from "react";
import {Card, CardHeader, IconButton} from "@mui/material";
import {EmailDetailsProperties} from "./email-details-properties";
import {EmailCardContent} from "./email-card-content";
import OpenInFullIcon from '@mui/icons-material/OpenInFull';
import {NavLink} from "react-router-dom";

export const EmailCard: FunctionComponent<EmailDetailsProperties> = ({email}) => {
    return (
        <Card variant="outlined">
            <CardHeader title={`Email ${email.id}`} action={
                <IconButton aria-label="open" component={NavLink} to={`/emails/${email.id}`}>
                    <OpenInFullIcon />
                </IconButton>
            }/>
            <EmailCardContent email={email} />
        </Card>
    );
}
