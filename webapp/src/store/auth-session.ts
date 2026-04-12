export const DEFAULT_SESSION_TIMEOUT_MINUTES = 10;
export const DEFAULT_SESSION_TIMEOUT_MS = DEFAULT_SESSION_TIMEOUT_MINUTES * 60 * 1000;

const LAST_ACTIVITY_KEY = "fakesmtp.auth.lastActivity";
const SESSION_TIMEOUT_KEY = "fakesmtp.auth.sessionTimeoutMs";

const isStorageAvailable = (): boolean => typeof window !== "undefined" && !!window.sessionStorage;

export const loadLastActivity = (): number | null => {
  if (!isStorageAvailable()) {
    return null;
  }
  const raw = window.sessionStorage.getItem(LAST_ACTIVITY_KEY);
  if (!raw) {
    return null;
  }
  const parsed = Number(raw);
  return Number.isFinite(parsed) ? parsed : null;
};

export const persistLastActivity = (timestamp: number | null): void => {
  if (!isStorageAvailable()) {
    return;
  }
  if (timestamp === null) {
    window.sessionStorage.removeItem(LAST_ACTIVITY_KEY);
    return;
  }
  window.sessionStorage.setItem(LAST_ACTIVITY_KEY, String(timestamp));
};

export const clearAuthSessionStorage = (): void => {
  if (!isStorageAvailable()) {
    return;
  }
  window.sessionStorage.removeItem(LAST_ACTIVITY_KEY);
  window.sessionStorage.removeItem(SESSION_TIMEOUT_KEY);
};

export const loadSessionTimeoutMs = (): number | null => {
  if (!isStorageAvailable()) {
    return null;
  }
  const raw = window.sessionStorage.getItem(SESSION_TIMEOUT_KEY);
  if (!raw) {
    return null;
  }
  const parsed = Number(raw);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
};

export const persistSessionTimeoutMs = (timeoutMs: number | null): void => {
  if (!isStorageAvailable()) {
    return;
  }
  if (timeoutMs === null) {
    window.sessionStorage.removeItem(SESSION_TIMEOUT_KEY);
    return;
  }
  if (!Number.isFinite(timeoutMs) || timeoutMs <= 0) {
    return;
  }
  window.sessionStorage.setItem(SESSION_TIMEOUT_KEY, String(timeoutMs));
};

export const resolveSessionTimeoutMs = (timeoutMinutes?: number): number => {
  if (typeof timeoutMinutes === "number" && Number.isFinite(timeoutMinutes) && timeoutMinutes > 0) {
    return timeoutMinutes * 60 * 1000;
  }
  return loadSessionTimeoutMs() ?? DEFAULT_SESSION_TIMEOUT_MS;
};
