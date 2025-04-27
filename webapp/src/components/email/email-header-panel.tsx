import React, {FunctionComponent} from "react";
import {EmailDetailsProperties} from "./email-details-properties";
import {Typography, Grid} from "@mui/material";
import {parseJSON, formatISO9075} from "date-fns";

export const EmailHeaderPanel: FunctionComponent<EmailDetailsProperties> = ({email}) => {
    return (
        <Grid container component='dl' spacing={2} className={"email-header"}>
            <Grid size={6}>
                <Typography component='dt' variant='subtitle2'>From:</Typography>
                <Typography component='dd' variant='body1'>{email.fromAddress}</Typography>
            </Grid>
            <Grid size={6}>
                <Typography component='dt' variant='subtitle2'>To:</Typography>
                <Typography component='dd' variant='body1'>{email.toAddress}</Typography>
            </Grid>
            <Grid size={6}>
                <Typography component='dt' variant='subtitle2'>ReceivedOn:</Typography>
                <Typography component='dd' variant='body1'>{formatISO9075(parseJSON(email.receivedOn))}</Typography>
            </Grid>
            <Grid size={12}>
                <Typography component='dt' variant='subtitle2'>Subject:</Typography>
                <Typography component='dd' variant='body1'>{email.subject}</Typography>
            </Grid>
        </Grid>
    )
}