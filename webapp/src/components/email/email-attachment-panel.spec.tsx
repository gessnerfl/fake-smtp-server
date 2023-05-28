import {render, screen} from "@testing-library/react";
import {testEmail1, testEmailWithAttachment} from "../../setupTests";
import * as React from "react";
import '@testing-library/jest-dom'
import {EmailAttachmentPanel} from "./email-attachment-panel";

describe('EmailAttachmentPanel', () => {
    it('render all attachments', () => {
        render(<EmailAttachmentPanel email={testEmailWithAttachment}/>);

        expect(screen.getByText("Attachments")).toBeInTheDocument()
        testEmailWithAttachment.attachments.forEach(e => {
            expect(screen.getByText(e.filename)).toBeInTheDocument()
        })
    })
})