# AI Development Flow

This document defines the required artifact flow for AI-assisted development in this repository.

## Purpose

The repository needs a single, predictable place for each kind of implementation artifact so that:

- humans know where to look first,
- lead agents and subagents do not invent competing structures,
- planning artifacts remain durable,
- temporary scratch material does not become accidental source of truth.
- `docs/` remains reserved for durable documentation.

## Canonical Locations

### 1. Implementation plan

- Location: `agent/plans/`
- Naming: `YYYY-MM-DD-<topic>-implementation-plan.md`
- Owner: lead developer or lead agent
- Purpose: approved implementation approach, scope, architecture, file ownership, verification strategy

Example:

- `agent/plans/2026-04-12-session-auth-implementation-plan.md`

### 2. Developer task list

- Location: `agent/tasks/`
- Naming: `YYYY-MM-DD-<topic>-tasks.md`
- Owner: lead developer or lead agent
- Purpose: executable task slicing derived from the implementation plan

The task file should reference the plan at the top and break work into concrete task groups with clear owners.

Example:

- `agent/tasks/2026-04-12-session-auth-tasks.md`

### 3. Scratch and supporting artifacts

- Location: `agent/`
- Naming: free-form but topic-oriented
- Owner: any contributor when needed
- Purpose: temporary notes, review findings, screenshots, merge-request descriptions, investigation notes

Files in `agent/` outside `agent/plans/`, `agent/tasks/`, and `agent/process/` are supporting material.

## Directory Roles

- `agent/plans/` contains implementation plans created during planning mode.
- `agent/tasks/` contains developer task lists, optional execution backlogs, and any derived task slicing.
- `agent/process/` contains process definitions such as this file.
- `docs/` contains durable project and product documentation. It is not the default home for AI execution artifacts.

## Required Flow

1. Intake

- Start from the user request, issue, or bug report.
- Clarify the target behavior and constraints.

2. Planning

- Create or update the implementation plan in `agent/plans/`.
- Keep it durable and readable for a later human reviewer.

3. Task slicing

- Create or update the execution task list in `agent/tasks/` when the work benefits from explicit slicing.
- Every task should map back to the implementation plan.
- If subagents are used, assign disjoint ownership here.
- In a later implementation session, the agent may decide whether additional task artifacts are useful. If yes, they also belong in `agent/tasks/`.

4. Execution

- Implement the code changes in the relevant source directories.
- Subagents should primarily work on code, tests, and their owned files.

5. Support material

- Put review findings, screenshots, experiment notes, and MR drafts into `agent/`.
- Do not place the master plan or canonical task list outside `agent/plans/` and `agent/tasks/`.

6. Verification

- Run the relevant test, build, and lint commands.
- Record durable documentation changes in `README.md` or other permanent docs when behavior changed.

## Rules

- Do not create new top-level plan or task files in the repository root.
- Do not place AI execution plans or task lists in `docs/`.
- Prefer one plan file and one task file per topic.
- Update the existing plan/task pair instead of creating duplicates when continuing the same workstream.
- If the work is tiny, the documents may be short, but the location rules still apply.
