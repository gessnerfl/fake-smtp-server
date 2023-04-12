import React, {FunctionComponent, useEffect} from "react";
import {EmailDetailsProperties} from "./email-details-properties";
import {Box, Tab, Tabs} from "@mui/material";
import {HtmlContentTabPanel} from "./html-content-tab-panel";
import {PlainContentTabPanel} from "./plain-content-tab-panel";
import {RawContentTabPanel} from "./raw-content-tab-panel";

export const EmailContentPanel: FunctionComponent<EmailDetailsProperties> = ({email}) => {
    const contentTabPrefix = "content-tab-"
    const htmlContentTypeName = "html"
    const plainContentTypeName = "plain"
    const rawContentTypeName = "raw"
    const htmlTabKey = contentTabPrefix + htmlContentTypeName;
    const plainTabKey = contentTabPrefix + plainContentTypeName;
    const rawTabKey = contentTabPrefix + rawContentTypeName;

    const [emailId, setEmailId] = React.useState(-1);
    const [selectedTabId, setSelectedTabId] = React.useState("raw");
    const [plainContentAvailable, setPlainContentAvailable] = React.useState(false);
    const [htmlContentAvailable, setHtmlContentAvailable] = React.useState(false);

    useEffect(() => {
        function hasContentOfType(contentType: string): boolean {
            return email.contents && email.contents.filter(c => contentType === c.contentType.toLowerCase()).length > 0
        }

        if (emailId !== email.id) {
            setSelectedTabId(email.contents && email.contents.length > 0 ? email.contents[0].contentType.toLowerCase() : "raw")
        }
        setHtmlContentAvailable(hasContentOfType(htmlContentTypeName))
        setPlainContentAvailable(hasContentOfType(plainContentTypeName))
        setEmailId(email.id)
    }, [email, selectedTabId, emailId])

    const handleTabChanged = (event: React.SyntheticEvent, newValue: string) => {
        setSelectedTabId(newValue);
    };

    function getContentOfType(contentType: string) {
        const content = email.contents && email.contents.find(c => contentType === c.contentType.toLowerCase())
        return content ? content.data : ""
    }

    return (
        <Box sx={{width: '100%'}} className={"email-content"}>
            <Box sx={{borderBottom: 1, borderColor: 'divider'}}>
                <Tabs value={selectedTabId} onChange={handleTabChanged} aria-label="basic tabs example">
                    {htmlContentAvailable &&
                        <Tab label="Html" id={htmlTabKey} key={htmlTabKey} aria-controls={htmlTabKey}
                             value={htmlContentTypeName}/>}
                    {plainContentAvailable &&
                        <Tab label="Plain" id={plainTabKey} key={plainTabKey} aria-controls={plainTabKey}
                             value={plainContentTypeName}/>}
                    <Tab label="Raw" id={rawTabKey} key={rawTabKey} aria-controls={rawTabKey}
                         value={rawContentTypeName}/>
                </Tabs>
            </Box>
            {htmlContentAvailable &&
                <HtmlContentTabPanel activeContentType={selectedTabId} email={email}
                                     data={getContentOfType(htmlContentTypeName)}/>}
            {plainContentAvailable &&
                <PlainContentTabPanel activeContentType={selectedTabId} email={email}
                                      data={getContentOfType(plainContentTypeName)}/>}
            <RawContentTabPanel activeContentType={selectedTabId} email={email} data={email.rawData}/>
        </Box>

    )
}