# Operations And Security Features

See the [feature documentation overview](./features-overview.md) for the document map and companion guides.

This document describes the runtime, security, and operator-facing behavior of Fake SMTP Server.

## Runtime Ports And Profiles

- The web application listens on port `8080` by default.
- The management server listens on port `8081` by default.
- The SMTP listener uses port `8025` by default.
- The application enables graceful shutdown and configures a short shutdown-phase timeout.
- The default runtime profile keeps the H2 console disabled.
- The `develop` profile enables:
  - Web UI authentication,
  - the H2 console,
  - shorter Web UI session defaults,
  - operator-friendly local development settings.

## Web UI Authentication

- Web UI authentication is session-based rather than token-based.
- Authentication can be enabled in two ways:
  - explicitly with `fakesmtp.webapp.authentication.enabled=true`,
  - implicitly by providing both username and password, although that implicit enablement is deprecated.
- When `enabled=true`, both username and password must be configured and non-empty.
- When `enabled=false`, username and password must not be configured.
- The authenticated Web UI user is backed by an in-memory Spring Security user.
- Concurrent session count is configurable and defaults to `1`.

## Public And Protected Surfaces

- When Web UI authentication is enabled:
  - `GET /`
  - `GET /emails/**`
  - `GET /assets/**`
  remain publicly reachable so the SPA shell can render the login screen.
- `GET /api/meta-data` remains public so the frontend can bootstrap itself.
- `/api/**` endpoints that expose email data require an authenticated session.
- `/api/emails/events` also requires an authenticated session when authentication is enabled.
- Swagger UI and OpenAPI documents remain reachable without login.
- The H2 console remains reachable without login when it is enabled.
- Actuator uses a separate security chain:
  - `/actuator/health` and `/actuator/info` are public,
  - other exposed Actuator endpoints require authentication.

## Session And Cookie Behavior

- The Web UI uses the normal Spring Security `JSESSIONID` session cookie.
- Session fixation protection is enabled through `changeSessionId()`.
- Session cookies are configured as:
  - `HttpOnly`
  - `SameSite=Lax`
- The effective Web UI session timeout comes from `fakesmtp.webapp.session.session-timeout-minutes`.
- Invalid or non-positive timeout values fall back to the default.
- Timeout values are capped at 24 hours on the backend.

## CSRF Model

- CSRF protection is enabled for the authenticated Web UI flow.
- The backend issues a CSRF token cookie named `XSRF-TOKEN`.
- The frontend sends the token back in the `X-XSRF-TOKEN` header.
- A fresh CSRF token is generated and saved again after successful login.
- State-changing API requests such as `DELETE /api/emails` require a valid CSRF token.
- The H2 console is excluded from CSRF enforcement.
- When Web UI authentication is disabled, CSRF checks for `/api/**` are skipped.

## Login Rate Limiting

- Login rate limiting is configurable under `fakesmtp.webapp.rate-limiting.*`.
- The defaults from `application.yaml` are:
  - enabled,
  - `5` attempts,
  - `1` minute window,
  - `1` minute cleanup interval,
  - localhost whitelisting enabled,
  - proxy-header trust disabled.
- Rate limiting applies to the login endpoint rather than to unrelated API routes.
- Failed login attempts emit the `X-RateLimit-Remaining` response header.
- When a client exceeds the configured limit, the login endpoint returns:
  - HTTP `429 Too Many Requests`
  - `Retry-After`
  - `X-RateLimit-Remaining: 0`
- Successful logins do not consume the remaining attempt budget.
- When localhost whitelisting is enabled, local requests are not rate limited.
- When proxy-header trust is enabled, the filter uses forwarded client IP information.
- When proxy-header trust is disabled, forwarded headers are ignored and the remote socket address is used instead.
- If Web UI authentication is disabled, login rate limiting becomes inactive.

## Management, Diagnostics, And Tooling

- The management server exposes `/actuator/health` and `/actuator/info` by default.
- If you expose additional endpoints such as `/actuator/metrics`, they remain protected.
- Protected Actuator endpoints use HTTP Basic authentication on the management port.
- Swagger UI is available at `/swagger-ui.html`.
- The H2 console is intended for development-oriented use and is enabled in the `develop` profile.
- `/api/meta-data` is intentionally lightweight and exposes application metadata for the SPA bootstrap instead of sensitive operational data.

## TLS And SMTP Authentication

- SMTP AUTH support is optional and can be configured with username and password.
- SMTP authentication being configured does not by itself mean that authentication is enforced for every SMTP session.
- SMTP AUTH command lines are redacted in debug logs so credentials are not written to logs verbatim.
- STARTTLS support is optional.
- `requireTLS` can be used to require a TLS handshake before protected SMTP commands proceed.
- When no explicit protocol override is configured, STARTTLS enables `TLSv1.3` and `TLSv1.2`.
- You can narrow the protocol list explicitly if compatibility testing requires it.

## Metrics

- Fake SMTP Server publishes Micrometer counters for:
  - `messages.delivered`
  - `messages.blocked`
- Sensitive email-address tags are disabled by default.
- Address tags can be enabled explicitly with `fakesmtp.metrics.include-address-tags=true`.
- Blocked-message metrics are incremented when a recipient is rejected by the blocklist.
- Delivered-message metrics are incremented when a message is accepted for delivery processing.

## Operational Behavior Worth Noting

- The frontend bootstrap and authenticated API flow are designed so email data is not embedded into the public SPA shell.
- SSE settings are operator-configurable through:
  - `session-timeout-minutes`
  - `sse-heartbeat-interval-seconds`
  - `sse-event-send-timeout-seconds`
- Attachment and message size limits are operational controls as well as feature behavior:
  - oversized attachments and inline images are skipped but the email stays visible,
  - oversized SMTP payloads are rejected during SMTP submission.
