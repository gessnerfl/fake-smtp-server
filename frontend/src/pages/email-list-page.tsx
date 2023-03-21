import React, {useEffect, useState} from "react";
import {DataGrid, GridColDef} from '@mui/x-data-grid';
import {useGetEmailsQuery} from "../stores/emails-api";
import {Email} from "../models/email";
import {useSearchParams} from "react-router-dom";


function EmailListPage() {
    const pageQueryParameter = "page";

    const [page, setPage] = useState(0)
    const [searchParams, setSearchParams] = useSearchParams()
    const {data, isLoading, refetch} = useGetEmailsQuery({page: page, pageSize: 15})
    const columns: GridColDef[] = [
        {
            field: 'id',
            headerName: 'ID',
            type: 'number',
            flex: 0.1,
            sortable: false,
            filterable: false,
            hideable: false,
            disableColumnMenu: true
        },
        {
            field: 'fromAddress',
            headerName: 'From',
            flex: 0.2,
            sortable: false,
            filterable: false,
            hideable: false,
            disableColumnMenu: true
        },
        {
            field: 'toAddress',
            headerName: 'To',
            flex: 0.2,
            sortable: false,
            filterable: false,
            hideable: false,
            disableColumnMenu: true
        },
        {
            field: 'subject',
            headerName: 'Subject',
            flex: 0.2,
            sortable: false,
            filterable: false,
            hideable: false,
            disableColumnMenu: true
        },
        {
            field: 'receivedOn',
            headerName: 'Received On',
            type: 'dateTime',
            flex: 0.2,
            sortable: false,
            filterable: false,
            hideable: false,
            disableColumnMenu: true
        },
    ];

    function transformEmail(e: Email) {
        return {...e, receivedOn: new Date(e.receivedOn)}
    }

    function formatPageQueryParam(page: number) {
        return pageQueryParameter + "=" + page;
    }

    function navigateTo(nextPage: number) {
        if(page !== nextPage){
            setSearchParams(formatPageQueryParam(nextPage))
        }
    }

    useEffect(() => {
        const pageString = searchParams.get(pageQueryParameter)
        if(pageString != null){
            const page = parseInt(pageString)
            setPage(isNaN(page) ? 0 : page)
            refetch()
        }
    }, [searchParams, refetch])

    if (data) {
        return (<div>
            <DataGrid
                columns={columns}
                rows={data.content.map(e => transformEmail(e))}
                initialState={{
                    pagination: {
                        paginationModel: {
                            pageSize: 15,
                            page: page
                        },
                    },
                }}
                rowCount={data.totalElements}
                pageSizeOptions={[15]}
                loading={isLoading}
                paginationMode={"server"}
                autoHeight={true}
                onPaginationModelChange={(m,_) => navigateTo(m.page)}
            />
        </div>)
    }
    return (<div>Inbox is empty</div>);
}

export default EmailListPage;