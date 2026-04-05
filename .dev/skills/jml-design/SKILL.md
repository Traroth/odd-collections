---
name: jml-design
description: >
  Write JML specifications (invariants, requires, ensures) for a class in
  odd-collections. Use this skill during step 2 of the workflow (stub + JML)
  or when adding JML annotations to an existing class. The goal is to produce
  a complete, consistent, and verifiable formal contract before implementation.
---

# JML Design — odd-collections

## When to run

- During step 2 of the workflow (stub class + JML specifications)
- When adding JML annotations to an existing class
- When the user says "add JML", "write contracts", or "specify invariants"

---

## Inputs

Before starting, read:

1. The class stub or source file
2. The relevant section of `INVARIANTS.md` (if it exists)
3. The class Javadoc (the informal contract)
4. The corresponding interface (if the class implements one)

---

## Specification checklist

### 1. Class-level invariants (`/*@ ... @*/`)

Place the invariant block inside the class body, before the first field or constructor.

- [ ] **Size consistency** — `size() >= 0`, and relationship to internal
  storage (e.g. `size == root.subtreeSize`, `size == sum of chunk sizes`)
- [ ] **Null handling** — whether null elements/keys/values are allowed
  or forbidden
- [ ] **Ordering** — if the structure maintains order, express the sort
  invariant (e.g. `compare(get(i), get(i+1)) < 0`)
- [ ] **Uniqueness** — if duplicates are forbidden, express it
- [ ] **Bijectivity** — for bijective structures, express the forward/reverse
  mapping consistency
- [ ] **Configuration** — non-null constraints on configurable fields
  (strategies, comparators, etc.)

### 2. Constructor contracts (`//@ ensures`)

Each constructor must specify:

- [ ] Post-conditions on `size()` (typically `== 0` for empty constructors)
- [ ] Post-conditions on configuration (chunk size, strategies, comparator)
- [ ] Pre-conditions (`//@ requires`) for parameters with constraints
  (e.g. `chunkSize >= 1`, `initialCapacity >= 1`)
- [ ] For copy constructors: `size() == other.size()` and element equality

### 3. Query method contracts

- [ ] `size()` — `ensures \result >= 0`
- [ ] `isEmpty()` — `ensures \result <==> size() == 0`
- [ ] `contains()` / `containsKey()` / `containsValue()` — relationship to
  other methods where expressible
- [ ] `get()` — pre-conditions on index bounds, post-conditions on result
- [ ] `indexOf()` / `lastIndexOf()` — result range and relationship to
  `contains()`

### 4. Mutation method contracts

These are the most important contracts. For each mutation method:

- [ ] **Pre-conditions** (`//@ requires`) — parameter constraints, index
  bounds, null rejection
- [ ] **Post-conditions** (`//@ ensures`) — size change, element presence/
  absence, return value
- [ ] **Frame conditions** — what is *not* changed (e.g. `size() == \old(size())`
  for `set()`, `reorganize()`)

Pay special attention to:

- `add` / `put` — size increment, element now present
- `remove` — size decrement, element now absent, return value equals removed
  element
- `clear` — `size() == 0`
- `set` / `replace` — size unchanged, element at position updated

### 5. View methods

- [ ] `keySet()`, `values()`, `entrySet()` — result not null, size equals
  map size
- [ ] `inverse()` — result not null, size preserved, mapping inverted

---

## JML syntax reference

```java
// Class-level invariant block (inside class body, before first field)
    /*@
      @ public invariant size() >= 0;
      @ public invariant (\forall int i; 0 <= i && i < size(); get(i) != null);
      @*/

// Method-level annotations (before method declaration, after Javadoc)
//@ requires index >= 0 && index < size();
//@ ensures \result != null;
//@ ensures size() == \old(size());

// Quantifiers
//@ ensures (\forall int i; 0 <= i && i < size(); get(i) != null);
//@ ensures (\exists int i; 0 <= i && i < size(); get(i).equals(o));
```

---

## Consistency rules

- All invariants in `INVARIANTS.md` must have a corresponding JML annotation
  in the code
- JML annotations must not reference private fields — use public methods only
- Pre-conditions must match the exceptions documented in Javadoc
- Post-conditions must be verifiable through the public API
- Synchronized variants must have the same contracts as their unsynchronized
  counterparts

---

## Output format

Report the annotations added, grouped by category:

1. **Invariants** — list each class-level invariant
2. **Constructors** — list contracts per constructor
3. **Methods** — list contracts per method

Flag any inconsistency found between `INVARIANTS.md` and the code.