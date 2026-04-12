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
    const authenticationEnabled = metaData?.authenticationEnabled;
    const sseHeartbeatIntervalSeconds = metaData?.sseHeartbeatIntervalSeconds;

    useEffect(() => {
        if (isLoading || authenticationEnabled === undefined) {
            return;
        }

        // Defer connection setup by one tick so StrictMode's mount/unmount probe
        // can cancel the first attempt before opening a second SSE connection.
        const setupTimer = window.setTimeout(() => {
            setupEmailEventSource(store, {
                authenticationEnabled,
                sseHeartbeatIntervalSeconds,
            });
        }, 0);

        return () => {
            window.clearTimeout(setupTimer);
            if (window.emailEventSource) {
                window.emailEventSource.close();
                delete window.emailEventSource;
            }
        };
    }, [isAuthenticated, isLoading, authenticationEnabled, sseHeartbeatIntervalSeconds]);

    return null;
};

export default EmailEventSourceManager;
