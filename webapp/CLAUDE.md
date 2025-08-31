# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is the web UI component of the Fake SMTP Server project - a React-based frontend for viewing and managing emails captured by the fake SMTP server. The UI is built with TypeScript, React 19, Material-UI, and Redux Toolkit.

## Common Commands

### Development
- `npm run dev` - Start the Vite development server with hot reload
- `npm run build` - Build for production (runs TypeScript compilation + Vite build)
- `npm run preview` - Preview the production build locally

### Code Quality
- `npm run lint` - Run ESLint on TypeScript/TSX files in src directory
- `npm test` - Run Jest unit tests

### Development Server Configuration
The development server (Vite) proxies API requests to `http://localhost:8080` where the backend Spring Boot application runs.

## Architecture

### State Management
- Uses Redux Toolkit with RTK Query for API state management
- Store configuration in `src/store/store.ts`
- REST API slice in `src/store/rest-api.ts` handles all backend communication

### Routing & Layout
- React Router v7 for client-side routing
- Main shell layout with navigation in `src/pages/shell.tsx`
- Two main routes:
  - `/` - Email list page
  - `/emails/:id` - Individual email detail page

### Component Structure
- `src/components/` - Reusable UI components, primarily email-related
- `src/pages/` - Route-level page components
- `src/models/` - TypeScript type definitions for API data models

### Material-UI Theme
Custom theme configured in `src/pages/shell.tsx` with:
- Light blue primary colors
- Yellow secondary colors
- Extended breakpoint for xl screens (1750px)

### API Integration
RTK Query endpoints for:
- `getEmails` - Paginated email list
- `getEmail` - Individual email details
- `deleteEmail` - Delete single email
- `deleteAllEmails` - Delete all emails
- `getMetaData` - Application metadata

### Testing
- Jest with jsdom environment (`jest-fixed-jsdom`)
- React Testing Library for component testing
- Test files use `.spec.tsx` extension
- Mock service worker (MSW) available for API mocking

## Development Notes

The webapp is designed to work with the parent Fake SMTP Server Spring Boot application. The backend provides REST APIs at `/api/*` endpoints that this frontend consumes. The application supports dynamic base path configuration through `src/base-path.ts` for flexible deployment scenarios.