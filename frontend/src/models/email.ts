import {Page} from "./page";

interface EmailPart {
    id: number,
    data: string,
}

export interface EmailContent extends EmailPart{
    contentType: string
}

export interface InlineImage extends EmailPart{
    contentId: string
    contentType: string
}

export interface EmailAttachment {
    id: number,
    filename: string,
}

export interface Email {
    id: number,
    fromAddress: string,
    toAddress: string,
    subject: string,
    receivedOn: string,
    rawData: string,
    contents: EmailContent[],
    attachments: EmailAttachment[],
    inlineImages: InlineImage[],
}

export interface EmailPage extends Page<Email> {}