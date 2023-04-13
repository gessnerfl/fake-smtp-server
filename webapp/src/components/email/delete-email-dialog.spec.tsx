import {render, screen, waitFor} from "@testing-library/react";
import {testEmail1} from "../../setupTests";
import * as React from "react";
import '@testing-library/jest-dom'
import {DeleteEmailDialog} from "./delete-email-dialog";
import userEvent from "@testing-library/user-event";

describe('DeleteEmailDialog', () => {
    it('keep delete email dialog closed when open is false', () => {
        render(<DeleteEmailDialog email={testEmail1} open={false} onClose={(_) => {}}/>);

        expect(screen.queryByText("Delete Email 1")).toBeNull()
    })
    it('render delete email dialog when open is true', () => {
        render(<DeleteEmailDialog email={testEmail1} open={true} onClose={(_) => {}}/>);

        expect(screen.getByText("Delete Email 1")).toBeInTheDocument()
        expect(screen.getByText("Do you really want to delete email with id 1?")).toBeInTheDocument()
    })
    it('should close delete email dialog with true when confirmed', async () => {
        let confirmed = false
        const closeCallback = (c: boolean) => confirmed = c

        render(<DeleteEmailDialog email={testEmail1} open={true} onClose={closeCallback}/>);
        userEvent.click(screen.getByText("Yes"))

        await waitFor(() => {
            expect(confirmed).toBeTruthy()
        })
    })
    it('should close delete email dialog with false when not confirmed', async () => {
        let confirmed = true
        const closeCallback = (c: boolean) => confirmed = c

        render(<DeleteEmailDialog email={testEmail1} open={true} onClose={closeCallback}/>);
        userEvent.click(screen.getByText("No"))

        await waitFor(() => {
            expect(confirmed).toBeFalsy()
        })
    })
})