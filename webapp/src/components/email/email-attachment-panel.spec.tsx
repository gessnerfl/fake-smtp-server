import '@testing-library/jest-dom';
import { render, screen } from "@testing-library/react";
import { testEmailWithAttachment } from "../../setupTests";
import { EmailAttachmentPanel } from "./email-attachment-panel";
import { Email } from "../../models/email";

describe('EmailAttachmentPanel', () => {
    it('render all attachments', () => {
        render(<EmailAttachmentPanel email={testEmailWithAttachment}/>);

        expect(screen.getByText("Attachments")).toBeInTheDocument()
        testEmailWithAttachment.attachments.forEach(e => {
            expect(screen.getByText(e.filename)).toBeInTheDocument()
        })
    })

    it('render skipped attachment as disabled with suffix', () => {
        const emailWithSkippedAttachment: Email = {
            ...testEmailWithAttachment,
            attachments: [{
                id: 1,
                filename: "large.zip",
                processingStatus: "SKIPPED_TOO_LARGE",
                processingMessage: "SKIPPED_TOO_LARGE: Attachment exceeded limit"
            }]
        };

        render(<EmailAttachmentPanel email={emailWithSkippedAttachment}/>);

        const button = screen.getByText("large.zip (skipped)");
        expect(button).toBeInTheDocument();
        expect(button.closest("button")).toBeDisabled();
    })
})
