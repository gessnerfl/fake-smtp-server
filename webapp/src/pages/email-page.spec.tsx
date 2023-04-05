import * as React from 'react'
import {screen} from '@testing-library/react'
import '@testing-library/jest-dom'
import {EmailPage} from "./email-page";
import {renderWithProviders} from "../tests/utils";
import {MemoryRouter} from "react-router-dom";


describe('EmailPage', () => {
    it('render alert when no email id provided', () => {
        renderWithProviders(<EmailPage />);

        expect(screen.getByText("Email with ID undefined does not exist.")).toBeInTheDocument()
    })
/*
    it('render alert when no email found for provided id', () => {
        renderWithProviders(<MemoryRouter initialEntries={["/emails/3"]}><EmailPage /></MemoryRouter>);

        expect(screen.getByText("Email with ID 3 does not exist.")).toBeInTheDocument()
    })*/
})