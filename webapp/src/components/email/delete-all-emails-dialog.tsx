import React, {FunctionComponent} from "react";
import {Button, Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle} from "@mui/material";

export interface DeleteAllEmailsDialogProperties {
    open: boolean;
    onClose: (confirmed: boolean) => void;
}

export const DeleteAllEmailsDialog : FunctionComponent<DeleteAllEmailsDialogProperties> = ({open, onClose}) => {

    function handleOk(){
        onClose(true)
    }

    function handleCancel(){
        onClose(false)
    }

    return <Dialog sx={{ '& .MuiDialog-paper': { width: '80%', maxHeight: 435 } }} maxWidth="xs" open={open} >
        <DialogTitle>Delete All Emails</DialogTitle>
        <DialogContent dividers>
            <DialogContentText>
                Do you really want to delete all emails?
            </DialogContentText>
        </DialogContent>
        <DialogActions>
            <Button onClick={handleOk} variant="contained" color="error">Yes</Button>
            <Button autoFocus onClick={handleCancel} variant="contained">No</Button>
        </DialogActions>
    </Dialog>
}