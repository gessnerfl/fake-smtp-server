# Frontend Features

See the [feature documentation overview](./features-overview.md) for the document map and companion guides.

This document describes the user-facing behavior of the React/Vite web UI. It focuses on what the UI renders, when it blocks access, how it reacts to backend state, and how it handles failure and reconnect scenarios.

## Application Shell And Routing

- The SPA mounts two global background managers before rendering routes:
  - an SSE manager for email events,
  - a session timeout manager for auth/session revalidation.
- The route structure is intentionally small:
  - `/` renders the inbox,
  - `/emails/:id` renders a standalone email detail view.
- The shell always bootstraps itself from `GET /api/meta-data` before deciding what to render.
- While that bootstrap request is still loading, the main content area shows `Loading...`.
- If the bootstrap request fails, the shell hides protected content and shows an error alert with a `Retry` action instead of rendering stale UI.
- Once meta-data is available, the shell branches into three modes:
  - login screen when authentication is enabled and the user is not authenticated,
  - protected content when authentication is disabled or the user is already authenticated,
  - error state if the initial bootstrap failed.

## Authentication And Login UX

- The login form is rendered only when the backend reports `authenticationEnabled = true` and the Redux auth state is not authenticated.
- The form contains username and password fields and disables the submit button until both fields are populated.
- During sign-in, the submit button switches from `Sign In` to `Signing In...` and stays disabled.
- A successful login:
  - submits `POST /api/auth/login` as `application/x-www-form-urlencoded`,
  - refetches `/api/meta-data`,
  - marks the client as authenticated.
- Login errors are normalized into user-facing messages:
  - `401` or equivalent unauthorized failures become `Invalid username or password. Please try again.`
  - fetch/network failures become `Network error. Please check your connection.`
  - any other failure becomes a generic login failure or unexpected-error message.
- Editing the username or password clears the currently displayed auth error so the form does not keep showing stale feedback while the user retries.

## Navigation, Logout, And Connection Status

- The top bar always shows:
  - the product name,
  - the backend-reported version,
  - a logout button when authentication is enabled and the current client session is authenticated.
- Logout is handled via `POST /api/auth/logout`.
- If logout fails, the UI keeps the user in the authenticated state and shows `Logout failed. Please try again.` instead of clearing local auth state prematurely.
- Stale logout errors are cleared automatically when authentication is turned off or the user is no longer authenticated.
- The realtime status indicator is shown only when the UI is in a state where an SSE connection is allowed:
  - authentication is disabled, or
  - the user is authenticated.
- The indicator communicates three states:
  - `Connected (last ping: Ns ago)` when the SSE stream is open,
  - `Reconnecting...` while the stream is in a connecting state,
  - `Disconnected` when no usable connection is available.
- The indicator also animates:
  - a short pulse on heartbeat events,
  - a longer, stronger pulse when a new email event arrives.

## Session Timeout And Revalidation

- Client-side session supervision is active only while the UI is authenticated.
- The UI stores two non-secret tracking values in `sessionStorage`:
  - the timestamp of the last known activity,
  - the currently effective session timeout in milliseconds.
- The timeout value comes from backend meta-data when available. If the backend does not provide a valid timeout, the frontend falls back to a default of 10 minutes.
- The session manager tracks user activity on:
  - mouse movement,
  - mouse down,
  - keyboard input,
  - scroll,
  - touch start,
  - window focus.
- Activity-triggered revalidation is throttled so the UI does not hit `/api/meta-data` on every single interaction burst.
- The client revalidates the session in three cases:
  - when the inactivity timer expires,
  - when a tracked activity checkpoint fires,
  - when the document becomes visible again.
- If a revalidation response says `authenticated = false`, the UI:
  - clears local auth state,
  - removes the session-tracking values from `sessionStorage`,
  - stops the current timeout cycle.
- If a revalidation request fails with `401` or `403`, the UI treats that as an expired/invalid session and clears auth state in the same way.
- If revalidation succeeds, the UI refreshes the stored timeout and last-activity timestamp and arms a fresh inactivity timer.

## Inbox View

- The inbox is rendered at `/`.
- Pagination is server-driven and encoded in the URL query string:
  - `page`
  - `pageSize`
- This makes inbox views shareable and reload-stable.
- `pageSize` accepts the default sizes `10`, `25`, `50`, and `100`. If a custom size is present in the URL, it is preserved in the selector as long as it is not above `100`.
- Email loading is skipped until the bootstrap meta-data is known and, when authentication is enabled, until the user is authenticated.
- The inbox grid shows the columns:
  - `ID`
  - `From`
  - `To`
  - `Subject`
  - `Received On`
- The grid is intentionally locked down as a thin inbox view:
  - server-side pagination,
  - compact density,
  - no sorting,
  - no column filter UI,
  - no column menu,
  - no column hiding.
- When exactly one row is selected, the UI switches into a split view:
  - the inbox grid stays visible,
  - the selected email is rendered beside it on extra-large screens and stacked below it on smaller screens.
- If the selected row no longer resolves to a loaded email, the split pane shows `Email not found!`.
- Inbox states are explicit:
  - `Loading emails…` while email data is still loading,
  - `Please sign in to view emails.` when auth is enabled and the user is not logged in,
  - `Inbox is empty` when the backend returns no rows.

## Delete Flows

- Single-email deletion is only available when exactly one email is selected.
- Clicking `Delete` opens a confirmation dialog titled `Delete Email <id>`.
- The single-delete dialog uses explicit `Yes` and `No` actions; cancelling leaves the inbox unchanged.
- Bulk deletion is exposed via `Delete All`.
- `Delete All` is disabled when no emails are currently available.
- Clicking `Delete All` opens a separate confirmation dialog before any delete request is sent.

## Email Detail View

- The direct detail route `/emails/:id` renders a standalone detail card for one email.
- The page header includes a close button that returns to `/`.
- If the route parameter is missing or the email cannot be loaded, the page shows a blocking error alert instead of a partial card.
- The card itself is composed from:
  - a header section with the email metadata,
  - a content section with tabs,
  - an attachment section when attachments exist.

## Email Content Rendering

- The detail view always offers a `Raw` tab.
- `Html` and `Plain` tabs are rendered only when corresponding parsed content exists.
- When parsed content exists, the first available parsed content type becomes the initially selected tab for that email.
- Plain-text content is normalized to line-based paragraphs for display.
- Raw content is rendered verbatim in a `<pre>` block.
- HTML content is sanitized before rendering.
- HTML rendering also resolves `cid:` image references against stored inline images when:
  - the matching inline image exists,
  - it was not marked `SKIPPED_TOO_LARGE`,
  - binary data is available.
- Skipped inline images are not converted into inline `data:` URLs, so the rendered HTML keeps only the resolvable inline assets.

## Attachments

- The attachment panel is rendered only when the email has attachments.
- Normal attachments are exposed as direct download links to `/api/emails/{mailId}/attachments/{attachmentId}`.
- Attachments marked `SKIPPED_TOO_LARGE` are rendered differently:
  - the button label gets a ` (skipped)` suffix,
  - the button is disabled,
  - the tooltip explains why the attachment was skipped, using the backend-provided processing message when available.

## Scope Boundary

- The frontend currently focuses on browsing, inspecting, and deleting captured emails.
- The backend already provides richer search capabilities, but there is no dedicated search UI in the current web interface yet.
