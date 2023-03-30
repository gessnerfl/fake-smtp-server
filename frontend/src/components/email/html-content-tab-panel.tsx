import React, {FunctionComponent} from "react";
import {EmailContentTabPanelProperties} from "./email-content-tab-panel-properties";
import DOMPurify from "dompurify";
import {Box} from "@mui/material";

export const HtmlContentTabPanel: FunctionComponent<EmailContentTabPanelProperties> = ({activeContentType, email, data}) => {

    function getInlineImage(cid: string): (string | undefined) {
        const image = email.inlineImages.find(i => i.contentId === cid)
        if (image) {
            return `data:${image.contentType};base64,${image.data}`
        }
    }

    function mapInlineImages(data: string): string {
        const matches = data.matchAll(/<img[^>]+src=(?:"cid:([^">]+)"|'cid:([^'>]+)')/g)
        let match = matches.next()
        let mappedData = data
        while (match.value) {
            const cid = match.value[1];
            const image = getInlineImage(cid)
            if (image) {
                mappedData = mappedData.replaceAll("cid:" + cid, image)
            }
            match = matches.next()
        }
        return mappedData
    }

    function formatHtml(): string {
        if (data) {
            const mappedData = mapInlineImages(data)
            return DOMPurify.sanitize(mappedData)
        }
        return ""
    }

    return (
        <div role="tabpanel" hidden={activeContentType !== "html"} id={"content-tabpanel-html"}
             aria-labelledby={"content-tab-html"}>
            {activeContentType === "html" && (
                <Box sx={{p: 3}}>
                    <div dangerouslySetInnerHTML={{__html: formatHtml()}}/>
                </Box>
            )}
        </div>
    );
}