import * as React from 'react'
import '@testing-library/jest-dom'
import {fireEvent, screen, waitFor} from '@testing-library/react'
import Navigation from "./navigation";
import {renderWithProviders} from "../test-utils";
import { useGetMetaDataQuery, useLogoutMutation } from "../store/rest-api";
import { setAuthenticated } from '../store/auth-slice';
import { setupStore } from '../store/store';

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

    it('polls connection status every second', () => {
        const setIntervalSpy = jest.spyOn(global, 'setInterval')

        renderWithProviders(<Navigation />)

        expect(setIntervalSpy).toHaveBeenCalledWith(expect.any(Function), 1000)
        setIntervalSpy.mockRestore()
    })

    it('keeps auth state when logout fails and shows an error', async () => {
        const mockLogout = jest.fn().mockReturnValue({
            unwrap: jest.fn().mockRejectedValue(new Error('logout failed')),
        });
        (useLogoutMutation as jest.Mock).mockReturnValue([mockLogout]);
        (useGetMetaDataQuery as jest.Mock).mockReturnValue({
            data: { version: "local", authenticationEnabled: true },
            isLoading: false
        });

        const store = setupStore();
        store.dispatch(setAuthenticated(true));

        renderWithProviders(<Navigation />, { store });

        fireEvent.click(screen.getByRole('button', { name: 'Logout' }));

        expect(await screen.findByRole('alert')).toHaveTextContent('Logout failed. Please try again.');
        await waitFor(() => expect(store.getState().auth.isAuthenticated).toBe(true));
    })

    it('clears a stale logout error when authentication is disabled', async () => {
        const mockLogout = jest.fn().mockReturnValue({
            unwrap: jest.fn().mockRejectedValue(new Error('logout failed')),
        });
        (useLogoutMutation as jest.Mock).mockReturnValue([mockLogout]);
        const metaDataQuery = useGetMetaDataQuery as jest.Mock;
        metaDataQuery.mockReturnValue({
            data: { version: "local", authenticationEnabled: true },
            isLoading: false
        });

        const store = setupStore();
        store.dispatch(setAuthenticated(true));

        const { rerender } = renderWithProviders(<Navigation />, { store });

        fireEvent.click(screen.getByRole('button', { name: 'Logout' }));
        expect(await screen.findByRole('alert')).toHaveTextContent('Logout failed. Please try again.');

        metaDataQuery.mockReturnValue({
            data: { version: "local", authenticationEnabled: false },
            isLoading: false
        });
        rerender(<Navigation />);

        await waitFor(() => expect(screen.queryByRole('alert')).not.toBeInTheDocument());
        expect(screen.queryByRole('button', { name: 'Logout' })).not.toBeInTheDocument();
    })
})
