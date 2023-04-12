import * as React from 'react'
import '@testing-library/jest-dom'
import {render, screen} from '@testing-library/react'
import {EmailHeaderPanel} from "./email-header-panel";
import {testEmail1} from "../../setupTests";

describe('EmailHeaderPanel', () => {
    it('render email header panel component', () => {
        render(<EmailHeaderPanel email={testEmail1} />);

        expect(screen.getByText(testEmail1.fromAddress)).toBeInTheDocument()
        expect(screen.getByText(testEmail1.toAddress)).toBeInTheDocument()
        expect(screen.getByText(testEmail1.subject)).toBeInTheDocument()
        expect(screen.getByText("2023-04-05 21:05:10")).toBeInTheDocument()
    })
})