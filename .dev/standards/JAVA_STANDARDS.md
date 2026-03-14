# Java Standards for odd-collections

Coding standards and practices to follow for every Java class generated or
modified in this project.

---

## 1. Oracle guidelines compliance

### Source file structure

Elements in a Java source file must appear in the following order:

1. Beginning file comment (see below)
2. `package` statement
3. `import` statements (explicit, see below)
4. Class or interface declaration, with Javadoc

### Beginning file comment

Every source file must begin with a C-style comment containing:
- The class name
- The version
- The copyright notice (LGPL license)

Example:
```
/*
 * ClassName.java
 *
 * Version 1.0
 *
 * odd-collections - A collection of unconventional Java data structures
 * Copyright (C) 2026  Dufrenoy
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, see
 * <https://www.gnu.org/licenses/>.
 */
```

### Imports

- Wildcard imports (`import java.util.*`) are forbidden
- Each imported class must have its own `import` statement
- Imports must be sorted alphabetically within each group
- Fully qualified references in code (e.g. `java.util.function.BiFunction`)
  must be replaced by explicit imports

### Declaration order within a class

1. Static variables (`public`, then `protected`, then package, then `private`)
2. Instance variables (`public`, then `protected`, then package, then `private`)
3. Constructors
4. Methods (grouped by functionality, not by visibility)

### Section separators

Custom decorative separators are allowed and encouraged for readability,
for example:

```java
// ─── Section name ─────────────────────────────────────────────────────────────
```

---

## 2. Javadoc

- All public classes must have a class-level Javadoc comment
- All public methods must have a Javadoc comment
- `@Override` methods may omit Javadoc if their behavior is identical to the
  parent contract, but must have one if the behavior differs (e.g. an exception
  is thrown, or the semantics are different)
- Parameters (`@param`), return values (`@return`), and exceptions (`@throws`)
  must be documented

---

## 3. Inheritance and contracts

- Prefer `extends AbstractXxx` (e.g. `AbstractMap`, `AbstractSet`) when
  available, to inherit `equals()`, `hashCode()`, `toString()`, and default
  method implementations
- Always override methods whose default implementation in the abstract parent
  class is less efficient than what our data structure can provide
- Never let an inherited method silently throw `UnsupportedOperationException`
  without it being intentional and documented

---

## 4. Tests

### Three levels of testing

Each interface/implementation group has three levels of tests:

#### Level 1 — Interface contract test (`FooTest`)

One test class per **interface**, using a `MockFoo` implementation.

- Tests the interface contract in isolation — that the methods exist, that
  enums are correct, that exceptions are thrown at the right places
- Uses a `MockFoo` that implements the interface by delegating to a simple
  standard library structure (e.g. `ArrayList`, `HashMap`), with no knowledge
  of any real implementation
- Must be writable without reading any concrete implementation
- Named `FooTest` (e.g. `ChunkyListTest`, `SymmetricMapTest`)

The `MockFoo` class:
- Is package-private, in the same test package
- Implements the interface minimally — just enough to satisfy the contract
- Lives in `src/test/java/fr/dufrenoy/util/`

#### Level 2 — Black-box tests (`FooImplBlackBoxTest`)

One test class per **concrete implementation**, using that implementation
directly.

- Tests that the implementation correctly fulfils the full interface contract
- No knowledge of the internal implementation is assumed or used
- Must be writable without reading the source code of the implementation
- Named `ConcreteClassBlackBoxTest` (e.g. `UnsynchronizedSymmetricMapBlackBoxTest`,
  `SynchronizedChunkyListBlackBoxTest`)
- For thread-safe implementations, also covers concurrency guarantees
  (snapshot iterator behaviour, thread-safety of writes, etc.) — these are
  part of the public contract documented in the Javadoc

#### Level 3 — White-box tests (`FooImplWhiteBoxTest`)

One test class per **concrete implementation**, leveraging knowledge of
the internal structure.

- Tests technically risky internal areas: hash collisions, resize boundaries,
  internal structure consistency, lock ordering, algorithmic edge cases
- Each test must have a comment explaining *why* it targets a specific
  internal point
- Named `ConcreteClassWhiteBoxTest` (e.g. `UnsynchronizedSymmetricMapWhiteBoxTest`,
  `SynchronizedChunkyListWhiteBoxTest`)

### Summary table

| Class type | Test class | What it tests |
|---|---|---|
| Interface `Foo` | `FooTest` (uses `MockFoo`) | Interface contract in isolation |
| `UnsynchronizedFoo` | `UnsynchronizedFooBlackBoxTest` | Full contract via real implementation |
| `UnsynchronizedFoo` | `UnsynchronizedFooWhiteBoxTest` | Internal structure and edge cases |
| `SynchronizedFoo` | `SynchronizedFooBlackBoxTest` | Full contract + concurrency guarantees |
| `SynchronizedFoo` | `SynchronizedFooWhiteBoxTest` | Internal structure, locking, edge cases |

### General principles

- Systematically cover: nominal case, boundary cases (null, empty, single
  element), error cases (expected exception)
- Black-box tests at level 2 are intentionally similar across synchronized
  and unsynchronized implementations — the duplication is deliberate, as both
  must satisfy the same contract independently
- White-box tests must not duplicate black-box tests — they target only what
  cannot be observed through the public API

---

## 5. Null handling

Prefer `Optional<T>` over `null` as a return type, except in the following
cases:

- **Interface contracts** — never change the return type of a method imposed
  by an interface (e.g. `Map.get()` must return `null`, not `Optional<V>`)
- **Method parameters** — never use `Optional` as a parameter type
- **Instance fields** — `Optional` is not `Serializable` and adds memory
  overhead; use `null` for optional fields
- **Collections** — never use `Optional<Collection<T>>`; return an empty
  collection instead

In all other cases, methods whose return value may be absent must return
`Optional<T>` rather than a nullable type. This makes the contract explicit
in the method signature and prevents silent `NullPointerException`s.

When a method is constrained by an interface to return `null`, this must be
documented in its Javadoc with `@return ... or {@code null} if not found`.

---

## 6. Versioning

The version in each source file header must match the project version in
`pom.xml`. When the project version is bumped, all file headers must be
updated accordingly.

Project versioning follows **Semantic Versioning** (semver):

- **Patch (x.y.Z)** — bug fix, Javadoc improvement, internal refactoring with
  no behavioral change
- **Minor (x.Y.0)** — new public method, new feature, backward-compatible change
- **Major (X.0.0)** — method signature change, method removal, breaking change

---

## 7. Static analysis

### Claude-assisted analysis

After every class generation or significant modification, explicitly ask Claude
to perform a static analysis of the produced code. Claude will report issues
classified by severity:

- **Critical** — bugs, contract violations, broken invariants
- **Major** — performance issues, missing null checks, unclear semantics
- **Minor** — style issues, Javadoc gaps, guideline violations

Claude also verifies that all of the following are written in English:
- Comments and Javadoc
- Class, method, field, and variable names
- Exception messages
- Log messages

This analysis is probabilistic, not formal — it complements but does not
replace automated tools.

### Automated tools (Maven)

The following tools are part of the recommended build pipeline and run
automatically with Maven:

**Checkstyle** — enforces coding style and Oracle guideline compliance:
```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-checkstyle-plugin</artifactId>
  <version>3.3.1</version>
</plugin>
```

**SpotBugs** — detects bug patterns and potential runtime errors:
```xml
<plugin>
  <groupId>com.github.spotbugs</groupId>
  <artifactId>spotbugs-maven-plugin</artifactId>
  <version>4.8.3</version>
</plugin>
```

### Division of responsibilities

| Concern                        | Claude | Checkstyle | SpotBugs | JaCoCo |
|-------------------------------|--------|------------|----------|--------|
| Logic bugs and invariants     | ✓      |            | ✓        |        |
| Contract violations           | ✓      |            |          |        |
| Code style and guidelines     | ✓      | ✓          |          |        |
| Null pointer risks            | ✓      |            | ✓        |        |
| Performance (algorithmic)     | ✓      |            |          |        |
| Security vulnerabilities      |        |            | ✓        |        |
| English language              | ✓      |            |          |        |
| Code coverage                 |        |            |          | ✓      |