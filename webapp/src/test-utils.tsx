import React, {PropsWithChildren} from 'react'
import {render} from '@testing-library/react'
import type {RenderOptions} from '@testing-library/react'
import {Provider} from 'react-redux'
import type {AppStore} from './store/store'
import {setupStore} from "./store/store";
import { TEST_BASE_PATH } from './base-path'

interface ExtendedRenderOptions extends Omit<RenderOptions, 'queries'> {
    store?: AppStore
}

export function renderWithProviders(
    ui: React.ReactElement,
    {
        store = setupStore(),
        ...renderOptions
    }: ExtendedRenderOptions = {}
) {
    function Wrapper({children}: PropsWithChildren<{}>): React.JSX.Element {
        return <Provider store={store}>{children}</Provider>
    }

    return {store, ...render(ui, {wrapper: Wrapper, ...renderOptions})}
}

export function endpointUrl(endpoint: string): string {
    return `${TEST_BASE_PATH}${endpoint}`
}