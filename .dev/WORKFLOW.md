# Workflow — odd-collections

This document defines the required order of operations for any creation or
significant modification of a class in this project. It applies to all working
sessions, regardless of the scope of the change.

---

## Order of operations

### 1. `.dev/design/ARCHITECTURE.md`

Update first. Record any new design decisions, alternatives considered, and
reasons for rejection. If the change modifies an existing decision, update the
relevant section rather than appending a contradictory one.

Do not write any code before this step is complete and, if needed, confirmed
with the user.

### 2. `.dev/backlog/BACKLOG.md`

Update second. Tick completed tasks, add new tasks discovered during the
design phase, and note any items put on hold. Keep the format consistent with
the existing entries.

### 3. Code — `src/main/java/`

Write or modify the production code. Follow `.dev/standards/JAVA_STANDARDS.md`
throughout. After completing this step, run the `static-analysis` skill before
moving on.

### 4. Tests — `src/test/java/`

Write or update `BlackBoxTest` and `WhiteBoxTest`. Black-box tests first, then
white-box tests. Do not write tests before the production code is stable.

### 5. `README.md`

Update last. Run the `update-readme` skill. Only update the sections affected
by the change — do not rewrite unrelated sections.

---

## Notes

- Steps 1 and 2 are never optional, even for small changes.
- Step 5 is skipped if the change has no impact on the public API, the
  documented behaviour, or the roadmap.
- The order is strict: do not jump ahead to code before the design is
  documented, and do not update the README before the tests are written.