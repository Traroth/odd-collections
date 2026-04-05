---
name: jml-test-generation
description: >
  Generate test cases derived from JML specifications (invariants, requires,
  ensures) for a class in odd-collections. Use this skill after JML contracts
  are in place and tests need to be written or reviewed. The goal is to ensure
  that every JML contract is exercised by at least one test.
---

# JML Test Generation — odd-collections

## When to run

- During step 6 of the workflow (tests — TDD) after JML contracts are written
- During step 10 (test coverage review) to identify gaps
- When the user says "generate tests from JML", "test the contracts", or
  "are the JML specs covered"

## Background

There is no actively maintained tool for automatic test generation from JML
specifications (JMLUnit and JMLUnit-NG are dormant since ~2014 and do not
support modern Java). This skill relies on Claude reading the JML annotations
and systematically deriving test cases from them.

OpenJML RAC (Runtime Assertion Checking) can complement this approach: code
compiled with the `openjml` Maven profile will check pre/post-conditions and
invariants at runtime during test execution.

---

## Inputs

Before starting, read:

1. The class source file (with JML annotations)
2. The corresponding interface (if applicable)
3. The existing test files (`FooTest`, `BlackBoxTest`, `WhiteBoxTest`)
4. `INVARIANTS.md` for the relevant class section

---

## Test derivation rules

### 1. From `@invariant` — invariant preservation tests

For each class-level invariant, generate tests that verify it holds after
every kind of mutation.

**Pattern:**

```
For each invariant I:
  For each mutation method M (add, remove, set, clear, put, ...):
    1. Create an instance in a known state
    2. Call M
    3. Assert I still holds
```

**Example:**

```java
// @invariant: size() >= 0
@Test
void sizeNeverNegativeAfterRemoveOnEmpty() {
    list.clear();
    assertThrows(IndexOutOfBoundsException.class, () -> list.remove(0));
    assertTrue(list.size() >= 0);
}
```

These tests belong in **BlackBoxTest** (observable through public API).

### 2. From `@requires` — pre-condition tests

For each `@requires` clause, generate two kinds of tests:

**a) Nominal case** — call with inputs that satisfy the pre-condition, verify
the method succeeds and the `@ensures` hold.

**b) Violation case** — call with inputs that violate the pre-condition,
verify the expected exception is thrown.

**Example:**

```java
// @requires 0 <= index && index < size()
@Test
void getWithValidIndex() {
    list.add("a");
    assertEquals("a", list.get(0));
}

@Test
void getWithNegativeIndexThrows() {
    list.add("a");
    assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
}

@Test
void getWithIndexEqualToSizeThrows() {
    list.add("a");
    assertThrows(IndexOutOfBoundsException.class, () -> list.get(1));
}
```

### 3. From `@ensures` — post-condition tests

For each `@ensures` clause, generate a test that:

1. Sets up a known pre-state
2. Calls the method
3. Asserts the post-condition

Pay special attention to `\old()` expressions — they require capturing state
before the call.

**Example:**

```java
// @ensures size() == \old(size()) + 1
@Test
void addIncrementsSizeByOne() {
    int before = list.size();
    list.add("x");
    assertEquals(before + 1, list.size());
}
```

### 4. From conditional `@ensures` — branch coverage

When an `@ensures` uses `==>` (implication), generate tests for both branches.

**Example:**

```java
// @ensures  \old(contains(o)) ==> size() == \old(size()) - 1
// @ensures !\old(contains(o)) ==> size() == \old(size())
@Test
void removeExistingElementDecreasesSize() {
    list.add("a");
    int before = list.size();
    list.remove("a");
    assertEquals(before - 1, list.size());
}

@Test
void removeAbsentElementKeepsSize() {
    list.add("a");
    int before = list.size();
    list.remove("z");
    assertEquals(before, list.size());
}
```

### 5. Boundary cases from quantifiers

When a `@requires` or `@ensures` uses quantifiers (`\forall`, `\exists`),
derive boundary tests:

- Empty collection (vacuously true quantifiers)
- Single element
- Multiple elements

---

## Coverage matrix

For each class, build a mental coverage matrix:

| JML clause | Method | Test exists? | Test name |
|---|---|---|---|

Report any uncovered clause.

---

## Test placement

| Derivation source | Test level |
|---|---|
| `@invariant` (observable) | BlackBoxTest |
| `@requires` / `@ensures` (public API) | BlackBoxTest |
| `@invariant` (internal structure) | WhiteBoxTest |

---

## Output format

1. **Coverage summary** — how many JML clauses exist, how many are covered
2. **Missing tests** — list of JML clauses without corresponding tests,
   with suggested test method signatures
3. **Generated tests** — if asked to write tests, provide the complete test
   methods with comments linking back to the JML clause