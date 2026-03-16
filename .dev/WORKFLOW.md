
# Workflow — odd-collections

This document defines the standard workflow for implementing or modifying
a class in the library. The goal is to maintain architectural consistency,
code quality, and reliable behaviour when using AI-assisted development.

All steps should be followed in order unless explicitly instructed otherwise.

---

## Overview

The development workflow follows a **design‑first and validation‑driven approach**:

1. Design
2. Design review
3. Update `ARCHITECTURE.md`
4. Update `BACKLOG.md`
5. Implementation
6. Static analysis
7. Refactoring
8. Tests
9. Test coverage review
10. Documentation
11. Session wrap‑up

Each step has a specific purpose and should not be skipped.

---

## 1. Design

Before writing any code, clarify the design of the class or structure.

Define:

- The **core data structure**
- The **class invariants**
- The **expected complexity** of key operations
- The **interfaces implemented** (`List`, `Map`, `Set`, etc.)
- The **base class** (`AbstractList`, `AbstractMap`, etc.)
- Whether a **thread‑safe variant** is required
- Any **configurable strategies**

If structural constraints exist, document them clearly.
Prefer documenting invariants using **JML `@invariant` annotations**.

Do not start coding until the design is clearly understood.

---

## 2. Design review

Run the `design-review` skill.

The goal is to validate:

- the suitability of the chosen data structure
- the clarity of invariants
- the feasibility of mutation operations
- the complexity guarantees
- possible alternative designs

If significant risks are identified, resolve them before proceeding.

---

## 3. Update ARCHITECTURE.md

Document the design decisions.

Add a section describing:

- the chosen data structure
- the invariants
- key implementation choices
- alternatives considered and rejected

This document acts as the **long‑term architectural memory** of the project.

---

## 4. Update BACKLOG.md

Add the class and its tasks to the backlog.

Typical entries include:

- class implementation
- tests
- documentation
- known limitations
- future improvements

Each task must be represented as a checklist item.

---

## 5. Implementation

Implement the class following the coding standards defined in:

`.dev/standards/JAVA_STANDARDS.md`

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

## 6. Static analysis

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

Resolve all **Critical issues** before continuing.

---

## 7. Refactoring

Run the `refactor-class` skill if the class has become complex.

The goal is to:

- simplify long methods
- extract helper methods
- remove duplicated logic
- improve readability

Refactoring must **not change observable behaviour**.

---

## 8. Tests

Write the two test classes.

### Black‑box tests

`ClassNameBlackBoxTest`

These tests verify the **public contract only**.

They must cover:

- normal behaviour
- boundary conditions
- error scenarios

They should be writable **without reading the implementation**.

### White‑box tests

`ClassNameWhiteBoxTest`

These tests target **implementation risks**, such as:

- resize boundaries
- collision scenarios
- structural invariants
- concurrent modification cases

Each test must explain **why the scenario is risky**.

---

## 9. Test coverage review

Run the `test-coverage-review` skill.

Verify that:

- all public methods are tested
- mutation sequences are covered
- edge cases are tested
- internal edge cases are exercised

Add missing tests if necessary.

---

## 10. Documentation

Update project documentation when public behaviour changes.

This may include:

- `README.md`
- `ARCHITECTURE.md`

Run the `update-readme` skill when a new class or public API change is introduced.

Ensure examples compile against the real API.

---

## 11. Session wrap‑up

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

### Invariants must always hold

All mutation methods must preserve class invariants.
If invariants are temporarily broken during a method, they must be restored before returning.

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

| Skill | Role |
|------|------|
| `design-review` | Validate architecture before coding |
| `static-analysis` | Detect structural and contract issues |
| `refactor-class` | Improve code structure |
| `test-coverage-review` | Verify test completeness |
| `update-backlog` | Maintain project task tracking |
| `update-readme` | Keep documentation up to date |
