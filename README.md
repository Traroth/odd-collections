# odd-collections

A collection of unconventional Java data structures — original implementations that go beyond what the standard library offers, built for fun and exploration.

---

## Structures

### ChunkyList

An **unrolled linked list** implementation — a hybrid data structure that combines the memory locality of arrays with the dynamic resizing of linked lists.

#### How it works

Instead of storing elements one by one (like a `LinkedList`) or in a single contiguous array (like an `ArrayList`), a `ChunkyList` maintains a **doubly-linked chain of fixed-size arrays**, called Chunks.

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Chunk 1    │────▶│  Chunk 2    │────▶│  Chunk 3    │
│ [A, B, C]  │◀────│ [D, E, F]  │◀────│ [G, H, _]  │
└─────────────┘     └─────────────┘     └─────────────┘
```

This structure offers a middle ground between `ArrayList` and `LinkedList`:
- Better **memory locality** than `LinkedList` (elements within a chunk are contiguous)
- **No costly full-array copies** when inserting or removing, unlike `ArrayList`

#### Architecture

| Type | Role |
|---|---|
| `ChunkyList<E>` | Interface extending `java.util.List`, exposing chunk-specific configuration |
| `UnsynchronizedChunkyList<E>` | Standard implementation — not thread-safe, fail-fast iterators |
| `SynchronizedChunkyList<E>` | Thread-safe implementation backed by a `ReentrantReadWriteLock` |

#### Features

- Full `java.util.List` implementation (`get`, `set`, `add`, `remove`, `addAll`, `indexOf`, `lastIndexOf`, `contains`, `clear`, ...)
- Configurable **chunk size** (default: 100)
- Pluggable **growing and shrinking strategies**, swappable at runtime
- Atomic **`setStrategies()`** to change both strategies simultaneously without risk of inconsistent intermediate state
- Native **`Iterator`** for efficient sequential traversal — O(1) per element, avoiding the O(n²) cost of the default `AbstractList` iterator
- Native **`Spliterator`** for efficient `stream()` and `parallelStream()` support
- Optimized **`addAll`** using bulk `System.arraycopy` — fills and creates chunks directly without per-element overhead
- Fail-fast iterators (`UnsynchronizedChunkyList`) and fail-safe snapshot iterators (`SynchronizedChunkyList`)
- Copy constructors and collection constructors
- `reorganize()` to compact sparsely filled chunks

#### Strategies

**GrowingStrategy** — controls what happens when an element is inserted into a full chunk:

| Strategy | Behaviour |
|---|---|
| `OVERFLOW_STRATEGY` *(default)* | The overflowing element is pushed into the next chunk (created if necessary) |
| `EXTEND_STRATEGY` | A new chunk is created after the current one to hold the overflowing element |

**ShrinkingStrategy** — controls what happens after an element is removed from a chunk:

| Strategy | Behaviour |
|---|---|
| `UNDERFLOW_STRATEGY` *(default)* | The first element of the next chunk is pulled into the current one |
| `DISAPPEAR_STRATEGY` | The chunk is simply removed if it becomes empty |

The two strategies are designed as **symmetric pairs**:
- `OVERFLOW_STRATEGY` ↔ `UNDERFLOW_STRATEGY`
- `EXTEND_STRATEGY` ↔ `DISAPPEAR_STRATEGY`

Using a symmetric pair guarantees **structural reversibility**: inserting and then removing the same element leaves the list in its original state.

Strategies can be changed at any time:

```java
list.setCurrentGrowingStrategy(ChunkyList.GrowingStrategy.EXTEND_STRATEGY);
list.setCurrentShrinkingStrategy(ChunkyList.ShrinkingStrategy.DISAPPEAR_STRATEGY);

// Or atomically:
list.setStrategies(ChunkyList.GrowingStrategy.EXTEND_STRATEGY,
                   ChunkyList.ShrinkingStrategy.DISAPPEAR_STRATEGY);
```

#### Usage

```java
// Basic usage
ChunkyList<String> list = new UnsynchronizedChunkyList<>();
list.add("Hello");
list.add("World");
System.out.println(list.get(0)); // Hello

// Custom chunk size
ChunkyList<Integer> list = new UnsynchronizedChunkyList<>(50);

// From an existing collection
List<String> source = List.of("a", "b", "c");
ChunkyList<String> list = new UnsynchronizedChunkyList<>(source);

// Thread-safe
ChunkyList<String> list = new SynchronizedChunkyList<>();
```

#### Thread safety

`UnsynchronizedChunkyList` is **not thread-safe**. For concurrent access, use `SynchronizedChunkyList`, which is backed by a `ReentrantReadWriteLock`: multiple threads may read concurrently, while writes are exclusive.

Iterators, spliterators, and streams on a `SynchronizedChunkyList` operate on a **snapshot** of the list taken at the time of the call.

> **Memory note:** snapshot-based operations copy the entire list. Avoid calling them on very large lists in memory-constrained environments.

`SynchronizedChunkyList` provides two variants of `reorganize()`:

```java
// Blocking (default): holds the write lock for the full duration
list.reorganize();

// Non-blocking: snapshot + reorganize + swap under write lock
// Warning: modifications made between the snapshot and the swap are silently lost.
list.reorganize(false);
```

---

### SymmetricMap

A **bijective map** implementation — a map where both keys and values are unique, providing O(1) average lookup in both directions.

#### How it works

Unlike a standard `Map`, a `SymmetricMap` enforces uniqueness on both keys and values. Each entry can be looked up by key or by value with equal efficiency.

```
          get("a") ──▶ 1
"a" ◀──▶ 1
          getKey(1) ──▶ "a"
```

The internal structure is a **single array of buckets**, each holding two independent collision chains — one indexed by key hash, one by value hash. Each entry belongs to both chains simultaneously, providing O(1) average access in both directions without duplicating storage.

#### Architecture

| Type | Role |
|---|---|
| `SymmetricMap<K, V>` | Interface extending `java.util.Map`, declaring the symmetric contract |
| `UnsynchronizedSymmetricMap<K, V>` | Standard implementation extending `AbstractMap` — not thread-safe |
| `SynchronizedSymmetricMap<K, V>` | Thread-safe implementation backed by a `ReentrantReadWriteLock` |

#### Features

- Full `java.util.Map` implementation via `AbstractMap` (`get`, `put`, `remove`, `containsKey`, `containsValue`, `size`, `clear`, ...)
- **Reverse lookup** via `getKey(V value)` — returns `Optional<K>`
- **Permissive insertion** via `put(K, V)` — silently removes conflicting entries to maintain bijectivity
- **Strict insertion** via `safePut(K, V)` — throws `IllegalArgumentException` if the key or value already exists
- **Reverse removal** via `removeByValue(V value)` — returns `Optional<K>`
- **Inverse map** via `inverse()` — returns an independent copy with keys and values swapped
- `values()` returns `Set<V>` rather than `Collection<V>`, reflecting the uniqueness of values
- Snapshot-based iterators in `SynchronizedSymmetricMap`

#### Usage

```java
// Basic usage
SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
map.put("a", 1);
map.put("b", 2);

// Forward lookup
System.out.println(map.get("a"));        // 1

// Reverse lookup
        System.out.println(map.getKey(1).get()); // "a"

// Strict insertion — throws if key or value already exists
        map.safePut("c", 3);

// Reverse removal
map.removeByValue(2);                    // removes "b" -> 2

// Inverse map
SymmetricMap<Integer, String> inv = map.inverse();
System.out.println(inv.get(1));          // "a"

// Thread-safe
SymmetricMap<String, Integer> map = new SynchronizedSymmetricMap<>();
```

#### Thread safety

`UnsynchronizedSymmetricMap` is **not thread-safe**. For concurrent access, use `SynchronizedSymmetricMap`, which is backed by a `ReentrantReadWriteLock`: multiple threads may read concurrently, while writes are exclusive.

Iterators on `keySet()`, `values()`, and `entrySet()` in `SynchronizedSymmetricMap` operate on a **snapshot** of the map taken at the time of the call.

> **Memory note:** each call to an iterator copies the entire entry set. Avoid iterating over very large maps in memory-constrained environments.

---

## Roadmap

- **`Tuple`** — a type-safe immutable tuple system without one class per arity, with Java 21 record-based implementation via multi-release JAR
- **`CircularBuffer`** — a circular doubly-linked list backed by a ring of nodes
- **`SortedChunkyList`** — a `ChunkyList` that maintains elements in sorted order using a `Comparator`, with O(log n) insertion via binary search across chunks
- **`TreeList`** — a `List` backed by a red-black order-statistic tree, providing O(log n) access by index
- **`MultiMap`** — a multidimensional map with a configurable number of dimensions, supporting partial key lookups that return sub-maps

---

## Requirements

- **Java 11 or later** to use this library
- **Java 11 or later** to build this project (required by SpotBugs 4.9.x and
  Checkstyle 10.x)
- A **Java 21 runtime** is recommended to take advantage of the optimized
  implementations provided via the multi-release JAR (planned)
- **OpenJML 21.0.23 or later** in WSL (Ubuntu) — optional, required only for
  formal JML verification. Activate with `mvn verify -P openjml` or via the
  Maven panel in IntelliJ. Download from
  [github.com/OpenJML/OpenJML/releases](https://github.com/OpenJML/OpenJML/releases)

This library is packaged as a named module (`fr.dufrenoy.util`). To use it as
a dependency in a modular project, add the following to your `module-info.java`:
```java
requires fr.dufrenoy.util;
```

---

## Working on this project with Claude

This project is developed with Claude as a coding assistant. The `.dev/`
directory contains everything Claude needs to work consistently on the
codebase — and everything a human contributor needs to understand the
project's conventions and history.

### Project-specific choices

A few decisions in this setup are deliberate choices for this project, not
universal standards or conventions:

- **`Optional<T>` as return type** — used more broadly than the Java
  community consensus, which often restricts it to stream operations.
  Here it is the default for any method whose result may be absent,
  except where an interface contract forbids it.
- **Three test classes per tested class** (`FooTest` / `BlackBoxTest` /
  `WhiteBoxTest`) — a stricter separation than most projects use, chosen
  to keep interface contract tests, implementation contract tests, and
  internal structure tests fully independent.
- **`inverse()` returns a copy, not a live view** — unlike Guava's `BiMap`,
  by design. See `.dev/design/ARCHITECTURE.md` for the rationale.
- **Skills as Markdown files in `.dev/skills/`** — Claude reads these files
  on demand to follow consistent workflows. This is not a standard Claude
  convention; it is a lightweight adaptation of the Claude Code skill system
  to work within a Claude.ai Project.

### The `.dev/` directory

| File | Purpose |
|------|---------|
| `.dev/CONTEXT.md` | Entry point — read this first at the start of every session |
| `.dev/WORKFLOW.md` | Required order of operations for any class creation or modification |
| `.dev/standards/JAVA_STANDARDS.md` | Coding conventions for all Java in this project |
| `.dev/backlog/BACKLOG.md` | Pending tasks and known issues, updated each session |
| `.dev/design/ARCHITECTURE.md` | Key design decisions and rationale for each class |
| `.dev/design/INVARIANTS.md` | Structural invariants for all data structures |

### Skills

Skills are Markdown files that encode recurring workflows. Claude reads the
relevant skill before starting the corresponding task.

| Skill | File | When it runs |
|-------|------|-------------|
| Static analysis | `.dev/skills/static-analysis/SKILL.md` | After every class generation or significant modification |
| New class | `.dev/skills/new-class/SKILL.md` | When scaffolding a new data structure |
| Design review | `.dev/skills/design-review/SKILL.md` | Before implementing a new class or data structure |
| Test coverage review | `.dev/skills/test-coverage-review/SKILL.md` | After writing tests or when coverage seems insufficient |
| API consistency check | `.dev/skills/api-consistency-check/SKILL.md` | After adding public methods or before a release |
| Architecture drift check | `.dev/skills/architecture-drift-check/SKILL.md` | After significant refactoring or before a release |
| Update backlog | `.dev/skills/update-backlog/SKILL.md` | At the end of every session |
| Update README | `.dev/skills/update-readme/SKILL.md` | When the public API or roadmap changes, and at end of session |

### Starting a session

This project is developed with **Claude Code** (JetBrains plugin) as the
primary coding agent. Claude Code has direct access to the filesystem and
reads the `.dev/` context files automatically at the start of each session.

If working in a browser-based Claude session instead, ask Claude to read
`.dev/CONTEXT.md` first. This gives Claude the full picture: coding standards,
pending tasks, and design decisions.

### Triggering a skill explicitly

Skills trigger automatically when Claude recognises the context (e.g. end of
session, new class being created). You can also trigger any skill explicitly:

```
Run the static-analysis skill on SymmetricMap.
```

```
We're done for today — run update-backlog and update-readme.
```

---

## License

[LGPL v3](https://www.gnu.org/licenses/lgpl-3.0.html)