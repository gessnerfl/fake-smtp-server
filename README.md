# Fake SMTP Server
[![Build Status](https://github.com/gessnerfl/fake-smtp-server/workflows/CI%2FCD/badge.svg)](https://github.com/gessnerfl/fake-smtp-server/workflows/CI%2FCD/badge.svg)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=de.gessnerfl.fake-smtp-server&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=de.gessnerfl.fake-smtp-server)

*Simple SMTP Server which stores all received emails in an in-memory database and renders the emails in a web interface*

## Introduction

The Fake SMTP Server is a simple SMTP server which is designed for development purposes. The server collects all
received emails, stores the emails in an in-memory database and provides access to the emails via a web interface.

There is no POP3 or IMAP interface included by intention. The basic idea of this software is that it is used during 
development to configure the server as target mail server. Instead of sending the emails to a real SMTP server which 
would forward the mails to the target recipient or return with mail undelivery for test email addresses (e.g. 
@example.com) the server just accepts all mails, stores them in the database so that they can be rendered in the UI. 
This allows you to use any test mail address and check the sent email in the web application of the Fake SMTP Server.

The server store a configurable maximum number of emails.
If the maximum number of emails is exceeded old emails will be deleted to avoid that the system consumes too much memory.

The server is also provided as docker image on docker hub [gessnerfl/fake-smtp-server](https://hub.docker.com/r/gessnerfl/fake-smtp-server/).
To change configuration parameters the corresponding configuration values have to be specified as environment variables for the docker container.
For details check the Spring Boot (http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#boot-features-external-config)
and docker documentation (https://docs.docker.com/engine/reference/run/#env-environment-variables).

> [!NOTE]
> Starting with version 2.6.0 the Docker image is based on `bellsoft/liberica-runtime-container:jdk-25-slim-musl` (Alpaquita Linux with musl libc).
> This change significantly reduces the image size and improves startup performance.

# Running Fake SMTP Server locally

> [!NOTE]  
> Starting with version 2.6.0 Java 25 is required to run Fake SMTP Server.

> [!NOTE]  
> Starting with version 2.2.0 Java 21 is required to run Fake SMTP Server.

> [!NOTE]  
> Starting with version 2.0.0 Java 17 is required to run Fake SMTP Server.

## Run from released JAR files

1. Download the latest `fake-smtp-server-<version>.jar` from 
[https://github.com/gessnerfl/fake-smtp-server/releases/latest](https://github.com/gessnerfl/fake-smtp-server/releases/latest)
2. Copy the file into the desired target folder
3. Execute the following command from the folder where the JAR file is located:
   
```
java -jar fake-smtp-server-<version>.jar
```

## Run from sources

In order to run this application locally from sources, execute:

    ./gradlew bootRun

Afterwards, the web interface is be availabe under http://localhost:8080.

> [!IMPORTANT]
> The provided `docker-compose.yml` is a local development/test convenience file that uses the `develop` profile and fixed local ports. It is not intended or supported as a production deployment manifest.

# Configuration

As the application is based on Spring Boot the same rules applies to the configuration as described in the Spring Boot 
Documentation (http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#boot-features-external-config).

The configuration file application.yaml can be placed next to the application jar, in a sub-directory config or 
in any other location when specifying the location with the parameter `-Dspring.config.location=<path to config file>`.

All configuration parameters can also be passed as environment variables using uppercase characters and underscores as 
separators such as `SERVER_PORT` or `MANAGEMENT_SERVER_PORT`.

The following paragraphs describe the application specific resp. pre-defined configuration parameters.

## Fake SMTP Server
The following snippet shows the configuration of a fake smtp server with its default values.

```yaml
fakesmtp:
  #The SMTP Server Port used by the Fake SMTP Server
  port: 8025

  #The binding address of the Fake SMTP Server; Bound to all interfaces by default / no value
  bindAddress: 127.0.0.1

  #List of recipient addresses which should be blocked/rejected
  blockedRecipientAddresses:
    - blocked@example.com
    - foo@eample.com

  #List of sender email addresses to ignore, as a comma-separated list of regex expressions.
  filteredEmailRegexList: john@doe\\.com,.*@google\\.com ; empty by default

  #Optional configuration option to specify the maximum allowed message size. The size can be 
  #defined using Spring Boot DataSize value type - https://docs.spring.io/spring-boot/docs/2.1.9.RELEASE/reference/html/boot-features-external-config.html#boot-features-external-config-conversion-datasize.
  #Default: 10MB
  maxMessageSize: 10MB

  #Optional configuration option to specify the maximum allowed attachment/inline image size.
  #This prevents OutOfMemoryError when processing large email attachments.
  #The size can be defined using Spring Boot DataSize value type.
  #Default: 10MB
  maxAttachmentSize: 10MB

  #Configure if TLS is required to connect to the SMTP server. Defaults to false. See TLS section below
  requireTLS: false

  #Optional override for STARTTLS server protocols. When omitted, Fake SMTP only enables TLSv1.3 and TLSv1.2.
  tls-protocols:
    - TLSv1.3
    - TLSv1.2

  #When set to true emails will be forwarded to a configured target email system. Therefore
  #the spring boot mail system needs to be configured. See also 
  # https://docs.spring.io/spring-boot/docs/current/reference/html/spring-boot-features.html#boot-features-email
  forwardEmails: false
```

#### Attachment Size Limit Behavior

If an attachment or inline image exceeds `fakesmtp.maxAttachmentSize`:

- the part is skipped (not truncated),
- the email remains processable and visible in the UI/API,
- metadata is exposed via `processingStatus=SKIPPED_TOO_LARGE` and `processingMessage`,
- downloading a skipped attachment from `/api/emails/{mailId}/attachments/{attachmentId}` returns HTTP `413`.

#### Message Size Limit Behavior

If the SMTP payload exceeds `fakesmtp.maxMessageSize`, Fake SMTP rejects the message during `DATA` with `552 5.3.4 Message size exceeds fixed limit`. This limit is enforced even if the client omits `MAIL FROM SIZE=...`.

### Metrics and Management Endpoints

By default, the management server only exposes `/actuator/health` and `/actuator/info` on port `8081`. If you explicitly expose additional Actuator endpoints, they are no longer anonymously reachable by default.

Fake SMTP publishes the Micrometer metrics `messages.delivered` and `messages.blocked`. Sensitive email address tags (`from` and `recipient`) are disabled by default and must be explicitly enabled with:

```yaml
fakesmtp:
  metrics:
    include-address-tags: false
```

Equivalent environment variables:

```
FAKESMTP_MAX_MESSAGE_SIZE=10MB
FAKESMTP_MAX_ATTACHMENT_SIZE=10MB
FAKESMTP_TLS_PROTOCOLS=TLSv1.3,TLSv1.2
FAKESMTP_METRICS_INCLUDE_ADDRESS_TAGS=false
```

> [!IMPORTANT]
> The sensitive-data concern is the Actuator/Micrometer metrics exposure path, not `/api/meta-data`. The `/api/meta-data` endpoint only exposes application metadata such as `authenticationEnabled`.
    
### Authentication
Optionally authentication can be turned on. Configuring authentication does not mean the authentication is enforced. It
just allows you to test PLAIN and LOGIN SMTP Authentication against the server instance.

```yaml
fakesmtp:
  authentication:
    #Username of the client to be authenticated
    username: myuser
    #Password of the client to be authenticated
    password: mysecretpassword 
```

SMTP `AUTH` command lines are redacted in debug logs. For example, a line such as `AUTH PLAIN <base64>` is logged as `Client: AUTH PLAIN <redacted>`.


### TLS
Optionally TLS can be activated. To configure TLS support, a trust store needs to be provided 
containing the TLS certificate used by the FakeSMTP Server. By default, Fake SMTP only enables `TLSv1.3` and `TLSv1.2` for STARTTLS and leaves JVM default enabled cipher suites in effect.

```yaml
fakesmtp:
  # true when TLS is mandatory otherwise TLS is optional
  requireTLS: true
  #configuration of the truststore to enable support for TLS.
  tlsKeystore:
    location: /path/to/truststore.p12
    password: changeit
    type: PKCS12 # or JKS
```

If you need to support a narrower protocol set for compatibility testing, you can override the allowed STARTTLS protocols explicitly:

```yaml
fakesmtp:
  tls-protocols:
    - TLSv1.2
```

Equivalent environment variable:

```
FAKESMTP_TLS_PROTOCOLS=TLSv1.2
```
           
### Data Retention Settings

#### Emails

To keep memory resources under control, there is a parallel process that deletes the oldest emails considering the maximum number of emails to retain and the time span to periodically recheck this maximum number of emails, controlling also the initial time to wait to start this parallel process. The default values are:

- maxNumberEmails: 100
- fixedDelay: 300000 # 5 minutes
- initialDelay: 60000 # 1 minute

```yaml
fakesmtp:
  persistence:
    maxNumberEmails:
      emails:
        #max numbers of most recent emails to retain and not deleted by the parallel process
        maxNumberEmails: 10
        # configuration settings of the background process (timer) responsible to delete the oldest emails
        emailDataRetentionTimer:
          #each 5 minutes from 'initialDelay' (see below), the parallel process will check if the deletion is necessary
          fixedDelay: 300000
          #each 'initialDelay' (see above)  after 2 minutes from the start, the parallel process will start checking if the deletion is necessary
          initialDelay: 120000
```

## Web UI
The following snippet shows the pre-defined web application configuration

```yaml
#Port of the web interface
server:
  port: 8080
  shutdown: graceful
  servlet:
    # Async timeout for SSE connections - set to 5s for faster graceful shutdown
    async-timeout: 5000
    session:
      cookie:
        http-only: true
        same-site: Lax

fakesmtp:
  webapp:
    session:
      # Session timeout: 10 minutes default, maximum 24 hours (1440 minutes)
      session-timeout-minutes: ${FAKESMTP_WEBAPP_SESSION_TIMEOUT_MINUTES:10}

spring:
  # Shutdown timeout - reduces graceful shutdown time from 30s to 5s
  lifecycle:
    timeout-per-shutdown-phase: 5s

management:
  server:
    port: 8081
  endpoints:
    web:
      exposure:
        include: health,info
```

### Web UI Authentication

You can optionally enable authentication for the web interface and REST API using a server-side session. When authentication is enabled, users will need to log in to access the web interface and API endpoints.

To enable authentication, set the username and password in the application.yml file:

```yaml
fakesmtp:
  webapp:
    authentication:
      # Explicitly enable or disable Web UI authentication.
      # If omitted, the legacy behavior still applies for compatibility:
      # configuring both username and password implies enabled, but this is deprecated.
      enabled: true
      # Username for web UI and API authentication
      username: admin
      # Password for web UI and API authentication
      password: password
      # Maximum number of concurrent sessions per user (default: 1)
      # Set to -1 for unlimited sessions (development mode)
      concurrent-sessions: 1
```

You can also set these values using environment variables:

```
FAKESMTP_WEBAPP_AUTHENTICATION_ENABLED=true
FAKESMTP_WEBAPP_AUTH_USERNAME=admin
FAKESMTP_WEBAPP_AUTH_PASSWORD=password
FAKESMTP_WEBAPP_SESSION_TIMEOUT_MINUTES=10
FAKESMTP_WEBAPP_AUTHENTICATION_CONCURRENT_SESSIONS=1
FAKESMTP_WEBAPP_SSE_HEARTBEAT_INTERVAL_SECONDS=30
FAKESMTP_WEBAPP_SSE_EVENT_SEND_TIMEOUT_SECONDS=5
FAKESMTP_WEBAPP_RATE_LIMITING_ENABLED=true
FAKESMTP_WEBAPP_RATE_LIMITING_MAX_ATTEMPTS=5
FAKESMTP_WEBAPP_RATE_LIMITING_WINDOW_MINUTES=1
FAKESMTP_WEBAPP_RATE_LIMITING_CLEANUP_INTERVAL_MINUTES=1
FAKESMTP_WEBAPP_RATE_LIMITING_WHITELIST_LOCALHOST=true
FAKESMTP_WEBAPP_RATE_LIMITING_TRUST_PROXY_HEADERS=false
```

Current authentication semantics:

- `enabled=true` requires non-empty `username` and `password`
- `enabled=false` requires that `username` and `password` are not configured
- If `enabled` is omitted, the legacy compatibility path still applies: configuring both `username` and `password` implies enabled authentication, but startup logs a deprecation warning recommending the explicit flag
- Partial credential configuration is invalid and fails application startup

If authentication is disabled, the web interface and API endpoints remain accessible without login. When authentication is enabled, the UI presents a login form and email data is served exclusively from `/api/**`, guarded by a session cookie (HttpOnly).

When authentication is enabled:
- The web interface will show a custom login form
- API endpoints require an authenticated session (the UI shell is public only to render the login form; it does not include email data)
- A logout button will be available in the navigation bar
- The Web UI session expires after 10 minutes of inactivity and logs the user out by default; configure the timeout with `FAKESMTP_WEBAPP_SESSION_TIMEOUT_MINUTES` (maximum: 1440 minutes / 24 hours)
- By default, logging in from a different browser invalidates the existing session (single session per user). To allow multiple concurrent sessions from different browsers/devices with the same credentials, set `FAKESMTP_WEBAPP_AUTHENTICATION_CONCURRENT_SESSIONS` to a value greater than 1 (e.g., 5). Default is 1 (secure behavior for production). Set to -1 to allow unlimited concurrent sessions (useful for development)
- SSE heartbeat interval can be configured with `FAKESMTP_WEBAPP_SSE_HEARTBEAT_INTERVAL_SECONDS` (default: 30s) to prevent proxy timeouts
- The following endpoints are protected and require authentication:
  - API endpoints (`/api/**`) except for `/api/meta-data`
- The following endpoints remain public and do not require authentication:
  - `/api/meta-data` (provides application metadata including authentication status)
  - Web UI shell routes (`/`, `/emails/**`) for rendering the login form
  - Static resources (`/assets/**`, `/webjars/**`)
  - Swagger UI (`/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**`)
  - `/actuator/health` and `/actuator/info`
  - H2 console (`/h2-console/**`)

Additional Actuator endpoints may be exposed explicitly, but they are not anonymously reachable by default.

> [!NOTE]  
> The Web UI authentication is separate from the SMTP server authentication. The SMTP server authentication is configured under `fakesmtp.authentication` and is used for authenticating SMTP clients, while the Web UI authentication is configured under `fakesmtp.webapp.authentication` and is used for authenticating users accessing the web interface and API endpoints.

### Rate Limiting for Login

To protect against brute-force attacks on the login endpoint, Fake SMTP enables rate limiting by default. The rate limiter tracks login attempts per client IP address and blocks further attempts after exceeding the configured limit. It remains configurable, but it is only active when Web UI authentication is effectively enabled.

```yaml
fakesmtp:
  webapp:
    rate-limiting:
      # Enable/disable rate limiting (default: true)
      enabled: true
      # Maximum number of login attempts per time window (default: 5, max: 100)
      max-attempts: 5
      # Time window in minutes (default: 1, max: 60)
      window-minutes: 1
      # Cleanup interval for expired entries in minutes (default: 1)
      cleanup-interval-minutes: 1
      # Exempt localhost/loopback addresses from rate limiting (default: true)
      whitelist-localhost: true
      # Trust X-Forwarded-For/X-Real-IP headers (default: false)
      trust-proxy-headers: false
```

**Features:**
- Returns HTTP 429 (Too Many Requests) with `Retry-After` header when limit is exceeded
- Includes `X-RateLimit-Remaining` header in responses to show remaining attempts
- Is inert when Web UI authentication is effectively disabled, even if `rate-limiting.enabled=true`
- Supports `X-Forwarded-For` and `X-Real-IP` headers when `trust-proxy-headers` is enabled
- Localhost addresses (127.0.0.1, ::1, localhost) are whitelisted by default
- Thread-safe in-memory storage with automatic cleanup of expired entries

> [!IMPORTANT]
> Leave `trust-proxy-headers` disabled unless the application runs behind a trusted reverse proxy that sanitizes forwarding headers.

### API Clients & CSRF

When Web UI authentication is enabled, the server issues an HttpOnly session cookie after a successful login (`POST /api/auth/login` with form fields `username` and `password`). State-changing API requests (`POST`, `PUT`, `DELETE`) must include the `X-XSRF-TOKEN` header, which matches the `XSRF-TOKEN` cookie set by the server (use `GET /api/meta-data` to obtain it). Logging out is handled via `POST /api/auth/logout`.
    

## Real-Time Email Notifications

The Fake SMTP Server supports real-time email notifications using Server-Sent Events (SSE). This feature eliminates the need for polling and provides instant updates when new emails are received.

### How It Works

1. The web interface automatically establishes an SSE connection to the server
2. When a new email is received by the SMTP server, an event is sent to all connected clients
3. The web interface updates the email list in real-time without refreshing the page

### Benefits

- **Instant Updates**: See new emails as soon as they arrive
- **Reduced Server Load**: Eliminates the need for frequent polling
- **Improved User Experience**: No delay in seeing new emails
- **Efficient Resource Usage**: SSE is more efficient than WebSockets for one-way communication

### Authentication Considerations

When Web UI Authentication is enabled, the SSE connection is only established after successful login. This ensures that only authenticated users receive real-time notifications.

### SSE Heartbeat & Connection Health

The SSE implementation includes a server-sent heartbeat mechanism to ensure connection reliability:

- **Server-Sent Heartbeat**: The server sends a ping event every 30 seconds (configurable via `FAKESMTP_WEBAPP_SSE_HEARTBEAT_INTERVAL_SECONDS`)
- **Client Health Check**: The client monitors heartbeat reception and automatically reconnects if no ping is received within 60 seconds
- **Proxy Compatibility**: Heartbeats prevent connection timeouts when running behind proxies or load balancers
- **Connection Status UI**: A visual indicator in the navigation bar shows the current connection state (green: connected, yellow: reconnecting, red: disconnected)
- **Automatic Reconnection**: The client uses exponential backoff with jitter (up to 30s) when attempting to reconnect

### SSE Performance & Scalability

The SSE implementation uses Java 21 Virtual Threads for optimal performance:

- **Virtual Thread-based Delivery**: Email notifications are delivered concurrently to all clients using Java 21 Virtual Threads (Project Loom)
- **Non-blocking Architecture**: Slow or unresponsive clients cannot block event delivery to others
- **Per-Client Timeout**: Each client has a configurable timeout (default: 5s via `FAKESMTP_WEBAPP_SSE_EVENT_SEND_TIMEOUT_SECONDS`)
- **Massive Scalability**: Supports thousands of concurrent SSE connections with minimal resource overhead
- **Automatic Cleanup**: Failed connections are detected and removed automatically after each event broadcast

## REST API

Documentation of exposed services is available at:
    
    localhost:8080/swagger-ui.html

## Developpment Environment

This requires to have docker installed.
If you need to implement a new feature, you will probably need an correct JDK version setup in an environement.

> [!NOTE]
> **Build Requirements:**
> - Java 25 (JDK)
> - Node.js 24.13.1 (LTS)
> - npm 11.8.0

```sh
sh/dev
```

Then, in the dev container started by the command above, you can use various commands. 
The following commands should be the most common ones:
```bash
sh gradlew test
sh gradlew test --tests '*EmailRepositoryIntegration*' --info
sh gradlew build
```

Run UI & Backend tests
```bash
sh/test
```

Current baseline in this branch:
- Backend tests: 281 passed
- Frontend tests: 52 passed

Build UI & Backend
```bash
sh/build
```

Run app (UI & Backend)
```bash
sh/run
```

### Build & Push a new development docker image

To update/change the development image, update the `dev.Dockerfile`, dont forget to change the version in the `dev-image-tag` file and edit the registery if needed.
```bash
sh/push-dev-image
```
