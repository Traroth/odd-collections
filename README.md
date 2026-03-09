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

- Full `java.util.List` implementation (`get`, `set`, `add`, `remove`, `indexOf`, `lastIndexOf`, `contains`, `clear`, ...)
- Configurable **chunk size** (default: 100)
- Pluggable **growing and shrinking strategies**, swappable at runtime
- Atomic **`setStrategies()`** to change both strategies simultaneously without risk of inconsistent intermediate state
- Native **`Spliterator`** for efficient `stream()` and `parallelStream()` support
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

## Roadmap

- **`SortedChunkyList`** — a `ChunkyList` that maintains elements in sorted order using a `Comparator`, with O(log n) insertion via binary search across chunks
- **`TreeList`** — a `List` backed by a red-black order-statistic tree, providing O(log n) access by index
- **`BiMap`** — a bijective map where values are as unique as keys, backed by a single array of collision chains with O(1) average lookup in both directions
- **`MultiMap`** — a multidimensional map with a configurable number of dimensions, supporting partial key lookups that return sub-maps

---

## Requirements

- Java 9 or later

---

## License

LGPL v3