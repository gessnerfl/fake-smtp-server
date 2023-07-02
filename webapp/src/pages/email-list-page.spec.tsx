import * as React from 'react'
import {screen, waitFor} from '@testing-library/react'
import '@testing-library/jest-dom'
import {renderWithProviders} from "../test-utils";
import App from "../app";
import {MemoryRouter} from "react-router-dom";
import {testData, originalTestData} from "../setupTests";
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
        renderWithProviders(<MemoryRouter initialEntries={["/"]}><App/></MemoryRouter>);

        expect(screen.getByText("Inbox")).toBeInTheDocument()
        await waitFor(() => {
            shouldContainPage(testData.slice(0, 10))
        })
    })
    it('render second page of email list', async () => {
        renderWithProviders(<MemoryRouter initialEntries={["/?page=1"]}><App/></MemoryRouter>);

        expect(screen.getByText("Inbox")).toBeInTheDocument()
        await waitFor(() => {
            shouldContainPage(testData.slice(10, 10))
        })
    })
    it('render first page of size 25', async () => {
        renderWithProviders(<MemoryRouter initialEntries={["/?pageSize=25"]}><App/></MemoryRouter>);

        expect(screen.getByText("Inbox")).toBeInTheDocument()
        await waitFor(() => {
            shouldContainPage(testData.slice(0, 25))
        })
    })
    it('should render email details on selection', async () => {
        renderWithProviders(<MemoryRouter initialEntries={["/"]}><App/></MemoryRouter>);

        await waitFor(() => {
            expect(screen.getByTitle("1")).toBeInTheDocument()
        })

        userEvent.click(screen.getByTitle("1"))

        await waitFor(() => {
            expect(screen.getByText("Email 1")).toBeInTheDocument()
        })
    })
    it('should not allow to open delete dialog when no email is selected', async () => {
        renderWithProviders(<MemoryRouter initialEntries={["/"]}><App/></MemoryRouter>);

        await waitFor(() => {
            expect(screen.getByTitle("1")).toBeInTheDocument()
        })

        expect(screen.getByText("Delete")).toBeDisabled()
    })
    it('should not delete selected email when delete is not confirmed', async () => {
        renderWithProviders(<MemoryRouter initialEntries={["/"]}><App/></MemoryRouter>);

        await waitFor(() => {
            expect(screen.getByTitle("1")).toBeInTheDocument()
        })

        userEvent.click(screen.getByTitle("1"))

        await waitFor(() => {
            expect(screen.getByText("Delete")).toBeEnabled()
        })

        userEvent.click(screen.getByText("Delete"))

        await waitFor(() => {
            expect(screen.getByText("Delete Email 1")).toBeInTheDocument()
        })

        userEvent.click(screen.getByText("No"))

        await waitFor(() => {
            expect(screen.getByText("Email 1")).toBeInTheDocument()
        })
    })
    it('should delete selected email when delete is confirmed', async () => {
        renderWithProviders(<MemoryRouter initialEntries={["/"]}><App/></MemoryRouter>);

        await waitFor(() => {
            expect(screen.getByTitle("1")).toBeInTheDocument()
        })

        userEvent.click(screen.getByTitle("1"))

        await waitFor(() => {
            expect(screen.getByText("Delete")).toBeEnabled()
        })

        userEvent.click(screen.getByText("Delete"))

        await waitFor(() => {
            expect(screen.getByText("Delete Email 1")).toBeInTheDocument()
        })

        act(() => {
            userEvent.click(screen.getByText("Yes"))
        })

        await waitFor(() => {
            shouldContainPage(originalTestData.splice(1, 1).slice(0, 10))
        })
    })
    it('should not delete all emails when delete is not confirmed', async () => {
        renderWithProviders(<MemoryRouter initialEntries={["/"]}><App/></MemoryRouter>);

        await waitFor(() => {
            expect(screen.getByTitle("5")).toBeInTheDocument()
        })

        userEvent.click(screen.getByText("Delete All"))

        await waitFor(() => {
            expect(screen.getByText("Delete All Emails")).toBeInTheDocument()
        })

        userEvent.click(screen.getByText("No"))

        await waitFor(() => {
            shouldContainPage(originalTestData.splice(1, 1).slice(0, 10))
        })
    })
    it('should delete all emails when delete is confirmed', async () => {
        renderWithProviders(<MemoryRouter initialEntries={["/"]}><App/></MemoryRouter>);

        await waitFor(() => {
            expect(screen.getByTitle("5")).toBeInTheDocument()
        })

        userEvent.click(screen.getByText("Delete All"))

        await waitFor(() => {
            expect(screen.getByText("Delete All Emails")).toBeInTheDocument()
        })

        act(() => {
            userEvent.click(screen.getByText("Yes"))
        })

        await waitFor(() => {
            expect(screen.getByText("No rows")).toBeInTheDocument()
        })
    })
})