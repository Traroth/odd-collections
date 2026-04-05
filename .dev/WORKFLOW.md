# Workflow — odd-collections

This document defines the standard workflow for implementing or modifying
a class in the library. The goal is to maintain architectural consistency,
code quality, and reliable behaviour when using AI-assisted development.
1
All steps should be followed in order unless explicitly instructed otherwise.

---

## Overview

The development workflow follows a **design‑first, TDD‑driven approach**:

1. Discussion
2. Stub class + JML specifications
3. Design review
4. Update `ARCHITECTURE.md`
5. Update `BACKLOG.md`
6. Tests (interface contract + black‑box, TDD)
7. Implementation
8. White‑box tests
9. Static analysis
10. JML conformance check (JML → Code)
11. JML completeness check (Code → JML)
12. Test coverage review
13. Documentation
14. Session wrap‑up

Each step has a specific purpose and should not be skipped.

---

## Model selection

Each step requires a different level of reasoning. Use the model indicated
below — do not default to the same model for every step.

| Step | Model | Reason |
|------|-------|--------|
| 1 — Discussion | **Opus** | Open-ended architectural reasoning; errors here propagate to all subsequent steps |
| 2 — Stub + JML | **Sonnet** | Precision required; structure already defined by step 1 |
| 3 — Design review | **Sonnet** | Validation against an existing design |
| 4 — Update ARCHITECTURE.md / INVARIANTS.md | **Sonnet** | Reformulating complex decisions into durable documentation |
| 5 — Update BACKLOG.md | **Haiku** | Mechanical, well-structured format |
| 6 — Tests (TDD) | **Sonnet** | Contractual precision required |
| 7 — Implementation | **Sonnet** | Code generation with fidelity to upstream decisions |
| 8 — White-box tests | **Sonnet** | Knowledge of internals required |
| 9 — Static analysis | **Sonnet** | Structured analysis against known criteria |
| 10 — JML conformance check | **Sonnet** | Requires reading code against formal specs |
| 11 — JML completeness check | **Sonnet** | Requires identifying missing formal specs |
| 12 — Test coverage review | **Sonnet** | Structured analysis against known criteria |
| 13 — Documentation | **Sonnet** / **Haiku** | Sonnet if the API or architecture changed; Haiku for minor updates |
| 14 — Session wrap-up | **Haiku** | Mechanical backlog and readme updates |

---

## 1. Discussion

**Use Plan mode** — no code modifications should be made during this step.

Before writing any code, discuss the design of the class or structure.

Define:

- The **core data structure**
- The **class invariants**
- The **expected complexity** of key operations
- The **interfaces implemented** (`List`, `Map`, `Set`, etc.)
- The **base class** (`AbstractList`, `AbstractMap`, etc.)
- Whether a **thread‑safe variant** is required
- Any **configurable strategies**

Do not move to the next step until the design is clearly understood and agreed upon.

---

## 2. Stub class + JML specifications

Create a skeleton of the class where:

- All public methods are declared with their full signature and Javadoc
- Method bodies contain only `throw new UnsupportedOperationException()`
- Class invariants are expressed as JML `@invariant` annotations
- Pre‑ and post‑conditions of key mutation methods are expressed as JML
  `@requires` / `@ensures` contracts

The goal of this step is to make the **contract explicit and reviewable**
before any logic is written. The stub is the artefact that the design review
and the black‑box tests will be written against.

---

## 3. Design review

**Use Plan mode** — no code modifications should be made during this step.

Run the `design-review` skill.

The goal is to validate:

- the suitability of the chosen data structure
- the clarity of invariants
- the feasibility of mutation operations
- the complexity guarantees
- possible alternative designs

If significant risks are identified, resolve them before proceeding.

---

## 4. Update ARCHITECTURE.md and INVARIANTS.md

### ARCHITECTURE.md

Document the design decisions.

Add a section describing:

- the chosen data structure
- the invariants
- key implementation choices
- alternatives considered and rejected

This document acts as the **long‑term architectural memory** of the project.

### INVARIANTS.md

Add a section for the new class documenting:

- null handling policy
- ordering and uniqueness invariants (if applicable)
- size consistency invariants
- internal structural invariants (node links, augmentation, etc.)

Express invariants using JML `@invariant` notation where possible, so
they can serve as a reference for both implementation and formal verification.

---

## 5. Update BACKLOG.md

Add the class and its tasks to the backlog.

Typical entries include:

- class implementation
- tests
- documentation
- known limitations
- future improvements

Each task must be represented as a checklist item. Move completed tasks to
the `## Done` section immediately when they are finished — do not leave
completed items checked in the active sections.

---

## 6. Tests — interface contract + black‑box (TDD)

Write the interface contract tests and the black‑box tests **before**
implementing the class. Both can be written against the stub produced in
step 2, without reading any implementation.

### Interface contract tests

`FooTest` (uses `MockFoo`)

These tests verify the **interface contract in isolation**.

They must cover:

- that the methods exist and behave as documented
- that enums are correct
- that exceptions are thrown at the right places

They must be writable without reading any concrete implementation.

### Black‑box tests

`ClassNameBlackBoxTest`

These tests verify the **public contract only**.

They must cover:

- normal behaviour
- boundary conditions
- error scenarios
- concurrency guarantees (for synchronized implementations)

They should be writable **without reading the implementation**.

At this point, all tests are expected to fail — the stub methods all throw
`UnsupportedOperationException`. This is intentional: the tests define the
target behaviour that the implementation must satisfy.

---

## 7. Implementation

Replace each `UnsupportedOperationException` stub with a real implementation,
following the coding standards defined in `.dev/standards/JAVA_STANDARDS.md`.

The goal is to make all black‑box tests pass. Once they do, run the **full
test suite** (`mvn test`) to verify that no existing class was broken by the
changes.

Key principles:

- Preserve class invariants
- Prefer clarity over cleverness
- Keep methods reasonably small
- Avoid unnecessary abstraction

Pay special attention to **mutation methods**, which are the most common
source of structural bugs:

- `put`
- `remove`
- `add`
- `set`
- `clear`
- `rehash`
- `resize`

---

## 8. White‑box tests

Write the white‑box tests **after** the implementation is complete.
These tests require knowledge of the internal structure and cannot be written
before the implementation exists.

`ClassNameWhiteBoxTest`

These tests target **implementation risks**, such as:

- resize boundaries
- collision scenarios
- structural invariants
- concurrent modification cases
- lock ordering

Each test must have a comment explaining **why the scenario is risky**.

White‑box tests must not duplicate black‑box tests — they target only what
cannot be observed through the public API.

---

## 9. Static analysis

Run the `static-analysis` skill.

The analysis verifies:

- logic correctness
- contract compliance
- invariant preservation
- complexity guarantees
- mutation safety
- iterator correctness
- null handling
- coding standards

Resolve all **Critical issues** before continuing. If a critical issue
requires changing the implementation, return to step 7, fix the code, and
re‑run the full test suite before resuming at step 9.

---

## 10. JML conformance check (JML → Code)

Run the `jml-conformance-check` skill.

Verify that the implementation respects its JML specifications:

- class invariants are established by constructors and preserved by all
  mutation methods
- pre‑conditions are validated or documented as caller responsibility
- post‑conditions are satisfied on all return paths
- purity annotations are correct
- overriding methods use the `also` keyword

If a violation is found, return to step 7, fix the code, and re‑run the
full test suite before resuming.

---

## 11. JML completeness check (Code → JML)

Run the `jml-completeness-check` skill.

Verify that the JML specifications are complete and up to date:

- every public method has JML contracts
- all invariants from `INVARIANTS.md` are annotated in the code
- no implicit invariant is left unformalized
- synchronized variants have the same contracts as their unsynchronized
  counterparts

If gaps are found, add the missing annotations, then re‑run step 10 to
verify the new annotations are correct.

---

## 12. Test coverage review

Run the `test-coverage-review` skill.

Verify that:

- all public methods are tested
- mutation sequences are covered
- edge cases are tested
- internal edge cases are exercised

Add missing tests if necessary. If gaps require new white‑box tests, return
to step 8 and add them, then re‑run the full test suite.

---

## 13. Documentation

Update project documentation when public behaviour changes.

This may include:

- `README.md`
- `ARCHITECTURE.md`
- `INVARIANTS.md`

Run the `update-readme` skill when a new class or public API change is
introduced.

Run the `api-consistency-check` skill when new public methods are added.

Run the `architecture-drift-check` skill after significant refactoring or
before a release.

Ensure examples compile against the real API.

---

## 14. Session wrap‑up

At the end of a session, run:

- `update-backlog`
- `update-readme`

This ensures:

- the backlog reflects the current state
- documentation stays up to date
- unfinished work is clearly recorded

This step should never be skipped.

---

## Guiding principles

### Design before code

Never implement a structure before its invariants and behaviour are defined.

### Fix known issues before adding new code

Never advance on new code while known bugs or issues remain unresolved.
Record all known issues in `BACKLOG.md` and resolve them first.

### Invariants must always hold

All mutation methods must preserve class invariants.
If invariants are temporarily broken during a method, they must be restored
before returning.

### Prefer clarity over cleverness

Readable and maintainable code is preferred over overly clever implementations.

### Small iterations

Prefer incremental development:

1. scaffold the structure
2. implement minimal behaviour
3. validate invariants
4. extend functionality

Avoid generating large complex classes in a single step.

---

## Skills used in the workflow

| Skill                      | Role                                          |
|----------------------------|-----------------------------------------------|
| `design-review`            | Validate architecture before coding           |
| `jml-design`               | Write JML contracts during step 2             |
| `static-analysis`          | Detect structural and contract issues         |
| `test-coverage-review`     | Verify test completeness                      |
| `jml-test-generation`      | Derive tests from JML specs                   |
| `api-consistency-check`    | Verify API consistency across the library     |
| `architecture-drift-check` | Verify implementation matches design          |
| `update-backlog`           | Maintain project task tracking                |
| `update-readme`            | Keep documentation up to date                 |