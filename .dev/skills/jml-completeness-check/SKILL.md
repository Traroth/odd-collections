---
name: jml-completeness-check
description: >
  Verify that the JML specifications are complete and up to date with
  respect to the implementation. Direction: Code -> JML. Use this skill
  after implementation is stable (after step 8) or after refactoring.
  Also trigger when the user says "are the specs complete", "missing
  contracts", or "JML coverage".
---

# JML Completeness Check — odd-collections

## Purpose

Verify that the **JML specifications are complete and up to date** with
respect to the code. This is the Code -> JML direction: given the
implementation, are all behaviours formally specified?

---

## When to run

- After step 8 (white-box tests) — the implementation is stable
- After refactoring a class that has JML annotations
- After adding a public method to a class with existing JML contracts
- When retrofitting JML annotations on an existing class

---

## Inputs

Before starting, read:

1. The class source file (implementation + JML annotations)
2. The corresponding interface (if any)
3. The relevant section of `INVARIANTS.md`
4. The class Javadoc (the informal contract)

---

## Checklist

### 1. Invariant completeness

- [ ] **Size consistency** — is there an invariant on `size() >= 0`?
- [ ] **Null handling** — is there an invariant expressing whether null
  elements/keys/values are allowed or forbidden?
- [ ] **Ordering** — if the structure maintains order, is the sort
  invariant expressed?
- [ ] **Uniqueness** — if duplicates are forbidden, is it expressed?
- [ ] **Structural integrity** — are internal structural invariants
  expressed (e.g. bijectivity, chunk chain consistency, tree balance)?
- [ ] **Configuration** — are non-null constraints on configurable
  fields expressed (strategies, comparators)?
- [ ] **Implicit invariants** — are there invariants that hold in the
  code but are not formalized? Look for:
  - Assertions or defensive checks that guard conditions never
    documented in JML
  - Comments like "this is always true at this point"
  - Fields that are never null but lack a `!= null` invariant

### 2. Constructor completeness

For each public constructor:

- [ ] **Pre-conditions present** — parameters with constraints have
  `@requires` (e.g. `capacity >= 1`, `loadFactor > 0`)
- [ ] **Post-conditions present** — state after construction is
  specified (e.g. `size() == 0`, configuration values set)
- [ ] **Copy constructors** — `size()` and element equality specified

### 3. Method completeness

For each public method:

- [ ] **Has JML annotations** — every public method that is not a
  trivial getter should have at least one `@requires` or `@ensures`
- [ ] **Pre-conditions complete** — all parameter constraints documented
  in Javadoc have a corresponding `@requires`
- [ ] **Post-conditions complete** — all behavioural guarantees
  documented in Javadoc have a corresponding `@ensures`
- [ ] **Return value specified** — methods with non-void return have
  `@ensures \result ...`
- [ ] **Size effects specified** — mutation methods specify the size
  change (`size() == \old(size()) + 1`, etc.)
- [ ] **Absence effects specified** — removal methods specify that the
  element is no longer present
- [ ] **Overriding methods** — specs start with `//@ also`

### 4. Purity annotations

- [ ] **Query methods annotated** — methods with no side effects are
  annotated `/*@ pure @*/` or `/*@ strictly_pure @*/`
- [ ] **Inherited purity** — if the parent declares a method as pure,
  the override is also annotated

### 5. Cross-check with INVARIANTS.md

- [ ] Every invariant in `INVARIANTS.md` has a corresponding
  `@invariant` annotation in the code
- [ ] Every `@invariant` in the code is documented in `INVARIANTS.md`
- [ ] No discrepancy in formulation between the two

### 6. Synchronized variants

- [ ] The synchronized variant has the same invariants as the
  unsynchronized variant
- [ ] No contract is missing from the synchronized variant that is
  present in the unsynchronized one

---

## Output format

Report findings grouped by category:

1. **Missing invariants** — invariants that should exist but don't
2. **Missing method contracts** — public methods without JML annotations
3. **Incomplete contracts** — methods with partial specs (e.g. has
   `@ensures` but missing `@requires`, or specifies return value but
   not size change)
4. **Stale contracts** — JML annotations that no longer match the code
   (e.g. method signature changed, behaviour modified)
5. **INVARIANTS.md drift** — discrepancies between the code annotations
   and the documentation

For each finding, specify what is missing and suggest the annotation
to add.