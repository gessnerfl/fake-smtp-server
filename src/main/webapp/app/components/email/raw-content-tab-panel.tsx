import React, {FunctionComponent} from "react";
import {EmailContentTabPanelProperties} from "./email-content-tab-panel-properties";
import {Box} from "@mui/material";

export const RawContentTabPanel: FunctionComponent<EmailContentTabPanelProperties> = ({activeContentType, data}) => {
    return (
        <div role="tabpanel" hidden={activeContentType !== "raw"} id={"content-tabpanel-raw"}
             aria-labelledby={"content-tab-raw"}>
            {activeContentType === "raw" && (
                <Box sx={{p: 3}}>
                    <pre>{data}</pre>
                </Box>
            )}
        </div>
    );
}