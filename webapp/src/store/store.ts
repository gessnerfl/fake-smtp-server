import {combineReducers, configureStore, PreloadedState} from "@reduxjs/toolkit";
import {emailsApi} from "./emails-api";
import {setupListeners} from "@reduxjs/toolkit/query";

const rootReducer = combineReducers({
    [emailsApi.reducerPath]: emailsApi.reducer
})

export function setupStore(preloadedState?: PreloadedState<RootState>) {
    return configureStore({
        reducer: rootReducer,
        middleware: (getDefaultMiddleware) =>
            getDefaultMiddleware().concat(emailsApi.middleware),
        preloadedState
    })
}

export const store = setupStore()
setupListeners(store.dispatch)

export type RootState = ReturnType<typeof rootReducer>
export type AppStore = ReturnType<typeof setupStore>
export type AppDispatch = AppStore['dispatch']