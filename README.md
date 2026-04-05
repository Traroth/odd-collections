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

#### Performance

JMH benchmarks comparing `UnsynchronizedChunkyList` against `ArrayList` and `LinkedList` at 100,000 elements, with the recommended configuration (`EXTEND_STRATEGY` / `DISAPPEAR_STRATEGY`, chunk size 500):

| Operation | ArrayList | ChunkyList | LinkedList |
|---|---|---|---|
| `add(E)` at end | 0,030 µs | **0,013 µs** | 0,16 µs |
| `add(int, E)` at middle | 8,8 µs | **1,4 µs** | 97 µs |
| `get(int)` | **0,002 µs** | 0,35 µs | 103 µs |
| `remove(int)` at middle | 3,6 µs | **0,42 µs** | 109 µs |
| iterate | **121 µs** | 186 µs | 256 µs |
| `addAll` (100k elements) | **43 µs** | 83 µs | 288 µs |
| `parallelStream` | **30 µs** | 75 µs | 407 µs |

Full benchmark results are available in `.dev/benchmarks/`.

**When to use ChunkyList**

- `add` at end, middle, or `remove` at middle are the dominant operations
- The list is large (> 10,000 elements) and frequently modified
- Parallel stream processing matters
- Random access by index is infrequent

**When to prefer ArrayList**

- `get(int)` is the dominant operation
- `addAll` is called frequently with large collections
- The list is small (< 1,000 elements)

> `ChunkyList` outperforms `LinkedList` on virtually every operation at every size — `LinkedList` is rarely the right choice.

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

### TreeList

A **sorted list with no duplicates** backed by a **red-black tree augmented with subtree sizes** (order-statistic tree). Each node stores the size of its subtree, enabling O(log n) index-based access — something a plain `TreeMap` cannot provide.

#### How it works

Elements are ordered by their natural ordering or by a `Comparator` supplied at construction time. Two elements are considered duplicates if `compare(a, b) == 0`; duplicate insertions are silently rejected.

```
           5 (BLACK, size=7)
          / \
      3 (RED)   8 (RED)
     / \        / \
    1   4      7   9
```

Unlike `ArrayList`, insertion and removal are O(log n) with no element shifting. Unlike a `TreeMap` wrapper, the augmented subtree sizes keep `get(int)` at O(log n) rather than O(n).

#### Architecture

| Type | Role |
|---|---|
| `TreeList<E>` | Interface extending `java.util.List`, declaring the `comparator()` method |
| `UnsynchronizedTreeList<E>` | Standard implementation backed by an order-statistic red-black tree — not thread-safe |
| `SynchronizedTreeList<E>` | Thread-safe implementation backed by a `ReentrantReadWriteLock` |

#### Features

- Full `java.util.List` implementation (`get`, `add`, `remove`, `contains`, `indexOf`, `removeAll`, `retainAll`, `addAll`, `clear`, ...)
- **Sorted order** always maintained — elements are kept in ascending order by natural ordering or by a provided `Comparator`
- **No duplicates** — `add(E)` returns `false` if the element is already present, leaving the list unchanged
- **O(log n)** for `add`, `remove(int)`, `remove(Object)`, `contains`, `indexOf`, and `get` — all operations exploit the tree structure
- **O(n)** full iteration via a native in-order traversal — avoids the O(n log n) cost of iterating via repeated `get(int)`
- `comparator()` returns the comparator in use, or an empty `Optional` for natural ordering
- Collection constructors (with or without a `Comparator`), with duplicate elements silently discarded
- Snapshot-based iterators in `SynchronizedTreeList`
- Positional insertion (`add(int, E)`), replacement (`set`), and `subList` are not supported and throw `UnsupportedOperationException`

#### Usage

```java
// Natural ordering
TreeList<Integer> list = new UnsynchronizedTreeList<>();
list.add(5);
list.add(2);
list.add(8);
System.out.println(list.get(0)); // 2 — always sorted
System.out.println(list.add(5)); // false — duplicate rejected
System.out.println(list.size()); // 3

// Index-based access in O(log n)
int third = list.get(2); // 8

// Custom comparator (reverse order)
TreeList<String> desc = new UnsynchronizedTreeList<>(Comparator.reverseOrder());
desc.add("banana");
desc.add("apple");
desc.add("cherry");
System.out.println(desc.get(0)); // cherry

// From an existing collection — duplicates discarded
List<Integer> source = List.of(3, 1, 4, 1, 5, 9);
TreeList<Integer> list = new UnsynchronizedTreeList<>(source);
System.out.println(list.size()); // 5 (duplicate 1 discarded)

// Thread-safe
TreeList<Integer> list = new SynchronizedTreeList<>();
```

#### Thread safety

`UnsynchronizedTreeList` is **not thread-safe**. For concurrent access, use `SynchronizedTreeList`, which is backed by a `ReentrantReadWriteLock`: multiple threads may read concurrently, while writes are exclusive.

`iterator()`, `listIterator()`, and `listIterator(int)` on a `SynchronizedTreeList` return **snapshot-based iterators**: a copy of the list is taken under a read lock at the time the iterator is created. Subsequent modifications to the list are not reflected in the iterator, and the iterator never throws `ConcurrentModificationException`.

> **Memory note:** snapshot-based operations copy the entire list. Avoid calling them on very large lists in memory-constrained environments.

#### Development note

`TreeList` is the first structure in this project developed **JML-first**: class invariants and method pre/post-conditions were expressed as JML `@invariant`, `@requires`, and `@ensures` annotations before any implementation was written. This made the contract explicit and unambiguous at design time, reduced back-and-forth during implementation, and produced a more structurally reliable result than the earlier classes — which received JML annotations retroactively.

---

### MultiMap

A **recursive multi-dimensional map**. Each `MultiMap<K, V>` associates keys of type `K` to values of type `V`, where `V` may itself be another `MultiMap`, enabling multi-level key hierarchies with heterogeneous key types per dimension.

#### How it works

A partial lookup (stopping before the deepest level) returns a sub-map of reduced dimensionality. A complete lookup returns the terminal value.

```
MultiMap<Country, MultiMap<City, MultiMap<Quarter, Integer>>>

  "France" ──▶ { "Paris" ──▶ { "Q1" ──▶ 42, "Q2" ──▶ 73 },
                 "Lyon"  ──▶ { "Q1" ──▶ 10 } }
```

The structure is backed by a `HashMap` at each level — no custom hash table is needed since there are no special structural constraints (unlike `SymmetricMap`'s dual hash chains).

#### Architecture

| Type | Role |
|---|---|
| `MultiMap<K, V>` | Interface — custom (does not extend `java.util.Map`), but mirrors the `Map` API where applicable |
| `UnsynchronizedMultiMap<K, V>` | Standard implementation — not thread-safe, fail-fast iterators |
| `SynchronizedMultiMap<K, V>` | Thread-safe implementation backed by a `ReentrantReadWriteLock` |

#### Features

- **Chained writes** via `getOrCreate(K, Supplier<V>)` — creates intermediate levels on the fly
- **Chained reads** via `get(K)` — returns `null` if the key is absent, enabling concise multi-level lookups
- **Safe reads** via `getOpt(K)` — returns `Optional<V>` for explicit absent-key handling with `flatMap`
- `put`, `remove`, `containsKey`, `size`, `isEmpty`, `clear`
- View methods: `keySet()`, `values()`, `entrySet()` (using `Map.Entry`)
- Null keys and null values are forbidden (`NullPointerException`)
- Fail-fast iterators (`UnsynchronizedMultiMap`) and snapshot-based iterators (`SynchronizedMultiMap`)
- Copy constructors from `Map` and `MultiMap`

#### Nullable `get` — exception to the project Optional convention

The project convention (`JAVA_STANDARDS.md` §5) prescribes `Optional<T>` for any return value that may be absent. `MultiMap` deliberately deviates from this for `get(K)`, `put(K, V)`, and `remove(K)`. The reason: chained multi-level lookups would be impractical with `Optional` at every level.

```java
// Nullable get — concise chaining for the common case
map.get("France").get("Paris").get("Q1")

// Optional getOpt — safe alternative when keys may be absent
map.getOpt("France")
   .flatMap(m -> m.getOpt("Paris"))
   .flatMap(m -> m.getOpt("Q1"))
```

#### Usage

```java
// Chained write — creates intermediate levels automatically
MultiMap<String, MultiMap<String, MultiMap<String, Integer>>> map =
    new UnsynchronizedMultiMap<>();
map.getOrCreate("France", UnsynchronizedMultiMap::new)
   .getOrCreate("Paris", UnsynchronizedMultiMap::new)
   .put("Q1", 42);

// Chained read
Integer val = map.get("France").get("Paris").get("Q1"); // 42

// Safe read
Optional<Integer> safe = map.getOpt("France")
    .flatMap(m -> m.getOpt("Paris"))
    .flatMap(m -> m.getOpt("Q1"));

// Partial lookup — returns a sub-map
MultiMap<String, MultiMap<String, Integer>> france = map.get("France");

// Thread-safe
MultiMap<String, MultiMap<String, Integer>> map = new SynchronizedMultiMap<>();
```

#### Thread safety

`UnsynchronizedMultiMap` is **not thread-safe**. For concurrent access, use `SynchronizedMultiMap`, which is backed by a `ReentrantReadWriteLock`: multiple threads may read concurrently, while writes are exclusive.

Iterators on `keySet()`, `values()`, and `entrySet()` in `SynchronizedMultiMap` operate on a **snapshot** of the map taken at the time of the call.

> **Atomicity note:** each operation is atomic at the current level only. In a recursive multi-level structure, operations on sub-maps are independent — there is no cross-level locking. Callers requiring atomicity across multiple levels must provide their own synchronization.

#### Development note

`MultiMap` is the second structure in this project developed **JML-first**: class invariants and method contracts were written before implementation.

---

## Roadmap

- **`Tuple`** — a type-safe immutable tuple system without one class per arity, with Java 21 record-based implementation via multi-release JAR
- **`CircularBuffer`** — a circular doubly-linked list backed by a ring of nodes
- **`SortedChunkyList`** — a `ChunkyList` that maintains elements in sorted order using a `Comparator`, with O(log n) insertion via binary search across chunks

---

## Requirements

- **Java 11 or later** to use this library — Java 11 is the compilation target
- **Java 11 or later** to build this project (also required by SpotBugs 4.9.x
  and Checkstyle 10.x)
- A **Java 21 runtime** is recommended to take advantage of the optimized
  implementations provided via the multi-release JAR (planned)
- **OpenJML 21.0.23 or later** — optional, required only for formal JML
  verification. Set `openjml.home` in `~/.m2/settings.xml` to the directory
  containing the `openjml` binary, then activate the appropriate profile:
  - **Linux or macOS**: `mvn verify -P openjml-unix`
  - **Windows** (via WSL): `mvn verify -P openjml-windows`

  Download from
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

Beyond being a collection of data structures, this project is intended as a
**practical example of reliable AI-assisted development**. The workflow,
tooling, and conventions documented here address a recurring challenge: LLMs
generate code quickly, but without discipline, they also introduce subtle bugs,
drift from the original design, and produce tests that are superficially broad
but structurally shallow.

The approach used here rests on four layers of rigour:

1. **Design before code** — invariants, complexity guarantees, and interface
   contracts are agreed upon before any implementation is written. This gives
   the model a precise target rather than an open-ended task.

2. **JML contracts as a specification layer** — class invariants and
   method pre/post-conditions are expressed in JML (`@invariant`, `@requires`,
   `@ensures`) directly in the source. Starting from `TreeList`, these are
   written *before* the implementation, making the contract machine-verifiable
   and leaving no room for implicit assumptions to slip through.

3. **Three-level testing** — interface contract tests (`FooTest`), black-box
   tests (`FooBlackBoxTest`), and white-box tests (`FooWhiteBoxTest`) are kept
   strictly separate. The first two are written before the implementation (TDD),
   the third after — targeting internal risks that the public API cannot expose.

4. **Systematic static analysis** — after every significant generation, Claude
   reviews the code for logic bugs, contract violations, invariant preservation,
   null safety, and complexity guarantees, before automated tools (Checkstyle,
   SpotBugs, JaCoCo) run.

Each layer catches a different class of defect. Together they make AI-generated
code auditable, reproducible, and maintainable — not just initially correct.

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