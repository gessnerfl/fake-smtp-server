import React, { useEffect, useRef, useCallback } from "react";
import { useDispatch, useSelector } from "react-redux";
import { clearAuthentication } from "../store/auth-slice";
import { RootState } from "../store/store";
import {
  clearAuthSessionStorage,
  persistLastActivity,
  persistSessionTimeoutMs,
  resolveSessionTimeoutMs
} from "../store/auth-session";
import { useGetMetaDataQuery } from "../store/rest-api";

const ACTIVITY_EVENTS = [
  "mousemove",
  "mousedown",
  "keydown",
  "scroll",
  "touchstart",
  "focus",
];
const ACTIVITY_REVALIDATION_THROTTLE_MS = 5_000;

const SessionTimeoutManager: React.FC = () => {
  const { isAuthenticated } = useSelector((state: RootState) => state.auth);
  const { data, refetch } = useGetMetaDataQuery();
  const dispatch = useDispatch();
  const timeoutRef = useRef<number | null>(null);
  const revalidationInFlightRef = useRef(false);
  const lastActivityCheckRef = useRef(0);
  const sessionTimeoutMs = resolveSessionTimeoutMs(data?.sessionTimeoutMinutes);

  const clearSessionTimer = useCallback(() => {
    if (timeoutRef.current !== null) {
      window.clearTimeout(timeoutRef.current);
      timeoutRef.current = null;
    }
  }, []);

  const clearSessionState = useCallback(() => {
    clearSessionTimer();
    lastActivityCheckRef.current = 0;
    dispatch(clearAuthentication());
    clearAuthSessionStorage();
  }, [clearSessionTimer, dispatch]);

  const isAuthFailure = (error: unknown): boolean => {
    if (typeof error !== "object" || error === null || !("status" in error)) {
      return false;
    }

    const status = (error as { status: unknown }).status;
    return status === 401 || status === 403;
  };

  const revalidateSession = useCallback(async (reason: "activity" | "visibility" | "timer") => {
    if (revalidationInFlightRef.current) {
      return;
    }

    if (reason === "activity") {
      const now = Date.now();
      if (now - lastActivityCheckRef.current < ACTIVITY_REVALIDATION_THROTTLE_MS) {
        return;
      }
      lastActivityCheckRef.current = now;
    }

    revalidationInFlightRef.current = true;

    try {
      const metaData = await refetch().unwrap();
      if (metaData.authenticated === false) {
        clearSessionState();
      } else {
        const refreshedTimeoutMs = resolveSessionTimeoutMs(metaData.sessionTimeoutMinutes);
        const now = Date.now();

        persistLastActivity(now);
        persistSessionTimeoutMs(refreshedTimeoutMs);
        clearSessionTimer();
        timeoutRef.current = window.setTimeout(() => {
          void revalidateSession("timer");
        }, refreshedTimeoutMs);
      }
    } catch (error) {
      if (isAuthFailure(error)) {
        clearSessionState();
      }
    } finally {
      revalidationInFlightRef.current = false;
    }
  }, [clearSessionState, clearSessionTimer, refetch]);

  const scheduleSessionCheck = useCallback((delayMs: number) => {
    clearSessionTimer();
    timeoutRef.current = window.setTimeout(() => {
      void revalidateSession("timer");
    }, delayMs);
  }, [clearSessionTimer, revalidateSession]);

  const handleActivity = useCallback(() => {
    if (!isAuthenticated) {
      return;
    }
    void revalidateSession("activity");
  }, [isAuthenticated, revalidateSession]);

  useEffect(() => {
    if (!isAuthenticated || typeof window === "undefined") {
      clearSessionTimer();
      clearAuthSessionStorage();
      return undefined;
    }

    if (!data) {
      return undefined;
    }

    if (data.authenticated === false) {
      clearSessionState();
      return undefined;
    }

    const now = Date.now();
    lastActivityCheckRef.current = 0;
    persistLastActivity(now);
    persistSessionTimeoutMs(sessionTimeoutMs);
    scheduleSessionCheck(sessionTimeoutMs);

    const onVisibilityChange = () => {
      if (document.visibilityState === "visible") {
        void revalidateSession("visibility");
      }
    };

    ACTIVITY_EVENTS.forEach((eventName) => {
      window.addEventListener(eventName, handleActivity, { passive: true });
    });
    document.addEventListener("visibilitychange", onVisibilityChange);

    return () => {
      ACTIVITY_EVENTS.forEach((eventName) => {
        window.removeEventListener(eventName, handleActivity);
      });
      document.removeEventListener("visibilitychange", onVisibilityChange);
      clearSessionTimer();
    };
  }, [clearSessionState, clearSessionTimer, data, handleActivity, isAuthenticated, revalidateSession, scheduleSessionCheck, sessionTimeoutMs]);

  return null;
};

export default SessionTimeoutManager;
