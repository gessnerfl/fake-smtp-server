# Repository Guidelines

## Project Overview
- Fake SMTP Server is a Spring Boot backend with a bundled React/Vite frontend.
- Backend sources live in `src/main/java/de/gessnerfl/fakesmtp`.
- Frontend sources live in `webapp/src`.
- The packaged application serves both backend APIs and frontend assets from one JAR.
- SMTP traffic is accepted on `8025`, the web UI runs on `8080`, and Actuator runs on `8081`.

## Project Structure & Module Organization
- Backend (Spring Boot): `src/main/java/de/gessnerfl/fakesmtp`
- Backend config/resources: `src/main/resources` (Flyway migrations in `src/main/resources/db/migration`)
- Backend tests: `src/test/java` and `src/test/resources`
- Frontend (React + Vite + TypeScript): `webapp/src`, static files in `webapp/public`
- Build and CI metadata: `build.gradle`, `gradle/libs.versions.toml`, `.github/workflows/*.yml`
- AI-assisted development artifacts: `agent/plans/`, `agent/tasks/`, `agent/process/`
- Working notes and review artifacts: `agent/`

## Codex Development Setup
- This repository includes a project-scoped Codex configuration in `.codex/config.toml` and `.codex/rules/dev.rules`.
- The goal is asynchronous agent work with minimal approval prompts for normal development tasks while keeping critical git operations guarded.
- Prefer local Gradle and npm commands over the Docker wrapper scripts in `sh/` for agent work. The `sh/*` scripts start containers and are intended for manual parity checks, not default Codex execution.
- Safe default commands for agent work are:
- `./gradlew test`
- `./gradlew build`
- `./gradlew compileJava`
- `./gradlew bootRun`
- `./gradlew jacocoTestReport`
- `cd webapp && npm test`
- `cd webapp && npm run lint`
- `cd webapp && npm run dev`
- Agent guidance should live in `AGENTS.md`, `webapp/AGENTS.md`, and `agent/` rather than in ad-hoc scratch files.

## AI Artifact Flow
- There is a strict separation between planning, execution tasks, and scratch material.
- Implementation plans must be stored in `agent/plans/` using `YYYY-MM-DD-<topic>-implementation-plan.md`.
- Developer task breakdowns must be stored in `agent/tasks/` using `YYYY-MM-DD-<topic>-tasks.md`.
- The process definition for AI-assisted development lives in `agent/process/ai-development-flow.md`.
- `agent/` is the canonical location for AI-assisted development artifacts in this repository.
- `docs/` is reserved for product and project documentation, not AI execution planning.
- Do not create new top-level planning files in the repository root.
- If a task is small, the plan may be short, but the location still stays `agent/plans/`.
- If subagents are used, the lead agent owns the plan and task files; subagents should primarily touch code and only write into `agent/` when explicitly instructed.

## Required Execution Flow
1. Capture scope from the user request or linked issue.
2. Write or update the implementation plan in `agent/plans/`.
3. Derive an executable task list in `agent/tasks/` from the approved plan when task slicing is useful.
4. Execute code changes in the worktree or subagent worktrees.
5. Store review notes, findings, screenshots, and MR text in `agent/`.
6. Reflect durable user-facing behavior changes in `README.md` or other permanent docs.
7. Verify with the relevant build, test, or lint commands before claiming completion.

## Build, Test, and Development Commands
- `./gradlew bootRun`  
  Starts the app locally (SMTP on `8025`, UI on `8080`, management on `8081`).
- `./gradlew test`  
  Runs JUnit tests and frontend Jest tests via the Gradle pipeline.
- `./gradlew build`  
  Full build including frontend bundle integration and JAR packaging.
- `./gradlew jacocoTestReport`  
  Generates JaCoCo XML report (used by Sonar/quality checks).
- `cd webapp && npm run dev`  
  Runs frontend dev server only.
- `cd webapp && npm run lint`  
  Runs ESLint for TS/TSX sources.

## Coding Style & Naming Conventions
- Java: 4-space indentation, `PascalCase` classes, `camelCase` methods/fields, package prefix `de.gessnerfl.fakesmtp`.
- TypeScript/React: follow existing patterns (`kebab-case` file names like `email-list-page.tsx`, `PascalCase` component names).
- Frontend formatting is governed by `webapp/.prettierrc` (2 spaces, single quotes, semicolons, max line length 120).
- Keep changes focused; avoid unrelated refactors in the same commit.

## Testing Guidelines
- Backend: JUnit 5 + Spring Boot test support (`@...IntegrationTest` for integration scope).
- Frontend: Jest + React Testing Library (`*.spec.ts` / `*.spec.tsx`).
- Naming: unit tests end with `Test`; integration tests end with `IntegrationTest`.
- Before opening a PR, run at least: `./gradlew test` (or `./gradlew build` for full verification).

## Commit & Pull Request Guidelines
- Commit messages are short, imperative, and specific (example: `Add session-based Web UI authentication (#815)`).
- Reference issue numbers when applicable: `(#123)`.
- PRs should include a clear problem statement and implementation scope.
- Link related issues and dependent PRs.
- Add test evidence (commands run, for example `./gradlew test`).
- Highlight configuration or environment variable changes (for example `FAKESMTP_*` variables).
- Include screenshots/GIFs for user-visible frontend changes.

## Git Policy For Agents
- `git commit` is only allowed when the user explicitly asks for a commit.
- `git push` is forbidden for agent-driven development in this repository.
- Prefer `git worktree add` for isolated subagent work instead of mutating the main worktree.
- Avoid destructive git commands such as `git reset`, `git clean`, `git rebase`, and `git checkout --` unless the user explicitly requests them.
- Keep staging focused if a user explicitly requests a commit; do not stage unrelated files.

## Subagent Coordination
- Prefer disjoint write sets across backend, frontend, and workflow/docs changes.
- Treat these as single-owner integration files unless the task explicitly says otherwise:
- `README.md`
- `build.gradle`
- `gradle/libs.versions.toml`
- `.github/workflows/**`
- `.codex/**`
- `agent/plans/**`
- `agent/tasks/**`
- `agent/process/**`
- Use `agent/plans/` for implementation plans, `agent/tasks/` for task slicing, `agent/process/` for the process definition, and `agent/` for implementation notes or review artifacts.

## Security & Configuration Tips
- Never commit real credentials or tokens.
- Prefer environment variables for runtime config (`FAKESMTP_WEBAPP_AUTH_USERNAME`, `FAKESMTP_WEBAPP_AUTH_PASSWORD`, etc.).
- Document new or changed env vars in `README.md` within the same PR.
