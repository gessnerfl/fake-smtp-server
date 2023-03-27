import {Page} from "./page";

export interface EmailContent {
    contentType: string
    data: string
}

export interface InlineImage {
    contentId: string
    contentType: string
    data: string
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