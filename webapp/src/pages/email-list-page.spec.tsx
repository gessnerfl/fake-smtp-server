import * as React from 'react'
import {prettyDOM, screen, waitFor} from '@testing-library/react'
import '@testing-library/jest-dom'
import {renderWithProviders} from "../utils";
import App from "../app";
import {MemoryRouter} from "react-router-dom";
import {EmailPage} from "../models/email";
import {testEmailPage1, testEmailPage2} from "../setupTests";
import userEvent from "@testing-library/user-event";
import {act} from "react-dom/test-utils";

function shouldContainPage(page: EmailPage) {
    page.content.forEach(mail => {
        expect(screen.getByText(mail.fromAddress)).toBeInTheDocument()
    })
}

describe('EmailListPage', () => {
    it('render first page of email list', async () => {
        renderWithProviders(<MemoryRouter initialEntries={["/"]}><App /></MemoryRouter>);

        expect(screen.getByText("Inbox")).toBeInTheDocument()
        await waitFor(() => {
            prettyDOM()
            shouldContainPage(testEmailPage1)
        })
    })
    it('render second page of email list', async () => {
        renderWithProviders(<MemoryRouter initialEntries={["/?page=2"]}><App /></MemoryRouter>);

        expect(screen.getByText("Inbox")).toBeInTheDocument()
        await waitFor(() => {
            shouldContainPage(testEmailPage2)
        })
    })
    it('should render email details on selection', async () => {
        renderWithProviders(<MemoryRouter initialEntries={["/"]}><App /></MemoryRouter>);

        await waitFor(() => {
            expect(screen.getByTitle("1")).toBeInTheDocument()
        })

        act(() => {
            userEvent.click(screen.getByTitle("1"))
        })

        await waitFor(() => {
            expect(screen.getByText("Email 1")).toBeInTheDocument()
        })
    })
})