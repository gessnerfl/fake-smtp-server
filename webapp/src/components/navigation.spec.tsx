import * as React from 'react'
import '@testing-library/jest-dom'
import {screen, waitFor} from '@testing-library/react'
import Navigation from "./navigation";
import {renderWithProviders} from "../test-utils";

describe('Navigation', () => {
    it('render navigation component', async () => {
        renderWithProviders(<Navigation />);

        expect(screen.getByText("FakeSMTPServer")).toBeInTheDocument()
        expect(screen.getByText("Version:")).toBeInTheDocument()
        await waitFor(() => {
            expect(screen.getByText("local")).toBeInTheDocument()
        })
    })
})