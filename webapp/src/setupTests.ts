import {Email, EmailPage} from "./models/email";
import {setupServer} from 'msw/node'
import {rest} from 'msw'
import {formatRFC3339} from "date-fns";
import {MetaData} from "./models/meta-data";

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

const generateTestData = () : Email[] => Array.from(Array(15).keys()).map(i => {
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

const pageSize = 10;
export let testData = generateTestData()

export const handlers = [
    rest.get('/api/emails', (req, res, ctx) => {
        const pageStr = req.url.searchParams.get('page')
        const page = pageStr !== null ? parseInt(pageStr) : 0
        if(page < 2) {
            const data = testData.slice(page, pageSize)
            const totalEntries = testData.length
            const totalPages = (testData.length / pageSize) + (testData.length % pageSize > 0 ? 1 :0)
            const pageData: EmailPage = {
                number: page,
                numberOfElements: data.length,
                size: data.length,
                totalPages: totalPages,
                totalElements: totalEntries,
                content: data
            }
            return res(ctx.json(pageData), ctx.delay(150))
        }
        return res(ctx.status(404), ctx.text("Not found"))
    }),
    rest.get('/api/emails/:emailId', (req, res, ctx) => {
        const {emailId} = req.params
        const id = typeof emailId === "string" ? parseInt(emailId) : undefined
        if (id) {
            const email = testData[id]
            return res(ctx.json(email), ctx.delay(150))
        }
        return res(ctx.status(404), ctx.text("Not found"))
    }),
    rest.get('/api/meta-data', (req, res, ctx) => {
        const metaData: MetaData = {version: "local"}
        return res(ctx.json(metaData), ctx.delay(150))
    }),
]

const server = setupServer(...handlers)

beforeAll(() => server.listen())
afterEach(() => {
    server.resetHandlers()
    testData = generateTestData()
})
afterAll(() => server.close())