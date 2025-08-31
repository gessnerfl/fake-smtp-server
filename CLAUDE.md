# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Fake SMTP Server is a Java Spring Boot application with a React frontend that provides a development SMTP server for testing email functionality. It captures emails in-memory and displays them through a web interface instead of actually sending them.

## Architecture

### Backend (Spring Boot)
- **Main Package**: `de.gessnerfl.fakesmtp`
- **SMTP Server**: Custom SMTP implementation in `smtp/` package with command handlers, authentication, and message processing
- **Data Layer**: JPA entities and repositories for email storage with H2 in-memory database
- **REST API**: Controllers in `controller/` package expose email management endpoints
- **Configuration**: Properties-based configuration in `config/` package for SMTP settings, authentication, TLS

### Frontend (React)
- **Location**: `webapp/` directory
- **Stack**: React 19, TypeScript, Material-UI, Redux Toolkit, Vite
- **Pages**: Email list and detail views with search/filtering capabilities
- **State**: RTK Query for API communication with backend REST endpoints

### Integration
- Frontend build artifacts are copied into Spring Boot's static resources during build
- Single deployable JAR contains both backend and frontend
- Development mode runs backend and frontend separately with API proxying

## Common Commands

### Full Application Development
```bash
# Run in development container (requires Docker)
sh/dev

# Inside container - run backend only
./gradlew bootRun

# Run both backend and frontend
sh/run

# Run all tests (backend + frontend)
sh/test

# Build complete application
sh/build
./gradlew build
```

### Backend Development (Java/Gradle)
```bash
# Run Spring Boot application
./gradlew bootRun

# Run backend tests only
./gradlew test

# Run specific test class
./gradlew test --tests '*EmailRepositoryIntegration*' --info

# Build JAR
./gradlew build

# Clean build
./gradlew clean
```

### Frontend Development (React/npm)
```bash
# From webapp/ directory:
npm run dev          # Vite dev server (proxies API to :8080)
npm run build        # Production build
npm test             # Jest unit tests
npm run lint         # ESLint
npm run preview      # Preview production build
```

## Key Configuration

### SMTP Server Settings
- **Port**: 8025 (configurable via `fakesmtp.port`)
- **Web UI**: 8080 (configurable via `server.port`)
- **Management**: 8081 (configurable via `management.server.port`)

### Development URLs
- **Backend**: http://localhost:8080
- **Frontend Dev**: http://localhost:3000 (proxies to backend)
- **API Docs**: http://localhost:8080/swagger-ui.html

## Application Structure

### Core Email Processing Flow
1. **SMTP Commands** (`smtp/command/`) handle SMTP protocol communication
2. **Message Handler** (`smtp/MessageHandler`) processes incoming email data
3. **Email Factory** creates `Email` entities from raw SMTP data
4. **Email Repository** persists emails to H2 database
5. **REST Controllers** expose email data via `/api/emails` endpoints
6. **React Frontend** displays emails with search/filter capabilities

### Key Components
- **Email Entity** (`model/Email.java`): Core email data model with attachments, content, headers
- **SMTP Server** (`smtp/server/SmtpServer.java`): Custom SMTP server implementation
- **Email REST Controller** (`controller/EmailRestController.java`): REST API for email CRUD operations
- **React Email Components** (`webapp/src/components/`): UI components for email display

### Configuration Properties
Primary configuration in `FakeSmtpConfigurationProperties.java`:
- SMTP port and binding
- Authentication settings
- TLS configuration  
- Message size limits
- Email retention policies
- Blocked recipient addresses

### Testing
- **Backend**: JUnit tests for services, repositories, SMTP functionality
- **Frontend**: Jest + React Testing Library for component testing
- **Integration**: Full application tests with embedded SMTP server

## Development Notes

The application uses Gradle's Node plugin to integrate frontend build into backend build process. The `copyStaticAssets` and `copyTemplateAssets` tasks handle moving React build output into Spring Boot's resources directory.

For development, run backend with `./gradlew bootRun` and frontend with `npm run dev` from webapp directory. Frontend dev server proxies API requests to backend.

The SMTP server implementation is custom-built (not using standard libraries) to provide full control over email handling and storage behavior.