import { createApi, fetchBaseQuery } from "@reduxjs/toolkit/query/react";
import { Email, EmailPage } from "../models/email";
import { Pageable } from "../models/pageable";
import { MetaData } from "../models/meta-data";
import { getBasePath } from "../base-path";
import { RootState } from "./store";
import { Credentials } from "../models/auth";

function getBasePathString() {
  const path = getBasePath();
  return path ?? ""
}

export const restApi = createApi({
  reducerPath: "restApi",
  baseQuery: fetchBaseQuery({
    baseUrl: `${(getBasePathString())}/api`,
    prepareHeaders: (headers, {getState}) => {
      const state = getState() as RootState;
      const {credentials, isAuthenticated} = state.auth;

      if (isAuthenticated && credentials) {
        const auth = btoa(`${credentials.username}:${credentials.password}`);
        headers.set("Authorization", `Basic ${auth}`);
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
  }),
  tagTypes: ["Emails"],
  endpoints: (builder) => ({
    login: builder.mutation<void, Credentials>({
      query: (credentials) => {
        const auth = btoa(`${credentials.username}:${credentials.password}`);
        return {
          url: "/emails?page=0&size=1",
          method: "GET",
          headers: {
            Authorization: `Basic ${auth}`,
          },
        };
      },
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
  let url = `${basePath}/api/emails/events`;

  const eventSource = new EventSource(url);

  window.emailEventSource = eventSource;

  eventSource.addEventListener("connection-established", (event) => {
    console.log("SSE connection established:", event.data);
  });

  eventSource.addEventListener("email-received", (event) => {
    const emailId = event.data;
    console.log("New email received:", emailId);

    store.dispatch(restApi.util.invalidateTags([{type: "Emails", id: "LIST"}]));
  });

  eventSource.onerror = (error) => {
    console.error("SSE connection error:", error);
    setTimeout(() => {
      if (eventSource.readyState === EventSource.CLOSED) {
        setupEmailEventSource(store, authenticationEnabled);
      }
    }, 5000);
  };

  return eventSource;
};

declare global {
  interface Window {
    emailEventSource?: EventSource;
  }
}

export const {useGetEmailsQuery} = restApi
export const {useGetEmailQuery} = restApi
export const {useDeleteAllEmailsMutation} = restApi
export const {useDeleteEmailMutation} = restApi
export const {useGetMetaDataQuery} = restApi
export const {useLoginMutation} = restApi
