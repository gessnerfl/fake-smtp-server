import {createApi, fetchBaseQuery} from "@reduxjs/toolkit/query/react";
import {Email, EmailPage} from "../models/email";
import {Pageable} from "../models/pageable";
import {MetaData} from "../models/meta-data";

export const restApi = createApi({
    reducerPath: 'restApi',
    baseQuery: fetchBaseQuery({ baseUrl: '/api' }),
    endpoints: (builder) => ({
        getEmails: builder.query<EmailPage, Pageable>({
            query: (p) => `/emails?page=${p.page}&size=${p.pageSize}`,
        }),
        getEmail: builder.query<Email, string>({
            query: (id) => `/emails/${id}`,
        }),
        getMetaData: builder.query<MetaData, void>({
            query: (p) => "/meta-data",
        }),
    }),
})

export const { useGetEmailsQuery } = restApi
export const { useGetEmailQuery } = restApi
export const { useGetMetaDataQuery } = restApi
