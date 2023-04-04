import {combineReducers, configureStore, PreloadedState} from "@reduxjs/toolkit";
import {emailsApi} from "./emails-api";
import {setupListeners} from "@reduxjs/toolkit/query";

const rootReducer = combineReducers({
    [emailsApi.reducerPath]: emailsApi.reducer
})

export const store = configureStore({
    reducer: rootReducer,
    middleware: (getDefaultMiddleware) =>
        getDefaultMiddleware().concat(emailsApi.middleware),
});
export function setupStore(preloadedState?: PreloadedState<RootState>) {
    return store
}

export type RootState = ReturnType<typeof rootReducer>
export type AppStore = ReturnType<typeof setupStore>
export type AppDispatch = typeof store.dispatch
setupListeners(store.dispatch)