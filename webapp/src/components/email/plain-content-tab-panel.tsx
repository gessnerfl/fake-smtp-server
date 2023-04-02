import React, {FunctionComponent} from "react";
import {EmailContentTabPanelProperties} from "./email-content-tab-panel-properties";
import {Box} from "@mui/material";

export const PlainContentTabPanel: FunctionComponent<EmailContentTabPanelProperties> = ({activeContentType, data}) => {
    function formatData(data: string) {
        const parts = data.replaceAll("\r\n", "\n").split("\n")
        return (<div>{parts.map((v, i) => (<p key={`plan-text-paragraph-${i}`}>{v}</p>))}</div>)
    }

    return (
        <div role="tabpanel" hidden={activeContentType !== "plain"} id={"content-tabpanel-plain"}
             aria-labelledby={"content-tab-plain"}>
            {activeContentType === "plain" && (
                <Box sx={{p: 3}}>
                    {formatData(data)}
                </Box>
            )}
        </div>
    );
}