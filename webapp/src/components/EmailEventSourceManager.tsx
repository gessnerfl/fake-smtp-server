import React, { useEffect } from "react";
import { useSelector } from "react-redux";
import { setupEmailEventSource, useGetMetaDataQuery } from "../store/rest-api";
import { RootState, store } from "../store/store";

/**
 * Component that manages the Server-Sent Events connection for email notifications.
 * This component doesn't render anything visible but handles the SSE connection lifecycle.
 *
 * The connection is only established when either:
 * 1. Authentication is not enabled (no login required), or
 * 2. The user is authenticated (logged in)
 */
const EmailEventSourceManager: React.FC = () => {
    const { isAuthenticated } = useSelector((state: RootState) => state.auth);
    const { data: metaData, isLoading } = useGetMetaDataQuery();

    useEffect(() => {
        if (!isLoading && metaData) {
            setupEmailEventSource(store, metaData.authenticationEnabled);
            return () => {
                if (window.emailEventSource) {
                    window.emailEventSource.close();
                    delete window.emailEventSource;
                }
            };
        }
    }, [isAuthenticated, metaData, isLoading]);

    return null;
};

export default EmailEventSourceManager;
