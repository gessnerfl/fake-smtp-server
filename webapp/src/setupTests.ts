import "whatwg-fetch";
import { Email, EmailPage } from "./models/email";
import { setupServer } from 'msw/node'
import { http, HttpResponse, delay } from 'msw'
import { formatRFC3339 } from "date-fns";
import { MetaData } from "./models/meta-data";
import { endpointUrl } from "./test-utils";

export const testEmail1: Email = {
    id: 1,
    fromAddress: "sender@example.com",
    toAddress: "receiver@example.com",
    subject: "subject",
    receivedOn: formatRFC3339(new Date(2023, 3, 5, 21, 5, 10, 20), { fractionDigits: 3 }),
    rawData: "test raw content\non multiple lines",
    contents: [],
    inlineImages: [],
    attachments: []
}

export const testEmailWithAttachment: Email = {
    id: 1,
    fromAddress: "sender2@example.com",
    toAddress: "receiver2@example.com",
    subject: "subject2",
    receivedOn: formatRFC3339(new Date(2023, 3, 5, 21, 5, 10, 20), { fractionDigits: 3 }),
    rawData: "test raw content\non multiple lines 2",
    contents: [],
    inlineImages: [],
    attachments: [{ id: 1234, filename: "test1.png" }, { id: 2345, filename: "test2.png" }]
}

const generateTestData = (): Email[] => Array.from(Array(15).keys()).map(i => {
    return {
        id: i,
        fromAddress: `sender${i}@example.com`,
        toAddress: `receiver${i}@example.com`,
        subject: `subject${i}`,
        receivedOn: formatRFC3339(new Date(2023, 3, 5, 21, i + 1, 10, 20), { fractionDigits: 3 }),
        rawData: `test raw content${i}`,
        contents: [],
        inlineImages: [],
        attachments: []
    }
})

export const originalTestData = generateTestData()
export let testData = [...originalTestData]

export const handlers = [
    http.get(endpointUrl('/api/emails'), async ({ request }) => {
        const url = new URL(request.url)
        const pageStr = url.searchParams.get('page')
        const page = pageStr !== null ? parseInt(pageStr) : 0
        const pageSizeStr = url.searchParams.get('size')
        const pageSize = pageSizeStr !== null ? parseInt(pageSizeStr) : 10
        if (page < 2) {
            const data = testData.slice(page * pageSize, pageSize)
            const totalEntries = testData.length
            const totalPages = (testData.length / pageSize) + (testData.length % pageSize > 0 ? 1 : 0)
            const pageData: EmailPage = {
                number: page,
                numberOfElements: data.length,
                size: data.length,
                totalPages: totalPages,
                totalElements: totalEntries,
                content: data
            }
            await delay(150)
            return HttpResponse.json(pageData)
        }
        return new HttpResponse("Not found", { status: 404 })
    }),
    http.get(endpointUrl('/api/emails/:emailId'), async ({ params }) => {
        const { emailId } = params
        const id = typeof emailId === "string" ? parseInt(emailId) : undefined
        if (id) {
            const emails = testData.filter(e => e.id === id)
            if (emails.length > 0) {
                delay(150)
                return HttpResponse.json(emails[0])
            }
        }
        return new HttpResponse("Not found", { status: 404 })
    }),
    http.delete(endpointUrl('/api/emails'), () => {
        testData = []
        return new HttpResponse("Not found", { status: 404 })
    }),
    http.delete(endpointUrl('/api/emails/:emailId'), async ({ params }) => {
        const { emailId } = params
        const id = typeof emailId === "string" ? parseInt(emailId) : undefined
        if (id) {
            const idx = testData.findIndex(e => e.id === id)
            if (idx > 0) {
                testData.splice(idx, 1)
                await delay(150)
                return new HttpResponse("Deleted", { status: 200 })
            }
        }
        return new HttpResponse("Not found", { status: 404 })
    }),
    http.get(endpointUrl('/api/meta-data'), async () => {
        const metaData: MetaData = { version: "local" }
        await delay(150)
        return HttpResponse.json(metaData)
    }),
]

const server = setupServer(...handlers)

beforeAll(() => { 
    server.listen() 
})
afterEach(() => {
    server.resetHandlers()
    testData = [...originalTestData]
})
afterAll(() => server.close())