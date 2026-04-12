# Backend Features

See the [feature documentation overview](./features-overview.md) for the document map and companion guides.

This document describes the server-side behavior that matters to integrators, backend-oriented readers, and anyone who needs to understand how messages are accepted, stored, exposed, and cleaned up.

## SMTP Server Runtime

- Fake SMTP Server runs its SMTP listener in the same Spring Boot process as the web application.
- The default SMTP port is `8025`.
- The SMTP server can bind to all interfaces or to a configured bind address.
- SMTP worker execution can use virtual threads when enabled in the runtime.
- STARTTLS support is optional.
- When TLS is enabled and no explicit protocol override is configured, the SMTP server enables `TLSv1.3` and `TLSv1.2` by default.
- If an explicit TLS protocol list is configured, the server validates that every requested protocol is actually supported by the JVM before using it.
- A maximum SMTP message size can be configured and is enforced during mail submission.

## SMTP Command Surface

- The SMTP command registry includes:
  - `AUTH`
  - `DATA`
  - `EHLO`
  - `HELO`
  - `HELP`
  - `MAIL`
  - `NOOP`
  - `QUIT`
  - `RCPT`
  - `RSET`
  - `STARTTLS`
  - `VRFY`
  - `EXPN`
- `VRFY` is intentionally disabled and returns `502 VRFY command is disabled`.
- `EXPN` is intentionally disabled and returns `502 EXPN command is disabled`.
- When `requireTLS` is active, the command wrappers enforce TLS before SMTP operations that should not proceed on an unprotected session.
- When SMTP authentication is required, the command wrappers enforce authentication before mail-submission commands proceed.

## Message Acceptance And Filtering

- Recipient blocking happens at SMTP acceptance time:
  - blocked recipients are rejected before delivery processing starts.
- Sender/recipient regex filters are applied after the raw SMTP payload has been received.
- Filtered messages are ignored rather than persisted.
- Ignored messages do not continue into storage, SSE notification, or forwarding.

## Message Transformation

- Incoming SMTP payloads are converted into domain entities through `EmailFactory`.
- The conversion layer supports:
  - plain-text messages,
  - HTML messages,
  - `multipart/alternative`,
  - `multipart/mixed`,
  - `multipart/related`,
  - regular attachments,
  - inline images with `Content-ID` references.
- For multipart messages, the backend walks the MIME tree and classifies parts into content, attachments, or inline images.
- Inline images are associated by normalized `Content-ID`, including support for common angle-bracket-wrapped values.
- If MIME parsing fails at the message level, the server falls back to a plain-text email built from the raw SMTP payload instead of dropping the message entirely.

## Attachment And Inline Image Limits

- The backend enforces a separate maximum size for attachments and inline images.
- Oversized parts are skipped rather than truncated.
- When a part is skipped, the email itself remains persisted and visible in the UI and API.
- Skipped parts are marked with:
  - `processingStatus = SKIPPED_TOO_LARGE`
  - a backend-generated `processingMessage`
- Attachments requested through `/api/emails/{mailId}/attachments/{attachmentId}` return HTTP `413` when the stored part was skipped because it exceeded the configured size limit.
- Inline images requested through `/api/emails/{mailId}/inline-images/{inlineImageId}` also return HTTP `413` when they were skipped for size reasons.

## REST API

### Application Metadata

- `GET /api/meta-data`
  returns:
  - build version,
  - whether Web UI authentication is enabled,
  - whether the current request is authenticated,
  - the effective session timeout in minutes,
  - the SSE heartbeat interval in seconds.

### Email Listing And Retrieval

- `GET /api/emails`
  returns a paged list of stored emails.
- The default sorting is by `receivedOn` in descending order.
- `GET /api/emails/{id}`
  returns one stored email or fails if the email does not exist.

### Binary Downloads

- `GET /api/emails/{mailId}/attachments/{attachmentId}`
  returns:
  - a download response with `Content-Disposition`,
  - server-derived media type,
  - exact binary length for stored attachments.
- `GET /api/emails/{mailId}/inline-images/{inlineImageId}`
  returns inline-image bytes when the stored content type and Base64 payload are valid.
- Invalid stored inline-image content types return HTTP `422` with a plain-text error body.
- Invalid stored inline-image Base64 payloads also return HTTP `422` with a plain-text error body.

### Delete Operations

- `DELETE /api/emails/{id}`
  deletes a single email.
- `DELETE /api/emails`
  deletes all stored emails.
- Single-email deletion throws a not-found error if the target ID does not exist.

### Search API

- `POST /api/emails/search`
  provides a backend-side search DSL with:
  - nested boolean expressions such as `and`, `or`, and `not`,
  - comparison operators such as `equal`, `like`, and null/comparison operators,
  - sort definitions,
  - paging.
- This search surface exists on the backend even though the current browser UI does not yet expose a dedicated search screen.

### SSE Endpoint

- `GET /api/emails/events`
  opens a `text/event-stream` subscription.
- The server emits:
  - `connection-established`
  - `email-received`
  - periodic `ping`
- When Web UI authentication is enabled, the SSE endpoint requires an authenticated session.

## Persistence Model

- Captured emails are stored in an in-memory H2 database.
- Schema creation and evolution are handled through Flyway migrations.
- The core persisted structures are:
  - `Email`
  - `EmailContent`
  - `EmailAttachment`
  - `InlineImage`

## Retention And Deletion

- The application enforces a maximum retained email count through a scheduled retention job.
- The retention job runs on a configurable fixed delay and initial delay.
- When the stored email count exceeds the configured limit, the oldest emails are removed.
- Full deletion of all emails uses explicit batch deletes for attachments, content, inline images, and finally emails.
- Single-email deletion is flushed immediately after delete so the repository state is consistent for subsequent reads.

## Realtime Integration

- After a message is saved, the backend publishes an `email-received` event through the SSE emitter service.
- This lets the frontend invalidate and refetch the inbox without polling.
- SSE emitter lifecycle management also cleans up dead or timed-out connections on the server side.

## Optional Forwarding

- When `forwardEmails` is enabled, the backend forwards the received raw message to a configured target mail system.
- The preferred forwarding path reuses the original MIME message.
- If MIME conversion fails during forwarding, the backend falls back to a `SimpleMailMessage` built from sender, recipient, and raw content.
