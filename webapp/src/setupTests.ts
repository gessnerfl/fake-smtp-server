import {Email} from "./models/email";
import { setupServer } from 'msw/node'
import { rest } from 'msw'
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
export const handlers = [
    rest.get('/api/emails/:emailId', (req, res, ctx) => {
        const { emailId } = req.params
        if(emailId && emailId === ""+testEmail1.id){
            return res(ctx.json(testEmail1), ctx.delay(150))
        }
        return res(ctx.status(404), ctx.text("Not found"))
    })
]

const server = setupServer(...handlers)

beforeAll(() => server.listen())
afterEach(() => server.resetHandlers())
afterAll(() => server.close())