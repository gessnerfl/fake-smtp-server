import * as React from 'react'
import '@testing-library/jest-dom'
import {screen, waitFor} from '@testing-library/react'
import {http, HttpResponse} from 'msw'
import {MemoryRouter, Route, Routes} from 'react-router-dom'
import Shell from "./shell";
import {endpointUrl, renderWithProviders} from "../test-utils";
import {server} from "../setupTests";

describe('Shell', () => {
    const renderShellWithOutlet = () => {
        renderWithProviders(
            <MemoryRouter initialEntries={['/']}>
                <Routes>
                    <Route path="/" element={<Shell/>}>
                        <Route index={true} element={<div>Shell content</div>}/>
                    </Route>
                </Routes>
            </MemoryRouter>
        );
    };

    it('render shell component', async () => {
        renderShellWithOutlet();

        expect(screen.getByText("FakeSMTPServer")).toBeInTheDocument()
        expect(await screen.findByText('Shell content')).toBeInTheDocument()
    })

    it('does not render protected content when metadata request fails', async () => {
        server.use(
            http.get(endpointUrl('/api/meta-data'), async () => new HttpResponse('Backend unavailable', {status: 503}))
        );

        renderShellWithOutlet();

        await waitFor(() => expect(screen.queryByText('Loading...')).not.toBeInTheDocument());
        expect(screen.queryByText('Shell content')).not.toBeInTheDocument();
        expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument();
    });

    it('shows an error state when metadata request fails', async () => {
        server.use(
            http.get(endpointUrl('/api/meta-data'), async () => new HttpResponse('Backend unavailable', {status: 503}))
        );

        renderShellWithOutlet();

        await waitFor(() => expect(screen.getByRole('alert')).toBeInTheDocument());
        expect(screen.getByRole('alert')).toHaveTextContent(/unable to load application state/i);
        expect(screen.queryByText('Shell content')).not.toBeInTheDocument();
    });
})
