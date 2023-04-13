import {screen, waitFor} from "@testing-library/react";
import * as React from "react";
import '@testing-library/jest-dom'
import {DeleteAllEmailsButton} from "./delete-all-emails-button";
import userEvent from "@testing-library/user-event";
import {renderWithProviders} from "../../test-utils";

describe('DeleteAllEmailsButton', () => {
    it('render delete email button in disabled state when no emails are available', () => {
        renderWithProviders(<DeleteAllEmailsButton emailsAvailable={false}/>);

        expect(screen.getByText("Delete All")).toBeDisabled()
    })
    it('render delete all emails button in enabled state when emails are available', () => {
        renderWithProviders(<DeleteAllEmailsButton emailsAvailable={true}/>);

        expect(screen.getByText("Delete All")).toBeEnabled()
    })
    it('open delete all emails dialog when clicking the button', async () => {
        renderWithProviders(<DeleteAllEmailsButton emailsAvailable={true}/>);
        userEvent.click(screen.getByText("Delete All"))

        //dialog header available
        await waitFor(() => {
            expect(screen.getByText("Delete All Emails")).toBeEnabled()
        })
    })
})