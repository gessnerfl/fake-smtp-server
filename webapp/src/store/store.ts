import {combineReducers, configureStore, PreloadedState} from "@reduxjs/toolkit";
import {restApi} from "./rest-api";
import {setupListeners} from "@reduxjs/toolkit/query";

const rootReducer = combineReducers({
    [restApi.reducerPath]: restApi.reducer,
})

export function setupStore(preloadedState?: PreloadedState<RootState>) {
    return configureStore({
        reducer: rootReducer,
        middleware: (getDefaultMiddleware) =>
            getDefaultMiddleware().concat(restApi.middleware),
        preloadedState
    })
}

export const store = setupStore()
setupListeners(store.dispatch)

export type RootState = ReturnType<typeof rootReducer>
export type AppStore = ReturnType<typeof setupStore>
export type AppDispatch = AppStore['dispatch']