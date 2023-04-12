import React, {FunctionComponent} from "react";
import {Button} from "@mui/material";
import {Email} from "../../models/email";
import DeleteIcon from '@mui/icons-material/Delete';
import {DeleteEmailDialog} from "./delete-email-dialog";
import {useDeleteEmailMutation} from "../../store/rest-api";

export interface DeleteButtonProperties {
    selectedEmail?: Email
}

export const DeleteEmailButton: FunctionComponent<DeleteButtonProperties> = ({selectedEmail}) => {
    const [open, setOpen] = React.useState(false);
    const [deleteEmail] = useDeleteEmailMutation()

    function onDialogClosed(confirmed: boolean) {
        if(confirmed && selectedEmail){
            deleteEmail(selectedEmail.id.toString())
        }
        setOpen(false)
    }


    function openDialog(){
        if(selectedEmail){
            setOpen(true)
        } else {
            setOpen(false)
        }
    }

    return (
        <span>
            <Button variant="contained" disabled={selectedEmail === undefined} startIcon={<DeleteIcon />} onClick={openDialog}>Delete</Button>
            <DeleteEmailDialog email={selectedEmail} open={open} onClose={onDialogClosed} />
        </span>
    );
}