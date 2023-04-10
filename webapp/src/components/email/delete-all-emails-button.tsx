import React, {FunctionComponent} from "react";
import {Button} from "@mui/material";
import FolderDeleteIcon from '@mui/icons-material/FolderDelete';
import {DeleteAllEmailsDialog} from "./delete-all-emails-dialog";
import {useDeleteAllEmailsMutation} from "../../store/rest-api";

export interface DeleteAllEmailButtonProperties {
    emailsAvailable: boolean
}

export const DeleteAllEmailButton: FunctionComponent<DeleteAllEmailButtonProperties> = ({emailsAvailable}) => {
    const [open, setOpen] = React.useState(false);
    const [deleteAllEmails] = useDeleteAllEmailsMutation()

    function onDialogClosed(confirmed: boolean) {
        if(confirmed){
            deleteAllEmails()
        }
        setOpen(false)
    }


    function openDialog(){
        setOpen(true)
    }

    return (
        <span>
            <Button color={"error"} variant="contained" disabled={!emailsAvailable} startIcon={<FolderDeleteIcon />} onClick={openDialog}>Delete All</Button>
            <DeleteAllEmailsDialog open={open} onClose={onDialogClosed} />
        </span>
    );
}