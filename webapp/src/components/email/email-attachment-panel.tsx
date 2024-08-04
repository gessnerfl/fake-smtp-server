import React, {FunctionComponent} from "react";
import {EmailDetailsProperties} from "./email-details-properties";
import {Button, Divider} from "@mui/material";
import AttachFileIcon from '@mui/icons-material/AttachFile';
import {EmailAttachment} from "../../models/email";
import {getBasePath} from "../../utils";

export const EmailAttachmentPanel: FunctionComponent<EmailDetailsProperties> = ({email}) => {
    const getBasePathString = () =>  {
        const path = getBasePath();
        return path ? path : ""
    }

    const renderAttachment = function (attachment: EmailAttachment) {
        return (
            <Button key={attachment.id}
                    style={{textTransform: 'none'}}
                    variant="text"
                    size="small"
                    startIcon={<AttachFileIcon/>}
                    href={`${getBasePath()}/api/emails/${email.id}/attachments/${attachment.id}`}>
                {attachment.filename}
            </Button>
        )
    }

    return (
        <div className={"attachments"}>
            <Divider/>
            <h5>Attachments</h5>
            {email.attachments.map(renderAttachment)}
        </div>
    )
}