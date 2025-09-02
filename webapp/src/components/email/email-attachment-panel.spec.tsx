import '@testing-library/jest-dom';
import { render, screen } from "@testing-library/react";
import { testEmailWithAttachment } from "../../setupTests";
import { EmailAttachmentPanel } from "./email-attachment-panel";

describe('EmailAttachmentPanel', () => {
    it('render all attachments', () => {
        render(<EmailAttachmentPanel email={testEmailWithAttachment}/>);

        expect(screen.getByText("Attachments")).toBeInTheDocument()
        testEmailWithAttachment.attachments.forEach(e => {
            expect(screen.getByText(e.filename)).toBeInTheDocument()
        })
    })
})
