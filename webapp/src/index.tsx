import React from 'react';
import ReactDOM from 'react-dom/client';

import './index.css';

import {BrowserRouter} from "react-router-dom";
import {Provider} from "react-redux";
import {store} from "./store/store";
import App from "./app";
import {getBasePath} from "./utils";

const root = ReactDOM.createRoot(
    document.getElementById('root') as HTMLElement
);

root.render(
    <React.StrictMode>
        <Provider store={store}>
            <BrowserRouter basename={getBasePath()}>
                <App />
            </BrowserRouter>
        </Provider>
    </React.StrictMode>
);
