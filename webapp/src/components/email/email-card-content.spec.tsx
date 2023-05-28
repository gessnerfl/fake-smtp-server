import {render, screen} from "@testing-library/react";
import {testEmail1, testEmailWithAttachment} from "../../setupTests";
import * as React from "react";
import '@testing-library/jest-dom'
import {EmailCardContent} from "./email-card-content";

describe('EmailCardContent', () => {
    it('render email card content component without attachments', () => {
        render(<EmailCardContent email={testEmail1}/>);

        //header in document
        expect(screen.getByText(testEmail1.fromAddress)).toBeInTheDocument()
        //content in document
        expect(screen.getByLabelText("Raw")).toBeInTheDocument()
        expect(screen.queryByLabelText("Attachments")).toBeNull()
    })
    it('render email card content component with attachments', () => {
        render(<EmailCardContent email={testEmailWithAttachment}/>);

        //header in document
        expect(screen.getByText(testEmailWithAttachment.fromAddress)).toBeInTheDocument()
        //content in document
        expect(screen.getByLabelText("Raw")).toBeInTheDocument()
        expect(screen.getByText("Attachments")).toBeInTheDocument()
    })
})