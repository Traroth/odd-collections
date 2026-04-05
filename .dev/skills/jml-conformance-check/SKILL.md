---
name: jml-conformance-check
description: >
  Verify that the implementation conforms to its JML specifications
  (invariants, requires, ensures). Direction: JML -> Code. Use this skill
  after implementation (step 7) or after modifying a class that has JML
  annotations. Also trigger when the user says "check conformance",
  "verify contracts", or "does the code match the specs".
---

# JML Conformance Check — odd-collections

## Purpose

Verify that the **code conforms to the JML specifications**. This is the
JML -> Code direction: given the contracts, does the implementation
respect them?

This skill complements OpenJML's `--esc` verification. OpenJML performs
formal static verification but may not be available or may crash on some
inputs. This skill performs a structured manual review.

---

## When to run

- After step 7 (implementation) when JML specs were written at step 2
- After modifying a method that has JML contracts
- After fixing a bug — to verify the fix didn't break a contract
- When OpenJML is unavailable or reports an internal error

---

## Inputs

Before starting, read:

1. The class source file (implementation + JML annotations)
2. The corresponding interface (if any) — check for inherited contracts
3. The relevant section of `INVARIANTS.md`

---

## Checklist

### 1. Class invariants

For each `@invariant` annotation in the class:

- [ ] **Established by constructors** — every constructor leaves the
  object in a state that satisfies all invariants
- [ ] **Preserved by mutation methods** — every method that modifies
  state restores all invariants before returning
- [ ] **Temporarily broken invariants** — if an invariant is broken
  mid-method, verify it is restored before any return path (including
  exceptional exits)
- [ ] **Not bypassed by internal helpers** — private methods called by
  public methods must not leave invariants broken if the caller does
  not restore them

### 2. Pre-conditions (`@requires`)

For each `@requires` clause:

- [ ] **Validated or assumed** — the method either checks the
  pre-condition and throws an appropriate exception, or documents that
  the caller is responsible
- [ ] **Consistent with Javadoc** — the `@requires` clause matches the
  `@throws` documentation (e.g. `requires index >= 0` corresponds to
  `@throws IndexOutOfBoundsException`)
- [ ] **Not silently ignored** — the method does not proceed normally
  when the pre-condition is violated

### 3. Post-conditions (`@ensures`)

For each `@ensures` clause:

- [ ] **Satisfied on all return paths** — including early returns and
  edge cases (empty collection, single element, null parameter where
  allowed)
- [ ] **Satisfied for boundary values** — size 0, size 1, maximum
  capacity, first/last element
- [ ] **Return value correct** — if the contract specifies `\result`,
  verify the actual return value matches
- [ ] **Size changes correct** — if the contract specifies
  `size() == \old(size()) + 1`, verify that `size` is incremented
  exactly once, on all paths
- [ ] **Side effects match** — if the contract says an element is now
  present (`containsKey(key)`), verify that it is actually inserted

### 4. Purity annotations

- [ ] Methods annotated `/*@ pure @*/` have no side effects (no field
  writes, no calls to non-pure methods)
- [ ] Methods annotated `/*@ strictly_pure @*/` additionally perform no
  allocation and call only `strictly_pure` methods
- [ ] Overriding methods inherit the purity constraint of the parent

### 5. `also` keyword

- [ ] Every JML spec on a method that overrides a parent method starts
  with `//@ also`

### 6. Cross-check with INVARIANTS.md

- [ ] Every invariant listed in `INVARIANTS.md` has a corresponding
  `@invariant` annotation in the code
- [ ] No invariant in the code contradicts `INVARIANTS.md`

---

## Output format

Report findings grouped by severity:

1. **Violations** — the code does not satisfy a JML contract
2. **Risks** — the code probably satisfies the contract but the
   reasoning is fragile (e.g. depends on insertion order, hash
   distribution, or undocumented JDK behaviour)
3. **OK** — contracts verified, no issues found

For each violation, specify:
- The JML annotation (quote it)
- The method or constructor where it fails
- The execution path or input that triggers the violation