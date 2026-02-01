import { createApi, fetchBaseQuery } from "@reduxjs/toolkit/query/react";
import type { BaseQueryFn, FetchArgs, FetchBaseQueryError } from "@reduxjs/toolkit/query";
import { Email, EmailPage } from "../models/email";
import { Pageable } from "../models/pageable";
import { MetaData } from "../models/meta-data";
import { getBasePath } from "../base-path";
import { RootState } from "./store";
import { Credentials } from "../models/auth";
import { clearAuthentication } from "./auth-slice";

function getBasePathString() {
  const path = getBasePath();
  return path ?? ""
}

type EmailEventListener = (event: MessageEvent<string>) => void;

interface EmailEventSourceLike {
  close: () => void;
  addEventListener: (type: string, listener: EmailEventListener) => void;
  removeEventListener?: (type: string, listener: EmailEventListener) => void;
  onerror: ((event: Event) => void) | null;
  readyState?: number;
}

class SseResponseError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

const createMessageEvent = (type: string, data: string): MessageEvent<string> => {
  if (typeof MessageEvent === "function") {
    return new MessageEvent(type, { data });
  }
  return { type, data } as unknown as MessageEvent<string>;
};

const createErrorEvent = (detail: unknown): Event => {
  if (typeof ErrorEvent === "function") {
    return new ErrorEvent("error", { error: detail });
  }
  if (typeof CustomEvent === "function") {
    return new CustomEvent("error", { detail });
  }
  if (typeof Event === "function") {
    return new Event("error");
  }
  return { type: "error" } as unknown as Event;
};

const getCookieValue = (name: string): string | null => {
  if (typeof document === "undefined") {
    return null;
  }
  const cookies = document.cookie ? document.cookie.split(";") : [];
  for (const entry of cookies) {
    const [rawKey, ...rest] = entry.trim().split("=");
    if (rawKey === name) {
      return decodeURIComponent(rest.join("="));
    }
  }
  return null;
};

const isErrorWithStatus = (value: unknown): value is { status: number } => {
  return typeof value === "object" && value !== null && "status" in value &&
    typeof (value as { status: unknown }).status === "number";
};

const resolveSseError = (event: Event): { status?: number; detail: unknown } => {
  const detail = (event as ErrorEvent).error ?? (event as CustomEvent).detail ?? event;
  const status = isErrorWithStatus(detail) ? detail.status : undefined;
  return { status, detail };
};

const isAuthFailure = (status?: number): boolean => status === 401 || status === 403;

const BASE_SSE_RETRY_DELAY_MS = 1000;
const MAX_SSE_RETRY_DELAY_MS = 30000;
let sseRetryCount = 0;
let sseRetryTimer: number | null = null;

const clearSseRetryTimer = () => {
  if (sseRetryTimer !== null && typeof window !== "undefined") {
    window.clearTimeout(sseRetryTimer);
    sseRetryTimer = null;
  }
};

const resetSseRetry = () => {
  sseRetryCount = 0;
  clearSseRetryTimer();
};

const scheduleSseReconnect = (store: any, authenticationEnabled?: boolean) => {
  if (typeof window === "undefined") {
    return;
  }
  clearSseRetryTimer();
  const attempt = Math.min(sseRetryCount, 5);
  const baseDelay = Math.min(MAX_SSE_RETRY_DELAY_MS, BASE_SSE_RETRY_DELAY_MS * 2 ** attempt);
  const delay = baseDelay + Math.floor(Math.random() * 250);
  sseRetryCount += 1;
  sseRetryTimer = window.setTimeout(() => {
    sseRetryTimer = null;
    setupEmailEventSource(store, authenticationEnabled);
  }, delay);
};

class AuthenticatedEventSource implements EmailEventSourceLike {
  static readonly CONNECTING = typeof EventSource !== "undefined" ? EventSource.CONNECTING : 0;
  static readonly OPEN = typeof EventSource !== "undefined" ? EventSource.OPEN : 1;
  static readonly CLOSED = typeof EventSource !== "undefined" ? EventSource.CLOSED : 2;

  public readyState: number = AuthenticatedEventSource.CONNECTING;
  public onerror: ((event: Event) => void) | null = null;

  private readonly controller = new AbortController();
  private readonly listeners = new Map<string, Set<EmailEventListener>>();
  private readonly url: string;
  private readonly headers: Record<string, string>;

  constructor(url: string, headers: Record<string, string>) {
    this.url = url;
    this.headers = headers;
    this.connect();
  }

  private async connect() {
    try {
      const response = await fetch(this.url, {
        headers: {...this.headers, Accept: "text/event-stream"},
        credentials: "same-origin",
        signal: this.controller.signal,
      });

      if (!response.ok || !response.body) {
        throw new SseResponseError(response.status, `SSE request failed with status ${response.status}`);
      }

      this.readyState = AuthenticatedEventSource.OPEN;

      const reader = response.body.getReader();
      const decoder = new TextDecoder("utf-8");
      let buffer = "";

      while (!this.controller.signal.aborted) {
        const {value, done} = await reader.read();
        if (done) {
          break;
        }
        buffer += decoder.decode(value, {stream: true});

        const segments = buffer.split("\n\n");
        buffer = segments.pop() ?? "";

        for (const segment of segments) {
          if (segment.trim().length === 0) {
            continue;
          }
          this.processSegment(segment);
        }
      }

      if (buffer.trim().length > 0) {
        this.processSegment(buffer);
      }

      if (!this.controller.signal.aborted) {
        this.dispatchError(new Error("SSE stream closed"));
      }
    } catch (error) {
      if (this.controller.signal.aborted) {
        return;
      }
      this.dispatchError(error);
    } finally {
      this.readyState = AuthenticatedEventSource.CLOSED;
    }
  }

  private processSegment(segment: string) {
    const lines = segment.split(/\r?\n/);
    let eventName = "message";
    const dataLines: string[] = [];

    for (const rawLine of lines) {
      const line = rawLine.trim();
      if (line.startsWith("event:")) {
        const value = line.slice(6).trim();
        if (value.length > 0) {
          eventName = value;
        }
      } else if (line.startsWith("data:")) {
        dataLines.push(line.slice(5).trimStart());
      }
    }

    const data = dataLines.join("\n");
    this.dispatchEvent(eventName, data);
  }

  private dispatchEvent(eventName: string, data: string) {
    const event = createMessageEvent(eventName, data);
    const listeners = this.listeners.get(eventName);
    if (listeners) {
      listeners.forEach(listener => listener(event));
    }
  }

  private dispatchError(error: unknown) {
    const event = createErrorEvent(error);
    if (this.onerror) {
      this.onerror(event);
    }
    const listeners = this.listeners.get("error");
    if (listeners) {
      listeners.forEach(listener => listener(event as unknown as MessageEvent<string>));
    }
  }

  addEventListener(type: string, listener: EmailEventListener) {
    if (!this.listeners.has(type)) {
      this.listeners.set(type, new Set());
    }
    this.listeners.get(type)!.add(listener);
  }

  removeEventListener(type: string, listener: EmailEventListener) {
    this.listeners.get(type)?.delete(listener);
  }

  close() {
    if (!this.controller.signal.aborted) {
      this.controller.abort();
    }
    this.readyState = AuthenticatedEventSource.CLOSED;
    this.listeners.clear();
  }
}

const rawBaseQuery = fetchBaseQuery({
  baseUrl: `${(getBasePathString())}/api`,
  credentials: "same-origin",
  prepareHeaders: (headers) => {
    const csrfToken = getCookieValue("XSRF-TOKEN");
    if (csrfToken) {
      headers.set("X-XSRF-TOKEN", csrfToken);
    }

    return headers;
  },
  // Custom fetch function to handle AbortSignal in tests
  fetchFn: async (input, init) => {
    // In test environment, create a new init object without the signal
    // to avoid AbortSignal compatibility issues with MSW
    if (process.env.NODE_ENV === 'test') {
      const { signal, ...restInit } = init || {};
      return fetch(input, restInit);
    }
    // In production, use the normal fetch with all parameters
    return fetch(input, init);
  },
});

const baseQueryWithAuthHandling: BaseQueryFn<string | FetchArgs, unknown, FetchBaseQueryError> = async (
  args,
  api,
  extraOptions
) => {
  const result = await rawBaseQuery(args, api, extraOptions);
  if (result.error && (result.error.status === 401 || result.error.status === 403)) {
    api.dispatch(clearAuthentication());
  }
  return result;
};

export const restApi = createApi({
  reducerPath: "restApi",
  baseQuery: baseQueryWithAuthHandling,
  tagTypes: ["Emails"],
  endpoints: (builder) => ({
    login: builder.mutation<void, Credentials>({
      query: (credentials) => {
        const body = new URLSearchParams({
          username: credentials.username,
          password: credentials.password,
        });
        return {
          url: "/auth/login",
          method: "POST",
          headers: {
            "Content-Type": "application/x-www-form-urlencoded",
          },
          body,
        };
      },
    }),
    logout: builder.mutation<void, void>({
      query: () => ({
        url: "/auth/logout",
        method: "POST",
      }),
    }),
    getEmails: builder.query<EmailPage, Pageable>({
      query: (p) => `/emails?page=${p.page}&size=${p.pageSize}`,
      providesTags: (result) =>
          result ? [
            ...result.content.map(({id}) => ({type: "Emails", id} as const)),
            {type: "Emails", id: "LIST"},
          ] : [{type: "Emails", id: "LIST"}],
    }),
    getEmail: builder.query<Email, string>({
      query: (id) => `/emails/${id}`,
      providesTags: (result, error, id) => [{type: "Emails", id}],
    }),
    deleteAllEmails: builder.mutation<{ success: boolean; id: string }, void>({
      query() {
        return {
          url: `emails`,
          method: "DELETE",
        }
      },
      invalidatesTags: (_) => ["Emails"],
    }),
    deleteEmail: builder.mutation<{ success: boolean; id: string }, string>({
      query(id) {
        return {
          url: `emails/${id}`,
          method: "DELETE",
        }
      },
      invalidatesTags: (result, error, id) => [{type: "Emails", id}],
    }),
    getMetaData: builder.query<MetaData, void>({
      query: () => `/meta-data`,
    }),
  }),
})

export const setupEmailEventSource = (store: any, authenticationEnabled?: boolean) => {
  const state = store.getState() as RootState;
  const {isAuthenticated} = state.auth;

  const shouldConnect = authenticationEnabled === false || isAuthenticated;

  if (!shouldConnect) {
    resetSseRetry();
    if (window.emailEventSource) {
      window.emailEventSource.close();
      window.emailEventSource = undefined;
    }
    console.log("SSE connection not established: authentication required but user not logged in");
    return undefined;
  }

  if (window.emailEventSource) {
    window.emailEventSource.close();
  }

  const basePath = getBasePathString();
  const url = `${basePath}/api/emails/events`;

  let eventSource: EmailEventSourceLike;

  if (authenticationEnabled) {
    eventSource = new AuthenticatedEventSource(url, {});
  } else {
    eventSource = new EventSource(url);
  }

  window.emailEventSource = eventSource;
  window.sseLastPingTimestamp = Date.now(); // Initialize timestamp on connection start

  const connectionEstablishedListener = (event: MessageEvent<string>) => {
    console.log("SSE connection established:", event.data);
    window.sseLastPingTimestamp = Date.now(); // Reset timestamp when connection is confirmed
    resetSseRetry();
  };

  const emailReceivedListener = (event: MessageEvent<string>) => {
    const emailId = event.data;
    console.log("New email received:", emailId);

    // Track event for UI animation
    window.sseLastEventTimestamp = Date.now();
    window.sseLastEventType = 'email';

    store.dispatch(restApi.util.invalidateTags([{type: "Emails", id: "LIST"}]));
  };

  // Heartbeat tracking
  let lastPingTimestamp = Date.now();
  const HEALTHY_TIMEOUT_MS = 60000; // 60s without ping = reconnect
  let healthCheckInterval: number | null = null;

  const pingListener = (event: MessageEvent<string>) => {
    lastPingTimestamp = Date.now();
    window.sseLastPingTimestamp = lastPingTimestamp;

    // Track event for UI animation
    window.sseLastEventTimestamp = lastPingTimestamp;
    window.sseLastEventType = 'ping';

    console.debug("SSE heartbeat received");
  };

  const startHealthCheck = () => {
    if (typeof window === "undefined") return;
    
    healthCheckInterval = window.setInterval(() => {
      const timeSinceLastPing = Date.now() - lastPingTimestamp;
      if (timeSinceLastPing > HEALTHY_TIMEOUT_MS) {
        console.warn("SSE heartbeat timeout after " + (timeSinceLastPing / 1000) + "s - reconnecting");
        resetSseRetry();
        eventSource.close();
        window.emailEventSource = undefined;
        scheduleSseReconnect(store, authenticationEnabled);
      }
    }, 10000); // Check every 10s
  };

  const stopHealthCheck = () => {
    if (healthCheckInterval !== null && typeof window !== "undefined") {
      window.clearInterval(healthCheckInterval);
      healthCheckInterval = null;
    }
  };

  // Wrap close method to cleanup health check
  const originalClose = eventSource.close.bind(eventSource);
  eventSource.close = () => {
    stopHealthCheck();
    originalClose();
  };

  eventSource.addEventListener("connection-established", connectionEstablishedListener);
  eventSource.addEventListener("email-received", emailReceivedListener);
  eventSource.addEventListener("ping", pingListener);

  // Start health check monitoring
  startHealthCheck();

  eventSource.onerror = (event: Event) => {
    const { status, detail } = resolveSseError(event);
    if (isAuthFailure(status)) {
      console.warn("SSE authentication failed; logging out.");
      resetSseRetry();
      stopHealthCheck();
      eventSource.close();
      window.emailEventSource = undefined;
      store.dispatch(clearAuthentication());
      return;
    }
    console.error("SSE connection error:", detail);
    stopHealthCheck();
    const closedState = typeof EventSource !== "undefined" ? EventSource.CLOSED : AuthenticatedEventSource.CLOSED;
    if (eventSource.readyState === undefined || eventSource.readyState === closedState) {
      scheduleSseReconnect(store, authenticationEnabled);
    }
  };

  return eventSource;
};

declare global {
  interface Window {
    emailEventSource?: EmailEventSourceLike;
    sseLastPingTimestamp?: number;
    sseLastEventTimestamp?: number;
    sseLastEventType?: 'ping' | 'email';
  }
}

export const {useGetEmailsQuery} = restApi
export const {useGetEmailQuery} = restApi
export const {useDeleteAllEmailsMutation} = restApi
export const {useDeleteEmailMutation} = restApi
export const {useGetMetaDataQuery} = restApi
export const {useLoginMutation} = restApi
export const {useLogoutMutation} = restApi
