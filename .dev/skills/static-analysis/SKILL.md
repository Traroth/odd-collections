---
name: static-analysis
description: >
  Perform a structured static analysis of any Java class in odd-collections.
  Use this skill after every class generation or significant modification —
  even if the user doesn't explicitly ask for it. Also trigger when the user
  says "analyse", "check", "review", "audit", or "is this correct".
---

# Static Analysis — odd-collections

## When to run

- After generating or significantly modifying any Java class
- When the user asks for a review, audit, or correctness check
- Before marking a backlog task as done

---

## Analysis checklist

Run every item below. Report only findings — skip items with no issues.

### 1. Critical — bugs, contract violations, broken invariants

- [ ] Logic bugs (off-by-one, wrong condition, unreachable branch)
- [ ] Broken invariants (e.g. bijectivity in `SymmetricMap`, chunk integrity in `ChunkyList`)
- [ ] `UnsupportedOperationException` thrown silently by an inherited method without
  being documented
- [ ] `AbstractMap` / `AbstractList` contract violations (e.g. `entrySet()` inconsistent
  with `size()`, `get()`, `containsKey()`)
- [ ] Concurrent modification not handled correctly in `SynchronizedChunkyList`
  (missing lock, wrong lock type)
- [ ] `null` returned where `Optional` is required (see null handling rules)
- [ ] Resize/rehash leaving entries in an inconsistent state

### 2. Major — performance, null safety, unclear semantics

- [ ] Method inherited from `AbstractXxx` that iterates `entrySet()` / `get(int)` when
  a direct O(1) implementation is possible — should be overridden
- [ ] Missing `Objects.requireNonNull` or null check where the contract requires it
- [ ] `setValue()` on an entry not throwing `UnsupportedOperationException` (breaks
  bijectivity in `SymmetricMap`)
- [ ] Snapshot cost not documented in Javadoc for methods that copy the list
  (`SynchronizedChunkyList`)
- [ ] Strategy change not atomic in `SynchronizedChunkyList` (growing and shrinking
  changed separately rather than via `setStrategies()`)
- [ ] `Optional` used as parameter type or collection element (forbidden by standards)

### 3. Minor — style, Javadoc, guideline compliance

- [ ] File header missing or malformed (must be C-style, before `package`)
- [ ] `package` statement not immediately after file header
- [ ] Wildcard import present
- [ ] Imports not sorted alphabetically within their group
- [ ] Fully qualified reference in code instead of explicit import
- [ ] Declaration order violated (statics → instance fields → constructors → methods)
- [ ] Section separator missing or malformed
- [ ] Javadoc missing on a public class or public method
- [ ] `@param`, `@return`, or `@throws` missing where applicable
- [ ] `@Override` method with different semantics has no Javadoc
- [ ] Any identifier, comment, Javadoc, exception message, or log message not in English
- [ ] Version in file header does not match `pom.xml`

---

## Output format

```
## Static analysis — ClassName

### Critical
- [description of issue, line reference if possible, suggested fix]

### Major
- [description of issue, line reference if possible, suggested fix]

### Minor
- [description of issue, line reference if possible, suggested fix]

### ✅ No issues found in: [list categories with zero findings]
```

If there are zero findings across all categories, output:

```
## Static analysis — ClassName
✅ No issues found.
```

---

## Notes

- This analysis is probabilistic, not formal. It complements but does not replace
  Checkstyle, SpotBugs, and JaCoCo.
- Do not report Checkstyle or SpotBugs concerns as Critical unless they also represent
  a logic bug or contract violation.