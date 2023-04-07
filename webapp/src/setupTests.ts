import {Email, EmailPage} from "./models/email";
import {setupServer} from 'msw/node'
import {rest} from 'msw'
import {formatRFC3339} from "date-fns";

export const testEmail1: Email = {
    id: 1,
    fromAddress: "sender@example.com",
    toAddress: "receiver@example.com",
    subject: "subject",
    receivedOn: formatRFC3339(new Date(2023, 3, 5, 21, 5, 10, 20), {fractionDigits: 3}),
    rawData: "test raw content\non multiple lines",
    contents: [],
    inlineImages: [],
    attachments: []
}

export const testEmailPage1: EmailPage = {
    number: 1,
    numberOfElements: 10,
    size: 10,
    totalPages: 2,
    totalElements: 15,
    content: Array.from(Array(10).keys()).map(i => {
        return {
            id: i,
            fromAddress: `sender${i}@example.com`,
            toAddress: `receiver${i}@example.com`,
            subject: `subject${i}`,
            receivedOn: formatRFC3339(new Date(2023, 3, 5, 21, i+1, 10, 20), {fractionDigits: 3}),
            rawData: `test raw content${i}`,
            contents: [],
            inlineImages: [],
            attachments: []
        }
    })
}

export const testEmailPage2: EmailPage = {
    number: 2,
    numberOfElements: 5,
    size: 5,
    totalPages: 2,
    totalElements: 15,
    content: Array.from(Array(5).keys()).map(i => {
        const index = i + 10;
        return {
            id: index,
            fromAddress: `sender${index}@example.com`,
            toAddress: `receiver${index}@example.com`,
            subject: `subject${index}`,
            receivedOn: formatRFC3339(new Date(2023, 3, 5, 21, index+1, 10, 20), {fractionDigits: 3}),
            rawData: `test raw content${index}`,
            contents: [],
            inlineImages: [],
            attachments: []
        }
    })
}

export const handlers = [
    rest.get('/api/emails', (req, res, ctx) => {
        const page = req.url.searchParams.get('page')
        if(page === null || "1" === page) {
            return res(ctx.json(testEmailPage1), ctx.delay(150))
        } else if("2" === page) {
            return res(ctx.json(testEmailPage2), ctx.delay(150))
        }
        return res(ctx.status(404), ctx.text("Not found"))
    }),
    rest.get('/api/emails/:emailId', (req, res, ctx) => {
        const {emailId} = req.params
        if (emailId && emailId === "" + testEmail1.id) {
            return res(ctx.json(testEmail1), ctx.delay(150))
        }
        return res(ctx.status(404), ctx.text("Not found"))
    })
]

const server = setupServer(...handlers)

beforeAll(() => server.listen())
afterEach(() => server.resetHandlers())
afterAll(() => server.close())