import {Email} from "./models/email";
import { setupServer } from 'msw/node'
import { rest } from 'msw'

export const testEmail1: Email = {
    id: 1,
    fromAddress: "sender@example.com",
    toAddress: "receiver@example.com",
    subject: "subject",
    receivedOn: new Date().toDateString(),
    rawData: "test raw content",
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