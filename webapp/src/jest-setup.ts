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
    rest.get('/api/emails/1', (req, res, ctx) => {
        return res(ctx.json(testEmail1), ctx.delay(150))
    })
]

const server = setupServer(...handlers)

beforeAll(() => server.listen())
afterEach(() => server.resetHandlers())
afterAll(() => server.close())