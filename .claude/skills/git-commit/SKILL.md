---
name: git-commit
description: >
  Safe git commit workflow for this project. Runs relevant tests first, then
  proposes which files to stage and a conventional commit message, and waits
  for user confirmation before executing the commit. Never includes
  Co-Authored-By attribution. Use this skill whenever the user asks to commit,
  says "commit my changes", "save to git", "commit and push", or asks to wrap
  up / finish an iteration. Also trigger when the user types /commit or asks
  to commit after completing a feature.
---

# Safe Git Commit Workflow

This skill gates every commit behind a test run and user confirmation. The goal is
to avoid broken commits and ensure the message accurately reflects the change.

## Step 1 — Detect what changed

Run `git status` and `git diff HEAD --name-only` to see which files are modified,
staged, or untracked. Group them by area:

- **src/** → backend tests
- **webapp/** → frontend tests

If nothing has changed, tell the user and stop.

## Step 2 — Run tests for affected areas

Run only the tests relevant to what changed. This keeps the feedback loop fast.

| Changed area | Command                                                        |
| ------------ | -------------------------------------------------------------- |
| `src/`       | `mvn test`                                                     |
| `webapp/`    | `cd webapp && npm run build && npm run lint && npm test --run` |

Run each applicable command. If any test run fails, **stop immediately** and show
the failure output. Do not proceed to staging or committing. Explain which area
failed and what the user should fix.

If all tests pass, summarise the result briefly (e.g. "✓ 111 tests passed in api
module") and continue.

## Step 3 — Propose files to stage

List the files you intend to stage. Use `git diff HEAD --name-only` and
`git ls-files --others --exclude-standard` for untracked files.

Exclude files that should not be committed:

- `.claude/settings.local.json` (personal local settings)
- `.env*` files
- `target/` or `node_modules/` contents (should be gitignored, but flag if not)

Present the proposed file list to the user clearly. If there are files you are
unsure about, flag them and ask.

## Step 4 — Generate a commit message

Analyse the diff with `git diff HEAD` and draft a commit message following the
project's convention:

```
<type>: <short description>

<optional body — bullet points describing key changes>
```

Types: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `perf`, `ci`

Keep the subject line under 72 characters. Use the body to capture the _why_ and
_what_ when the change is non-trivial. Do not include `Co-Authored-By` lines.

## Step 5 — Ask for confirmation

Present a summary to the user before doing anything irreversible:

```
Files to stage:
  <list of files>

Commit message:
  <type>: <subject>

  <body if any>

Proceed? (yes / edit message / cancel)
```

Wait for the user's response:

- **yes** (or any affirmative) → proceed to Step 6
- **edit message** / user provides new message → use their version, then proceed
- **cancel** → stop, do nothing

## Step 6 — Stage and commit

Stage only the files from Step 3 (add them individually, not with `git add -A`
or `git add .`, to avoid accidentally staging sensitive files).

Commit with the confirmed message using a heredoc to avoid shell escaping issues:

```bash
git commit -m "$(cat <<'EOF'
<type>: <subject>

<body>
EOF
)"
```

Do **not** append `Co-Authored-By` or any attribution footer.

After the commit, run `git log --oneline -3` and show the user the result so they
can confirm everything looks correct.

## Notes

- If the user asks to also push after committing, do so with `git push`.
- If the user is on a new branch with no upstream, use `git push -u origin <branch>`.
- If the user already staged files themselves before invoking the skill, respect
  that staging and skip the "propose files" step — just show what's staged and
  confirm the message.
