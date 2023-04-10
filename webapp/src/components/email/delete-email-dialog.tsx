import React, {FunctionComponent} from "react";
import {Button, Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle} from "@mui/material";
import {Email} from "../../models/email";

export interface DeleteEmailDialogProperties {
    email?: Email
    open: boolean;
    onClose: (confirmed: boolean) => void;
}

export const DeleteEmailDialog : FunctionComponent<DeleteEmailDialogProperties> = ({email, open, onClose}) => {

    function handleOk(){
        onClose(true)
    }

    function handleCancel(){
        onClose(false)
    }

    return <Dialog sx={{ '& .MuiDialog-paper': { width: '80%', maxHeight: 435 } }} maxWidth="xs" open={open} >
        <DialogTitle>Delete Email {email?.id}</DialogTitle>
        <DialogContent dividers>
            <DialogContentText>
                Do you really want to delete email with id {email?.id}?
            </DialogContentText>
        </DialogContent>
        <DialogActions>
            <Button onClick={handleOk} variant="contained" color="error">Yes</Button>
            <Button autoFocus onClick={handleCancel} variant="contained">No</Button>
        </DialogActions>
    </Dialog>
}