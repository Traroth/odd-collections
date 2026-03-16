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

### 1. Naming

- [ ] Method names follow standard Java collection conventions
- [ ] Similar methods use consistent naming across classes

### 2. Null handling

- [ ] Null handling matches library standards
- [ ] `Optional` used consistently

### 3. Exceptions

- [ ] Exceptions match Java standard collections behaviour
- [ ] No unexpected runtime exceptions

### 4. Symmetry

- [ ] Methods that conceptually form pairs are symmetrical
- [ ] Behaviour consistent with similar structures

---

## Output format
