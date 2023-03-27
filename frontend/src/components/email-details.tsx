import {Email} from "../models/email";
import React, {FunctionComponent, useEffect} from "react";
import {Box, Card, CardContent, Tabs, Tab, Typography, Unstable_Grid2 as Grid} from "@mui/material";
import formatISO9075 from "date-fns/formatISO9075"
import DOMPurify from "dompurify"

interface EmailDetailsProperties {
    email: Email
}

interface EmailContentTabPanelProperties {
    email: Email
    activeContentType: string
    data: string
}

const HtmlContentTabPanel: FunctionComponent<EmailContentTabPanelProperties> = ({activeContentType,  email, data}) => {

    function getInlineImage(cid: string) : (string | undefined) {
        const image = email.inlineImages.find(i => i.contentId === cid)
        if(image){
            return `data:${image.contentType};base64,${image.data}`
        }
    }

    function mapInlineImages(data: string) : string {
        const matches = data.matchAll(/<img[^>]+src=(?:"cid:([^">]+)"|'cid:([^'>]+)')/g)
        let match = matches.next()
        let mappedData = data
        while (match.value){
            const cid = match.value[1];
            const image = getInlineImage(cid)
            if(image){
                mappedData = mappedData.replaceAll("cid:"+cid, image)
            }
            match = matches.next()
        }
        return mappedData
    }

    function formatHtml() : string {
        if(data){
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
                    <div dangerouslySetInnerHTML={{ __html: formatHtml() }} />
                </Box>
            )}
        </div>
    );
}

const PlainContentTabPanel: FunctionComponent<EmailContentTabPanelProperties> = ({activeContentType, data}) => {
    function formatData(data: string) {
        const parts = data.replaceAll("\r\n", "\n").split("\n")
        return (<div>{parts.map((v,i) => (<p key={`plan-text-paragraph-${i}`}>{v}</p>))}</div>)
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

const RawContentTabPanel: FunctionComponent<EmailContentTabPanelProperties> = ({activeContentType, data}) => {
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

const EmailContentPanel: FunctionComponent<EmailDetailsProperties> = ({email}) => {
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

        if(emailId !== email.id) {
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
        <Box sx={{width: '100%'}}>
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
                <HtmlContentTabPanel activeContentType={selectedTabId} email={email} data={getContentOfType(htmlContentTypeName)}/>}
            {plainContentAvailable &&
                <PlainContentTabPanel activeContentType={selectedTabId} email={email} data={getContentOfType(plainContentTypeName)}/>}
            <RawContentTabPanel activeContentType={selectedTabId} email={email} data={email.rawData}/>
        </Box>

    )
}

const EmailHeader: FunctionComponent<EmailDetailsProperties> = ({email}) => {
    return (
        <Grid container component='dl' spacing={2}>
            <Grid xs={6}>
                <Typography component='dt' variant='subtitle2'>From:</Typography>
                <Typography component='dd' variant='body1'>{email.fromAddress}</Typography>
            </Grid>
            <Grid xs={6}>
                <Typography component='dt' variant='subtitle2'>To:</Typography>
                <Typography component='dd' variant='body1'>{email.toAddress}</Typography>
            </Grid>
            <Grid xs={6}>
                <Typography component='dt' variant='subtitle2'>ReceivedOn:</Typography>
                <Typography component='dd' variant='body1'>{formatISO9075(Date.parse(email.receivedOn))}</Typography>
            </Grid>
            <Grid xs={12}>
                <Typography component='dt' variant='subtitle2'>Subject:</Typography>
                <Typography component='dd' variant='body1'>{email.subject}</Typography>
            </Grid>
        </Grid>
    )
}

const EmailDetails: FunctionComponent<EmailDetailsProperties> = (props) => {
    return (
        <Card variant="outlined">
            <CardContent>
                <Typography sx={{fontSize: 14}} color="text.secondary" gutterBottom>Email {props.email.id}</Typography>
                <EmailHeader email={props.email}/>
                <EmailContentPanel email={props.email}/>
            </CardContent>
        </Card>
    );
}

export default EmailDetails;