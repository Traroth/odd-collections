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

### Two separate test classes per tested class

For each class `Foo`, create:

- `FooBlackBoxTest` — black-box tests, based solely on the public contract and
  Javadoc, with no knowledge of the internal implementation
- `FooWhiteBoxTest` — white-box tests, leveraging knowledge of the
  implementation to target technically risky areas (hash collisions, resize,
  internal structure consistency, algorithmic edge cases)

### General principles

- Black-box tests must be writable without reading the source code
- White-box tests must document why they target a specific internal point
- Systematically cover: nominal case, boundary cases (null, empty, single
  element), error cases (expected exception)