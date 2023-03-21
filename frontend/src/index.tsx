import React from 'react';
import ReactDOM from 'react-dom/client';

import './index.css';
import '@fontsource/roboto/300.css';
import '@fontsource/roboto/400.css';
import '@fontsource/roboto/500.css';
import '@fontsource/roboto/700.css';

import Shell from './pages/shell';
import {createBrowserRouter, RouterProvider} from "react-router-dom";
import EmailListPage from "./pages/email-list-page";
import ErrorPage from "./pages/error-page";
import {Provider} from "react-redux";
import {store} from "./stores/store";


const router = createBrowserRouter([
    {
        path: "/",
        element:<Shell />,
        errorElement: <ErrorPage />,
        children: [
            {
                index: true,
                element: <EmailListPage />
            }
        ]
    },
]);

const root = ReactDOM.createRoot(
    document.getElementById('root') as HTMLElement
);
root.render(
    <React.StrictMode>
        <Provider store={store}>
            <RouterProvider router={router} />
        </Provider>
    </React.StrictMode>
);
