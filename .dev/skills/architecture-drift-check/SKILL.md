---
name: architecture-drift-check
description: >
  Verify that the current implementation of a class still matches the design
  decisions documented in ARCHITECTURE.md.
---

# Architecture Drift Check — odd-collections

## When to run

- After significant refactoring
- Before a release
- When the user asks if the implementation still follows the design

---

## Review checklist

### 1. Data structure

- [ ] Implementation matches the architecture description
- [ ] Internal structures remain consistent with the design

### 2. Invariants

- [ ] Invariants described in ARCHITECTURE.md still apply
- [ ] Code maintains these invariants

### 3. Responsibilities

- [ ] Class responsibilities unchanged
- [ ] No unintended responsibilities introduced

### 4. Public API

- [ ] Public API matches documented design
- [ ] No undocumented behaviour introduced

---

## Output format
