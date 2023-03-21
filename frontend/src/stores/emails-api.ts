import {createApi, fetchBaseQuery} from "@reduxjs/toolkit/query/react";
import {EmailPage} from "../models/email";
import {Pageable} from "../models/pageable";

export const emailsApi = createApi({
    reducerPath: 'emailsApi',
    baseQuery: fetchBaseQuery({ baseUrl: '/api' }),
    endpoints: (builder) => ({
        getEmails: builder.query<EmailPage, Pageable>({
            query: (p) => `/emails?page=${p.page}&size=${p.pageSize}`,
        }),
    }),
})

export const { useGetEmailsQuery } = emailsApi
