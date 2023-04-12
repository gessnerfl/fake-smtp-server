import {createApi, fetchBaseQuery} from "@reduxjs/toolkit/query/react";
import {Email, EmailPage} from "../models/email";
import {Pageable} from "../models/pageable";
import {MetaData} from "../models/meta-data";

export const restApi = createApi({
    reducerPath: 'restApi',
    baseQuery: fetchBaseQuery({baseUrl: '/api'}),
    tagTypes: ['Emails'],
    endpoints: (builder) => ({
        getEmails: builder.query<EmailPage, Pageable>({
            query: (p) => `/emails?page=${p.page}&size=${p.pageSize}`,
            providesTags: (result) =>
                result ? [
                        ...result.content.map(({id}) => ({type: 'Emails', id} as const)),
                        {type: 'Emails', id: 'LIST'},
                    ] : [{type: 'Emails', id: 'LIST'}],
        }),
        getEmail: builder.query<Email, string>({
            query: (id) => `/emails/${id}`,
            providesTags: (result, error, id) => [{ type: 'Emails', id }],
        }),
        deleteAllEmails: builder.mutation<{ success: boolean; id: string }, void>({
            query() {
                return {
                    url: `emails`,
                    method: 'DELETE',
                }
            },
            invalidatesTags: (_) => ["Emails"],
        }),
        deleteEmail: builder.mutation<{ success: boolean; id: string }, string>({
            query(id) {
                return {
                    url: `emails/${id}`,
                    method: 'DELETE',
                }
            },
            invalidatesTags: (result, error, id) => [{ type: 'Emails', id }],
        }),
        getMetaData: builder.query<MetaData, void>({
            query: (p) => "/meta-data",
        }),
    }),
})

export const {useGetEmailsQuery} = restApi
export const {useGetEmailQuery} = restApi
export const {useDeleteAllEmailsMutation} = restApi
export const {useDeleteEmailMutation} = restApi
export const {useGetMetaDataQuery} = restApi
