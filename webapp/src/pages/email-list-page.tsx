import React, {useEffect, useState} from "react";
import {DataGrid, GridColDef, GridRowSelectionModel} from '@mui/x-data-grid';
import {useGetEmailsQuery} from "../store/rest-api";
import {Email} from "../models/email";
import {useSearchParams} from "react-router-dom";
import Grid from '@mui/material/Grid2';
import {EmailCard} from "../components/email/email-card";
import {Alert, Card, CardContent, CardHeader} from "@mui/material";
import {parseJSON} from "date-fns";
import {DeleteEmailButton} from "../components/email/delete-email-button";
import './email-list-page.tsx.scss'
import {DeleteAllEmailsButton} from "../components/email/delete-all-emails-button";

function EmailListPage() {
    const pageQueryParameter = "page";
    const pageSizeQueryParameter = "pageSize";
    const noRowSelected = -1;

    const [pageSizeOptions, setPageSizeOptions] = useState([10, 25, 50, 100])
    const [pageSize, setPageSize] = useState(10)
    const [page, setPage] = useState(0)
    const [selectedRow, setSelectedRow] = useState(noRowSelected)
    const [searchParams, setSearchParams] = useSearchParams()
    const {data, isLoading, refetch} = useGetEmailsQuery({page: page, pageSize: pageSize})
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
        return {...e, receivedOn: parseJSON(e.receivedOn)}
    }

    function formatPageQueryParam(page: number, pageSize: number) {
        return `${pageQueryParameter}=${page}&${pageSizeQueryParameter}=${pageSize}`;
    }

    function navigateTo(nextPage: number, newPageSize: number) {
        if (page !== nextPage || pageSize !== newPageSize) {
            setSearchParams(formatPageQueryParam(nextPage, newPageSize))
        }
    }

    useEffect(() => {
        const pageString = searchParams.get(pageQueryParameter)
        let fetchNeeded = false
        if (pageString != null) {
            const page = parseInt(pageString)
            setPage(isNaN(page) ? 0 : page)
            fetchNeeded = true
        }
        const pageSizeString = searchParams.get(pageSizeQueryParameter)
        if (pageSizeString != null) {
            const defaultPageSizeOptions = [10, 25, 50, 100]
            const parsedPageSize = parseInt(pageSizeString)
            const pageSize = parsedPageSize > 100 ? 100 : parsedPageSize
            const pageSizeOptions = defaultPageSizeOptions.indexOf(pageSize) > -1 ? defaultPageSizeOptions : [...defaultPageSizeOptions, pageSize].sort((a,b) => a > b ? 1 : -1)
            setPageSize(isNaN(pageSize) ? 10 : pageSize)
            setPageSizeOptions(pageSizeOptions)
            fetchNeeded = true
        }
        if(fetchNeeded){
            refetch()
        }
    }, [searchParams, refetch])

    function toSelectedRow(selection: GridRowSelectionModel): number {
        const rowId = selection.length > 0 ? selection.at(0) : undefined
        return rowId ? parseInt(rowId.toString()) : noRowSelected
    }

    function renderGrid() {
        return <div>
            <div className={"toolbar"}>
                <DeleteEmailButton selectedEmail={getSelectedEmail()}/>
                <DeleteAllEmailsButton emailsAvailable={data !== undefined && data.numberOfElements > 0}/>
            </div>
            <DataGrid
                columns={columns}
                rows={data!.content.map(e => transformEmail(e))}
                initialState={{
                    pagination: {
                        paginationModel: {
                            pageSize: pageSize,
                            page: page
                        },
                    },
                }}
                rowSelectionModel={selectedRow > 0 ? [selectedRow] : []}
                rowCount={data!.totalElements}
                pageSizeOptions={pageSizeOptions}
                loading={isLoading}
                paginationMode={'server'}
                autoHeight={true}
                density={'compact'}
                onPaginationModelChange={(m, _) => navigateTo(m.page, m.pageSize)}
                onRowSelectionModelChange={(m, _) => setSelectedRow(toSelectedRow(m))}
            />
        </div>;
    }

    function getSelectedEmail(): Email | undefined {
        return data?.content.find(e => e.id === selectedRow);
    }

    function renderEmail() {
        const email = getSelectedEmail()
        if (email) {
            return <EmailCard email={email}/>
        }
        return <Alert severity="error">Email not found!</Alert>
    }

    function renderSplitView() {
        return <Grid container spacing={2}>
            <Grid size={{xs:12, xl:6}}>{renderGrid()}</Grid>
            <Grid size={{xs:12, xl:6}}>{renderEmail()}</Grid>
        </Grid>
    }

    function renderData() {
        if (data) {
            if (selectedRow && selectedRow !== noRowSelected) {
                return renderSplitView()
            }
            return (
                renderGrid()
            )
        }
        return (<div>Inbox is empty</div>);
    }

    return (
        <Card>
            <CardHeader title={"Inbox"}/>
            <CardContent>
                {renderData()}
            </CardContent>
        </Card>
    )
}

export default EmailListPage;