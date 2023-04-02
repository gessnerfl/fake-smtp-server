import {createApi, fetchBaseQuery} from "@reduxjs/toolkit/query/react";
import {Email, EmailPage} from "../models/email";
import {Pageable} from "../models/pageable";

export const emailsApi = createApi({
    reducerPath: 'emailsApi',
    baseQuery: fetchBaseQuery({ baseUrl: '/api' }),
    endpoints: (builder) => ({
        getEmails: builder.query<EmailPage, Pageable>({
            query: (p) => `/emails?page=${p.page}&size=${p.pageSize}`,
        }),
        getEmail: builder.query<Email, string>({
            query: (id) => `/emails/${id}`,
        }),
    }),
})

export const { useGetEmailsQuery } = emailsApi
export const { useGetEmailQuery } = emailsApi
