import {combineReducers, configureStore} from "@reduxjs/toolkit";
import {restApi} from "./rest-api";
import {setupListeners} from "@reduxjs/toolkit/query";
import authReducer from "./auth-slice";

const rootReducer = combineReducers({
    [restApi.reducerPath]: restApi.reducer,
    auth: authReducer,
})

export function setupStore() {
    return configureStore({
        reducer: rootReducer,
        middleware: (getDefaultMiddleware) =>
            getDefaultMiddleware().concat(restApi.middleware),
    })
}

export const store = setupStore()
setupListeners(store.dispatch)

export type RootState = ReturnType<typeof rootReducer>
export type AppStore = ReturnType<typeof setupStore>
export type AppDispatch = AppStore['dispatch']
