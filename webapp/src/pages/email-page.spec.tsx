import * as React from 'react'
import {screen} from '@testing-library/react'
import '@testing-library/jest-dom'
import {EmailPage} from "./email-page";
import {renderWithProviders} from "../tests/utils";

describe('EmailPage', () => {
    it('render alert when no email is provided', () => {
        renderWithProviders(<EmailPage />);

        expect(screen.getByText("Email with ID undefined does not exist.")).toBeInTheDocument()
    })
})