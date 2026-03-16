---
name: refactor-class
description: >
  Refactor an existing Java class in odd-collections to improve readability,
  maintainability, and structure without changing its observable behaviour.
  Use this skill after a class has grown significantly or when the user asks
  for refactoring.
---

# Refactor Class — odd-collections

## When to run

- After implementing a complex class
- When a class exceeds ~300 lines
- When the user asks for "refactor", "clean up", or "improve this class"

---

## Refactoring checklist

Focus on structural improvements without altering behaviour.

### 1. Method structure

- [ ] Long methods identified and simplified
- [ ] Repeated code extracted into private helper methods
- [ ] Complex logic split into smaller methods
- [ ] Method names clearly describe behaviour

### 2. Class structure

- [ ] Fields grouped logically
- [ ] Declaration order follows project standards
- [ ] Responsibilities clearly separated

### 3. Encapsulation

- [ ] Fields use the most restrictive visibility possible
- [ ] Internal structures are not unnecessarily exposed
- [ ] Defensive copies added where required

### 4. Duplication

- [ ] Duplicate logic detected
- [ ] Reusable helper methods introduced

### 5. Invariants

- [ ] Refactoring does not break class invariants
- [ ] Mutation methods remain consistent

---

## Output format
