import * as React from 'react'
import '@testing-library/jest-dom'
import {render, screen} from '@testing-library/react'
import {testEmail1} from "../../setupTests";
import {EmailContentPanel} from "./email-content-panel";

describe('EmailContentPanel', () => {
    it('render email content panel component with raw data only', () => {
        render(<EmailContentPanel email={testEmail1} />);

        expect(screen.queryByLabelText("Html")).toBeNull()
        expect(screen.queryByLabelText("Plain")).toBeNull()
        expect(screen.getByLabelText("Raw")).toBeInTheDocument()
    })
    it('render email content panel component with html data only', () => {
        const email = {...testEmail1}
        email.contents = [{contentType: "html", data: "<p>test</p>"}]

        render(<EmailContentPanel email={email} />);

        expect(screen.getByLabelText("Html")).toBeInTheDocument()
        expect(screen.queryByLabelText("Plain")).toBeNull()
        expect(screen.getByLabelText("Raw")).toBeInTheDocument()
    })
    it('render email content panel component with plain data only', () => {
        const email = {...testEmail1}
        email.contents = [{contentType: "plain", data: "Some plain text"}]

        render(<EmailContentPanel email={email} />);

        expect(screen.queryByLabelText("Html")).toBeNull()
        expect(screen.getByLabelText("Plain")).toBeInTheDocument()
        expect(screen.getByLabelText("Raw")).toBeInTheDocument()
    })
})