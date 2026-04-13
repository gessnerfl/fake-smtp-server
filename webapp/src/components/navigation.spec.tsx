import * as React from 'react'
import '@testing-library/jest-dom'
import {screen} from '@testing-library/react'
import { jest } from '@jest/globals';

const mockUseGetMetaDataQuery = jest.fn();

jest.unstable_mockModule("../store/rest-api", () => {
    return {
        useGetMetaDataQuery: mockUseGetMetaDataQuery
    };
});

const { default: Navigation } = await import("./navigation");
const { renderWithProviders } = await import("../test-utils");

describe('Navigation', () => {
    beforeEach(() => {
        jest.clearAllMocks();

        mockUseGetMetaDataQuery.mockReturnValue({
            data: { version: "local", authenticationEnabled: false },
            isLoading: false
        });
    });

    it('render navigation component', async () => {
        renderWithProviders(<Navigation />);

        expect(screen.getByText("FakeSMTPServer")).toBeInTheDocument()
        expect(screen.getByText("Version:")).toBeInTheDocument()
        expect(screen.getByText("local")).toBeInTheDocument()
    })
})
