import {CardContent} from "@mui/material";
import {EmailHeaderPanel} from "./email-header-panel";
import {EmailContentPanel} from "./email-content-panel";
import React, {FunctionComponent} from "react";
import {EmailDetailsProperties} from "./email-details-properties";
import "./email-card-content.scss"

export const EmailCardContent: FunctionComponent<EmailDetailsProperties> = ({email}) => {

    return (
        <CardContent className={"email-card"}>
            <EmailHeaderPanel email={email} />
            <EmailContentPanel email={email} />
        </CardContent>
    );

}