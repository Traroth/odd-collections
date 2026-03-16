---
name: design-review
description: >
  Perform a design review for a new class or data structure in odd-collections.
  Use this skill before implementing any non-trivial class or when the user
  explicitly asks for a design validation. The goal is to validate the core
  structure, invariants, complexity, and alternatives before any code is written.
---

# Design Review — odd-collections

## When to run

- Before implementing a new class or data structure
- When the user proposes a new design
- When the user says "review the design", "is this a good structure",
  or "what do you think of this architecture"

---

## Review checklist

Report only findings or recommendations.

### 1. Core data structure

- [ ] The underlying data structure is appropriate for the required operations
- [ ] The structure has clearly defined invariants
- [ ] The invariants can be preserved by all mutation operations
- [ ] The structure does not introduce unnecessary complexity

### 2. Interface and contracts

- [ ] The class implements the correct Java interface (`List`, `Map`, `Set`, etc.)
- [ ] The correct `AbstractXxx` base class is used when applicable
- [ ] The public API is minimal and coherent
- [ ] Method semantics match the expectations of the implemented interface

### 3. Complexity guarantees

- [ ] Expected complexity for key operations is clearly defined
- [ ] The chosen structure supports these complexity guarantees
- [ ] No operation requires full traversal where O(1) or O(log n) is expected

### 4. Invariants

- [ ] Structural invariants are clearly defined
- [ ] Invariants can be documented using JML `@invariant`
- [ ] All mutation methods can preserve these invariants

### 5. Thread safety

- [ ] Thread-safety requirements are clear
- [ ] If applicable, a synchronized variant is considered
- [ ] The locking strategy is realistic and safe

### 6. Alternatives

For each relevant alternative:

- [ ] Alternative design identified
- [ ] Advantages and disadvantages compared
- [ ] Reason for rejection documented

---

## Output format
