---
name: api-consistency-check
description: >
  Verify that the public API of a class in odd-collections follows consistent
  naming, null handling, and design conventions across the library.
---

# API Consistency Check — odd-collections

## When to run

- After adding new public methods
- When the user asks for an API review
- Before releasing a new version

---

## Consistency checklist

Report only findings or recommendations.

### 1. Naming

- [ ] Method names follow standard Java collection conventions (`get`, `put`,
  `remove`, `contains`, `size`, `clear`, `iterator`, etc.)
- [ ] Symmetric methods use consistent naming across classes
  (e.g. `getKey` / `get`, `removeByValue` / `remove`)
- [ ] Boolean methods use `is` or `has` prefix where appropriate
- [ ] No abbreviations unless they are standard Java convention

### 2. Null handling

- [ ] Methods whose result may be absent return `Optional<T>`, except where
  an interface contract forbids it
- [ ] `Optional` is never used as a parameter type or field type
- [ ] Methods constrained by an interface to return `null` document it with
  `@return ... or {@code null} if not found`
- [ ] `Objects.requireNonNull` or explicit null check used where the contract
  requires it

### 3. Exceptions

- [ ] `IllegalArgumentException` used for invalid arguments
- [ ] `IndexOutOfBoundsException` used for invalid indices
- [ ] `UnsupportedOperationException` used for unsupported operations, and
  always documented in Javadoc
- [ ] No unexpected `NullPointerException` thrown without documentation
- [ ] Exception messages are in English and descriptive

### 4. Symmetry

- [ ] Methods that conceptually form pairs are symmetrical in name, signature,
  and return type (e.g. `put`/`safePut`, `remove`/`removeByValue`,
  `get`/`getKey`)
- [ ] Synchronized variants expose the same public API as their unsynchronized
  counterparts, plus any concurrency-specific methods (e.g. `reorganize(boolean)`)
- [ ] Covariant return types are used consistently across implementations
  (e.g. `inverse()` returns the concrete type, not the interface)

### 5. Javadoc

- [ ] All public methods have Javadoc
- [ ] `@param`, `@return`, `@throws` present where applicable
- [ ] Overrides with different semantics have their own Javadoc
- [ ] Snapshot cost documented for copy-based operations in synchronized classes

### 6. Versioning

- [ ] File header version matches `pom.xml`
- [ ] New public methods justify a minor version bump (semver)

---

## Output format

Report findings grouped by category. For each finding, state:
- The method or class concerned
- The violation
- The recommended fix

If no issues are found in a category, skip it entirely.