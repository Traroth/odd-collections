---
name: update-backlog
description: >
  Update BACKLOG.md at the end of a working session in odd-collections.
  Trigger this skill when the user says "update the backlog", "end of session",
  "wrap up", or "what did we do". Also run it automatically after completing
  any backlog task, even if the user doesn't ask — it should never be skipped.
---

# Update Backlog — odd-collections

## When to run

- At the end of every working session
- After completing, partially completing, or abandoning a backlog task
- After discovering a new issue or design decision worth tracking

---

## Procedure

### 1. Identify changes from the session

Review the conversation and extract:

- Tasks that were **fully completed** → move to `### Done` under the correct class,
  using `[x]`
- Tasks that were **partially completed** → keep under the class with `[ ]`, add a
  note like `(in progress — [what remains])`
- Tasks that were **abandoned or put on hold** → keep with `[ ]`, update or add
  `— on hold` with a short reason
- **New tasks or issues discovered** → add under the correct class with `[ ]`
- **New design decisions** → these belong in `ARCHITECTURE.md`, not the backlog;
  flag them separately for the user

### 2. Determine the correct section

Each task belongs to one of:

- `## Project-wide` — affects the whole library or no specific class
- `## SymmetricMap` — specific to `SymmetricMap`
- `## ChunkyList` — specific to `ChunkyList` or its implementations
- A new `## ClassName` section if a new class was introduced this session

### 3. Write the update

Follow the existing format exactly:

```markdown
- [ ] Task description — optional note
- [x] Completed task description
```

- One task per line
- No sub-bullets
- Short, imperative phrasing ("Create", "Fix", "Implement", "Change")
- If a task has a dependency or constraint, append it after ` — `
  (e.g. `— on hold`, `— blocked by setValue()`)

### 4. Present the diff to the user

Before writing, show the user:
- What you are marking as done
- What you are adding
- What you are modifying

Ask for confirmation if anything is ambiguous.

---

## Format reminder

The full structure of `BACKLOG.md` is:

```markdown
# Backlog — odd-collections

Pending tasks and known issues, grouped by class. Update this file at the end
of each session.

---

## Project-wide
...

## SymmetricMap
...

## ChunkyList
...

---

## Done

### ClassName
- [x] ...
```

Preserve all existing content. Only add, tick, or annotate — never delete.