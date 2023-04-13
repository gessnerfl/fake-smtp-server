import {screen, waitFor} from "@testing-library/react";
import {testEmail1} from "../../setupTests";
import * as React from "react";
import '@testing-library/jest-dom'
import {DeleteEmailButton} from "./delete-email-button";
import userEvent from "@testing-library/user-event";
import {renderWithProviders} from "../../test-utils";

describe('DeleteAllEmailButton', () => {
    it('render delete email button in disabled state when no email is selected', () => {
        renderWithProviders(<DeleteEmailButton selectedEmail={undefined}/>);

        expect(screen.getByText("Delete")).toBeDisabled()
    })
    it('render delete email button in enabled state when email is selected', () => {
        renderWithProviders(<DeleteEmailButton selectedEmail={testEmail1}/>);

        expect(screen.getByText("Delete")).toBeEnabled()
    })
    it('open delete email dialog when clicking the button', async () => {
        renderWithProviders(<DeleteEmailButton selectedEmail={testEmail1}/>);
        userEvent.click(screen.getByText("Delete"))

        //dialog header available
        await waitFor(() => {
            expect(screen.getByText("Delete Email 1")).toBeEnabled()
        })
    })
})