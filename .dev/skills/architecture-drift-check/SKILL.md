---
name: architecture-drift-check
description: >
  Verify that the current implementation of a class still matches the design
  decisions documented in ARCHITECTURE.md and INVARIANTS.md.
---

# Architecture Drift Check — odd-collections

## When to run

- After significant refactoring
- Before a release
- When the user asks if the implementation still follows the design
- When a new feature is added to an existing class

---

## Before running

Read the following files in full:

1. `.dev/design/ARCHITECTURE.md` — design decisions and rationale
2. `.dev/design/INVARIANTS.md` — structural invariants

---

## Review checklist

Report only findings — skip items with no issues.

### 1. Data structure

- [ ] The internal data structure matches the description in `ARCHITECTURE.md`
- [ ] No new internal structures have been introduced without being documented
- [ ] Complexity guarantees are still met (O(1) average for `get`, `getKey`,
  `put`, `remove` in `SymmetricMap`; O(1) amortized for `add`, `remove` in
  `ChunkyList`)

### 2. Invariants

- [ ] All invariants documented in `INVARIANTS.md` still hold
- [ ] Mutation methods restore invariants before returning
- [ ] No new mutation method has been added without verifying invariant
  preservation

### 3. Thread safety

- [ ] Locking strategy matches the documented design
  (`ReentrantReadWriteLock`, read lock for reads, write lock for writes)
- [ ] No operation reads shared state without a read lock
- [ ] No operation writes shared state without a write lock
- [ ] `setStrategies()` remains atomic (single write lock for both strategies)
- [ ] `replaceAll()` remains fully atomic in `SynchronizedSymmetricMap`
- [ ] Snapshot-based iterators still copy under read lock

### 4. Public API

- [ ] Public API matches the documented design in `ARCHITECTURE.md`
- [ ] No public method has been added or removed without updating
  `ARCHITECTURE.md` and `BACKLOG.md`
- [ ] Covariant return types preserved (`inverse()`, `values()`)
- [ ] `values()` still returns `Set<V>` in both `SymmetricMap` implementations

### 5. Design decisions

For each design decision in `ARCHITECTURE.md`, verify that the implementation
still honours the stated rationale:

- [ ] `SymmetricMap` — single bucket array with two collision chains
  (not two separate maps)
- [ ] `put` permissive, `safePut` strict — no inversion or conflation
- [ ] `inverse()` returns an independent copy, not a live view
- [ ] Iterators in synchronized classes are snapshot-based, not fail-fast
- [ ] `reorganize(boolean)` — blocking and non-blocking modes preserved
- [ ] `Entry.setValue()` delegates to `put()` to maintain bijectivity

### 6. Documentation consistency

- [ ] `ARCHITECTURE.md` reflects the current implementation
- [ ] `INVARIANTS.md` reflects the current invariants
- [ ] Any design decision made during this session has been added to
  `ARCHITECTURE.md`

---

## Output format

Report findings grouped by category. For each finding, state:
- The class or method concerned
- The drift detected (what the architecture says vs. what the code does)
- The recommended action (update the code, or update the documentation)

If no drift is found in a category, skip it entirely.