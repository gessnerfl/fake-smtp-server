import React, {FunctionComponent} from "react";
import {EmailDetailsProperties} from "./email-details-properties";
import {Button, Divider, Tooltip} from "@mui/material";
import AttachFileIcon from '@mui/icons-material/AttachFile';
import {EmailAttachment} from "../../models/email";
import {getBasePath} from "../../base-path";

export const EmailAttachmentPanel: FunctionComponent<EmailDetailsProperties> = ({email}) => {
    const getBasePathString = () =>  {
        const path = getBasePath();
        return path ? path : ""
    }

    const renderAttachment = function (attachment: EmailAttachment) {
        const skipped = attachment.processingStatus === "SKIPPED_TOO_LARGE";
        const buttonLabel = skipped ? `${attachment.filename} (skipped)` : attachment.filename;
        const tooltipTitle = skipped
            ? (attachment.processingMessage ?? "Attachment was skipped because it exceeded the configured size limit.")
            : "";

        return (
            <Tooltip key={attachment.id} title={tooltipTitle} disableHoverListener={!skipped}>
                <span>
                    <Button
                        style={{textTransform: 'none'}}
                        variant="text"
                        size="small"
                        startIcon={<AttachFileIcon/>}
                        disabled={skipped}
                        href={skipped ? undefined : `${getBasePathString()}/api/emails/${email.id}/attachments/${attachment.id}`}>
                        {buttonLabel}
                    </Button>
                </span>
            </Tooltip>
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
