import {render, screen} from "@testing-library/react";
import * as React from "react";
import '@testing-library/jest-dom'
import {DeleteAllEmailsDialog} from "./delete-all-emails-dialog";
import userEvent from "@testing-library/user-event";
import {act} from "react-dom/test-utils";

describe('DeleteAllEmailsDialog', () => {
    it('keep delete all emails dialog closed when open is false', () => {
        render(<DeleteAllEmailsDialog open={false} onClose={(_) => {}}/>);

        //card header in document
        expect(screen.queryByText("Delete All Emails")).toBeNull()
    })
    it('render delete all emails dialog when open is true', () => {
        render(<DeleteAllEmailsDialog open={true} onClose={(_) => {}}/>);

        //card header in document
        expect(screen.getByText("Delete All Emails")).toBeInTheDocument()
        expect(screen.getByText("Do you really want to delete all emails?")).toBeInTheDocument()
    })
    it('should close delete all emails dialog with true when confirmed', () => {
        let confirmed = false
        const closeCallback = (c: boolean) => confirmed = c

        render(<DeleteAllEmailsDialog open={true} onClose={closeCallback}/>);
        act(() => userEvent.click(screen.getByText("Yes")))

        //card header in document
        expect(confirmed).toBeTruthy()
    })
    it('should close delete all emails dialog with false when not confirmed', () => {
        let confirmed = true
        const closeCallback = (c: boolean) => confirmed = c

        render(<DeleteAllEmailsDialog open={true} onClose={closeCallback}/>);
        act(() => userEvent.click(screen.getByText("No")))

        //card header in document
        expect(confirmed).toBeFalsy()
    })
})