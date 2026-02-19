import * as React from 'react'
import '@testing-library/jest-dom'
import {screen} from '@testing-library/react'
import Navigation from "./navigation";
import {renderWithProviders} from "../test-utils";
import { useGetMetaDataQuery, useLogoutMutation } from "../store/rest-api";

jest.mock("../store/rest-api", () => {
    return {
        useGetMetaDataQuery: jest.fn(),
        useLogoutMutation: jest.fn()
    };
});

describe('Navigation', () => {
    beforeEach(() => {
        jest.clearAllMocks();

        (useGetMetaDataQuery as jest.Mock).mockReturnValue({
            data: { version: "local", authenticationEnabled: false },
            isLoading: false
        });
        const mockLogout = jest.fn().mockReturnValue({
            unwrap: jest.fn().mockResolvedValue({}),
        });
        (useLogoutMutation as jest.Mock).mockReturnValue([mockLogout]);
    });

    it('render navigation component', async () => {
        renderWithProviders(<Navigation />);

        expect(screen.getByText("FakeSMTPServer")).toBeInTheDocument()
        expect(screen.getByText("Version:")).toBeInTheDocument()
        expect(screen.getByText("local")).toBeInTheDocument()
    })
})
