# ChunkyList

A Java implementation of an **unrolled linked list** — a hybrid data structure that combines the memory locality of arrays with the dynamic resizing of linked lists.

---

## How it works

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

---

## Architecture

The library is organized around a central interface and two implementations:

| Type | Role |
|---|---|
| `ChunkyList<E>` | Interface extending `java.util.List`, exposing chunk-specific configuration |
| `UnsynchronizedChunkyList<E>` | Standard implementation — not thread-safe, fail-fast iterators |
| `SynchronizedChunkyList<E>` | Thread-safe implementation backed by a `ReentrantReadWriteLock` |

---

## Features

- Full `java.util.List` implementation (`get`, `set`, `add`, `remove`, `indexOf`, `lastIndexOf`, `contains`, `clear`, ...)
- Configurable **chunk size** (default: 100)
- Pluggable **growing and shrinking strategies**, swappable at runtime
- Atomic **`setStrategies()`** to change both strategies simultaneously without risk of inconsistent intermediate state
- Native **`Spliterator`** for efficient `stream()` and `parallelStream()` support
- Fail-fast iterators (`UnsynchronizedChunkyList`) and fail-safe snapshot iterators (`SynchronizedChunkyList`)
- Copy constructors and collection constructors
- `reorganize()` to compact sparsely filled chunks

---

## Strategies

### GrowingStrategy

Controls what happens when an element is inserted into a full chunk:

| Strategy | Behaviour |
|---|---|
| `OVERFLOW_STRATEGY` *(default)* | The overflowing element is pushed into the next chunk (created if necessary) |
| `EXTEND_STRATEGY` | A new chunk is created after the current one to hold the overflowing element |

### ShrinkingStrategy

Controls what happens after an element is removed from a chunk:

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

---

## Usage

### Basic usage

```java
ChunkyList<String> list = new UnsynchronizedChunkyList<>();
list.add("Hello");
list.add("World");
System.out.println(list.get(0)); // Hello
```

### Custom chunk size

```java
ChunkyList<Integer> list = new UnsynchronizedChunkyList<>(50);
```

### From an existing collection

```java
List<String> source = List.of("a", "b", "c");
ChunkyList<String> list = new UnsynchronizedChunkyList<>(source);

// With a custom chunk size
ChunkyList<String> list2 = new UnsynchronizedChunkyList<>(10, source);
```

### Copy constructors

```java
// Faithful copy (preserves chunk size and strategies)
UnsynchronizedChunkyList<String> copy = new UnsynchronizedChunkyList<>(original);

// Copy with a different chunk size (chunks that fit are preserved as-is;
// chunks exceeding the new size are split using the GrowingStrategy)
UnsynchronizedChunkyList<String> resized = new UnsynchronizedChunkyList<>(25, original);
```

### Streams

```java
// Sequential
list.stream()
    .filter(s -> s.startsWith("A"))
    .forEach(System.out::println);

// Parallel (uses native Spliterator that splits by chunk)
list.parallelStream()
    .map(String::toUpperCase)
    .collect(Collectors.toList());
```

### Reorganize

After many removals, chunks may become sparsely filled. `reorganize()` redistributes all elements into chunks of `chunkSize` elements (except possibly the last one), without changing their order:

```java
list.reorganize();
```

---

## Thread safety

`UnsynchronizedChunkyList` is **not thread-safe**. If multiple threads access an instance concurrently and at least one modifies it structurally, it must be synchronized externally:

```java
List<String> safeList = Collections.synchronizedList(new UnsynchronizedChunkyList<>());
```

For better performance, use `SynchronizedChunkyList`, which is backed by a `ReentrantReadWriteLock`: multiple threads may read concurrently, while writes are exclusive.

```java
ChunkyList<String> list = new SynchronizedChunkyList<>();
list.add("Hello");
list.add("World");
```

### Iterators and streams

Iterators, list iterators, spliterators, and streams on a `SynchronizedChunkyList` operate on a **snapshot** of the list taken at the time of the call. Subsequent modifications are not reflected in the snapshot.

> **Memory note:** snapshot-based operations copy the entire list. Avoid calling them on very large lists in memory-constrained environments.

### reorganize()

`SynchronizedChunkyList` provides two variants of `reorganize()`:

```java
// Blocking (default): holds the write lock for the full duration
list.reorganize();
list.reorganize(true);

// Non-blocking: takes a snapshot, reorganizes without a lock,
// then swaps the result in under a write lock.
// Warning: modifications made between the snapshot and the swap are silently lost.
list.reorganize(false);
```

---

## Requirements

- Java 9 or later

---

## License

LGPL v3