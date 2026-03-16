---
name: test-coverage-review
description: >
  Review the test suite of a class in odd-collections and identify missing
  test cases. Use this skill after writing tests or when the user asks for a
  coverage review.
---

# Test Coverage Review — odd-collections

## When to run

- After writing `BlackBoxTest` and `WhiteBoxTest`
- Before marking a backlog task as done
- When the user says "review the tests" or "is the test coverage sufficient"

---

## Coverage checklist

### 1. Public API coverage

- [ ] Every public method has at least one test
- [ ] Both nominal and error cases are tested
- [ ] Boundary cases covered (empty, single element, null where applicable)

### 2. Behavioural coverage

- [ ] Tests verify observable behaviour rather than implementation details
- [ ] Contract of the implemented interface respected

### 3. Mutation scenarios

- [ ] Add / remove operations tested
- [ ] Structure integrity verified after mutations
- [ ] Sequences of operations tested

### 4. Edge cases

- [ ] Empty structure behaviour
- [ ] Large structure behaviour
- [ ] Boundary values

### 5. White-box coverage

- [ ] Internal edge cases tested
- [ ] Resize / rehash boundaries tested
- [ ] Collision scenarios tested where applicable

---

## Output format
