import React, { useEffect, useRef, useCallback } from "react";
import { useDispatch, useSelector } from "react-redux";
import { clearAuthentication } from "../store/auth-slice";
import { RootState } from "../store/store";
import {
  clearAuthSessionStorage,
  loadLastActivity,
  persistLastActivity,
  persistSessionTimeoutMs,
  resolveSessionTimeoutMs
} from "../store/auth-session";
import { useGetMetaDataQuery, useLogoutMutation } from "../store/rest-api";

const ACTIVITY_EVENTS = [
  "mousemove",
  "mousedown",
  "keydown",
  "scroll",
  "touchstart",
  "focus",
];

const SessionTimeoutManager: React.FC = () => {
  const { isAuthenticated } = useSelector((state: RootState) => state.auth);
  const { data } = useGetMetaDataQuery();
  const dispatch = useDispatch();
  const [logout] = useLogoutMutation();
  const timeoutRef = useRef<number | null>(null);
  const sessionTimeoutMs = resolveSessionTimeoutMs(data?.sessionTimeoutMinutes);

  const triggerLogout = useCallback(() => {
    logout()
      .unwrap()
      .catch(() => undefined)
      .finally(() => dispatch(clearAuthentication()));
  }, [dispatch, logout]);

  const clearLogoutTimer = () => {
    if (timeoutRef.current !== null) {
      window.clearTimeout(timeoutRef.current);
      timeoutRef.current = null;
    }
  };

  const scheduleLogout = (delayMs: number) => {
    clearLogoutTimer();
    timeoutRef.current = window.setTimeout(() => {
      triggerLogout();
    }, delayMs);
  };

  const handleActivity = () => {
    if (!isAuthenticated) {
      return;
    }
    const now = Date.now();
    const lastActivity = loadLastActivity();
    if (lastActivity !== null && now - lastActivity >= sessionTimeoutMs) {
      triggerLogout();
      return;
    }
    persistLastActivity(now);
    scheduleLogout(sessionTimeoutMs);
  };

  useEffect(() => {
    if (!isAuthenticated || typeof window === "undefined") {
      clearLogoutTimer();
      clearAuthSessionStorage();
      return undefined;
    }

    const now = Date.now();
    const lastActivity = loadLastActivity();
    if (lastActivity !== null && now - lastActivity >= sessionTimeoutMs) {
      triggerLogout();
      return undefined;
    }

    persistLastActivity(lastActivity ?? now);
    persistSessionTimeoutMs(sessionTimeoutMs);
    const remaining = Math.max(sessionTimeoutMs - (now - (lastActivity ?? now)), 0);
    scheduleLogout(remaining);

    const onVisibilityChange = () => {
      if (document.visibilityState === "visible") {
        handleActivity();
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
      clearLogoutTimer();
    };
  }, [dispatch, isAuthenticated, sessionTimeoutMs, triggerLogout]);

  return null;
};

export default SessionTimeoutManager;
