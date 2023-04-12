import * as React from 'react'
import {prettyDOM, screen, waitFor} from '@testing-library/react'
import '@testing-library/jest-dom'
import {renderWithProviders} from "../test-utils";
import App from "../app";
import {MemoryRouter} from "react-router-dom";
import {testData} from "../setupTests";
import userEvent from "@testing-library/user-event";
import {act} from "react-dom/test-utils";
import {Email} from "../models/email";

function shouldContainPage(data: Email[]) {
    data.forEach(mail => {
        expect(screen.getByText(mail.fromAddress)).toBeInTheDocument()
    })
}

describe('EmailListPage', () => {
    it('render first page of email list', async () => {
        renderWithProviders(<MemoryRouter initialEntries={["/"]}><App /></MemoryRouter>);

        expect(screen.getByText("Inbox")).toBeInTheDocument()
        await waitFor(() => {
            prettyDOM()
            shouldContainPage(testData.slice(0, 10))
        })
    })
    it('render second page of email list', async () => {
        renderWithProviders(<MemoryRouter initialEntries={["/?page=1"]}><App /></MemoryRouter>);

        expect(screen.getByText("Inbox")).toBeInTheDocument()
        await waitFor(() => {
            shouldContainPage(testData.slice(1, 10))
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