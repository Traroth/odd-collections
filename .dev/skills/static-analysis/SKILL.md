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

Focus especially on **mutation methods** (`put`, `remove`, `add`, `set`,
`clear`, `rehash`, `resize`), as they are the most common source of
structural bugs.

---

# Analysis checklist

Run every item below. Report only findings — skip items with no issues.

---

# 1. Critical — bugs, contract violations, broken invariants

### Logic and contract violations

- [ ] Logic bugs (off-by-one, wrong condition, unreachable branch)
- [ ] Broken invariants (e.g. bijectivity in `SymmetricMap`, chunk integrity in `ChunkyList`)
- [ ] `UnsupportedOperationException` thrown silently by an inherited method without
  being documented
- [ ] `AbstractMap` / `AbstractList` contract violations
  (e.g. `entrySet()` inconsistent with `size()`, `get()`, `containsKey()`)
- [ ] Concurrent modification not handled correctly in `SynchronizedChunkyList`
  (missing lock, wrong lock type)
- [ ] Resize/rehash leaving entries in an inconsistent state

### Structural invariants

- [ ] Class invariants not documented (preferably using JML `@invariant`)
- [ ] Constructor does not establish all invariants
- [ ] Mutation methods break invariants
- [ ] Temporary invariant violations not restored before method exit

Examples of invariant risks:

- forward/backward maps not synchronized
- chunk sizes inconsistent with element count
- index structures not updated after mutation

---

# 2. Major — performance, mutation safety, iterator correctness

### Complexity

- [ ] Method expected to be O(1) accidentally iterates the whole structure
- [ ] Method inherited from `AbstractXxx` causing hidden O(n) behaviour
- [ ] Resize or rehash logic breaking amortized complexity guarantees

### Mutation consistency

- [ ] All internal structures not updated consistently after mutation
- [ ] `size()` inconsistent after add/remove operations
- [ ] `remove()` leaves stale references
- [ ] `clear()` does not fully reset the structure

### Iterator correctness

- [ ] Iterator does not respect the contract of the implemented interface
- [ ] `iterator.remove()` incorrect or unsupported without documentation
- [ ] Missing `ConcurrentModificationException` where fail-fast behaviour is expected

### Null safety

- [ ] Missing `Objects.requireNonNull` or null check where the contract requires it
- [ ] `null` returned where `Optional` is required (see null handling rules)
- [ ] `Optional` used as parameter type or collection element (forbidden by standards)

### Behavioural consistency

- [ ] `setValue()` on an entry not throwing `UnsupportedOperationException`
  (breaks bijectivity in `SymmetricMap`)
- [ ] Strategy change not atomic in `SynchronizedChunkyList`
- [ ] Snapshot cost not documented in Javadoc for copy-based operations

---

# 3. Minor — style, documentation, guideline compliance

### File structure

- [ ] File header missing or malformed (must be C-style, before `package`)
- [ ] `package` statement not immediately after file header
- [ ] Version in file header does not match `pom.xml`

### Imports

- [ ] Wildcard import present
- [ ] Imports not sorted alphabetically within their group
- [ ] Fully qualified reference in code instead of explicit import

### Code organisation

- [ ] Declaration order violated
  (statics → instance fields → constructors → methods)
- [ ] Section separator missing or malformed

### Documentation

- [ ] Javadoc missing on a public class or public method
- [ ] `@param`, `@return`, or `@throws` missing where applicable
- [ ] `@Override` method with different semantics has no Javadoc

### Language

- [ ] Any identifier, comment, Javadoc, exception message, or log message not in English

---

# Output format
