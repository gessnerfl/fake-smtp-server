import * as React from 'react'
import {screen, waitFor} from '@testing-library/react'
import '@testing-library/jest-dom'
import {EmailPage} from "./email-page";
import {renderWithProviders} from "../test-utils";
import {MemoryRouter} from "react-router-dom";
import App from "../app";

describe('EmailPage', () => {
    it('render alert when no email id provided', () => {
        renderWithProviders(<EmailPage />);

        expect(screen.getByText("Email with ID undefined does not exist.")).toBeInTheDocument()
    })
    it('render alert when no email found for provided id', () => {
        renderWithProviders(<MemoryRouter initialEntries={["/emails/3"]}><App /></MemoryRouter>);

        expect(screen.getByText("Email with ID 3 does not exist.")).toBeInTheDocument()
    })
    it('render email when email is found for provided id', async () => {
        renderWithProviders(<MemoryRouter initialEntries={["/emails/1"]}><App /></MemoryRouter>);

        await waitFor(() => {
            expect(screen.getByText("Email 1")).toBeInTheDocument()
        })
    })
})