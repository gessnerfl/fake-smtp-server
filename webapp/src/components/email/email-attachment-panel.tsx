import React, {FunctionComponent} from "react";
import {EmailDetailsProperties} from "./email-details-properties";
import {Button, Divider} from "@mui/material";
import AttachFileIcon from '@mui/icons-material/AttachFile';
import {EmailAttachment} from "../../models/email";

export const EmailAttachmentPanel: FunctionComponent<EmailDetailsProperties> = ({email}) => {
    const renderAttachment = function (attachment: EmailAttachment) {
        return (
            <Button key={attachment.id}
                    style={{textTransform: 'none'}}
                    variant="text"
                    size="small"
                    startIcon={<AttachFileIcon/>}
                    href={`/api/emails/${email.id}/attachments/${attachment.id}`}>
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