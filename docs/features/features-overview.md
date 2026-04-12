# Feature Documentation Overview

This file is the entry point for the durable feature documentation of Fake SMTP Server.

All feature-oriented documentation now lives under `docs/features/` instead of the repository root.

## Document Map

- [Frontend features](./frontend-features.md)
  Covers the React web UI, including routing, login, session UX, inbox behavior, email details, attachment handling, and realtime indicators.
- [Backend features](./backend-features.md)
  Covers SMTP behavior, message processing, REST APIs, downloads, retention, persistence, and forwarding.
- [Operations and security features](./operations-and-security-features.md)
  Covers authentication, CSRF, session management, rate limiting, TLS, Actuator, Swagger UI, H2 console, metrics, and operator-facing runtime behavior.
- [Coverage matrix](./coverage-matrix.md)
  Tracks which source files and tests back each documented feature area.

## Product Summary

Fake SMTP Server is a Spring Boot application with a bundled React/Vite frontend. It accepts SMTP traffic, stores received emails in an in-memory database, and exposes them through a browser UI and REST APIs.

The application is designed for development and testing scenarios where a team needs to inspect outbound email behavior without relaying messages to a real mail system.

## Major Feature Areas

### Web UI

- Browser-based inbox at `/`
- Direct email detail route at `/emails/:id`
- Optional Web UI authentication with session-based login
- Realtime inbox refresh via Server-Sent Events
- Inline HTML rendering, plain-text rendering, raw-source viewing, and attachment downloads

### SMTP And Message Capture

- Built-in SMTP listener in the same application process
- Optional STARTTLS and SMTP AUTH support
- Message-size and attachment-size controls
- MIME parsing for plain, HTML, multipart, inline-image, and attachment scenarios
- Filter and blocklist handling

### API And Download Surface

- Paged email listing and single-email retrieval
- Email deletion endpoints
- Attachment and inline-image download endpoints
- Search API for backend-side filtering, sorting, and paging
- SSE endpoint for email-received and heartbeat events

### Operations

- Separate management port for Actuator
- Swagger UI for API discovery
- Optional H2 console in the develop profile
- Rate limiting for login attempts
- Metrics for delivered and blocked messages

## Audience Guide

- Read [Frontend features](./frontend-features.md) if you care about the browser UX and user-visible flows.
- Read [Backend features](./backend-features.md) if you care about SMTP, storage, parsing, APIs, or integration behavior.
- Read [Operations and security features](./operations-and-security-features.md) if you run, secure, or monitor the application.
- Use [Coverage matrix](./coverage-matrix.md) if you want to audit documentation completeness against code and tests.

## Current Scope Boundary

- The browser UI currently focuses on browsing, inspecting, and deleting captured emails.
- The backend exposes more capability than the current UI uses, especially the search API and some operator-facing endpoints.
- The documentation is intentionally split by reader intent so operational concerns do not get buried inside UI or protocol details.
