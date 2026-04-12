# Webapp Guidelines

## Scope
- This file applies to everything under `webapp/`.

## Frontend Stack
- React 19 + TypeScript + Vite
- Material UI 7 for component primitives
- Redux Toolkit and RTK Query for state and API access
- React Router 7 for routing

## Structure
- Route-level pages live in `src/pages/`.
- Reusable UI lives in `src/components/`.
- API models live in `src/models/`.
- Backend communication belongs in `src/store/rest-api.ts`.
- Base-path handling is centralized in `src/base-path.ts`.

## Frontend Commands
- `npm test`
- `npm run lint`
- `npm run build`
- `npm run dev`

## Frontend Testing
- Use Jest and React Testing Library.
- Test files should use `*.spec.ts` or `*.spec.tsx`.
- For frontend-only changes, run at least `npm test` and `npm run lint`.

## Frontend Editing Notes
- Preserve the existing Material UI and RTK Query patterns unless the task explicitly changes architecture.
- Do not edit generated output under `dist/`.
- Keep routing and shell-level concerns in `src/pages/` and avoid spreading them into leaf components.
