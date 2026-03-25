# Architecture decisions — odd-collections

This document records the key design decisions made for each class in this
project, along with the alternatives that were considered and the reasons
they were rejected. It is intended to provide context for future contributors
and for AI coding agents working on the project.

---

## ChunkyList

### Interface + two implementations rather than an abstract class

`ChunkyList<E>` is defined as an interface, with two concrete implementations:
`UnsynchronizedChunkyList` and `SynchronizedChunkyList`.

**Alternative considered:** a single abstract class with a `synchronized`
flag, or a single class with optional locking.

**Reason for rejection:** separating the two implementations makes the
thread-safety contract explicit in the type system. A user who instantiates
`SynchronizedChunkyList` knows they are getting thread-safe behaviour; a user
who instantiates `UnsynchronizedChunkyList` knows they are not. A single class
with a flag would require runtime checks and obscure the contract.

---

### `ReentrantReadWriteLock` rather than `synchronized`

`SynchronizedChunkyList` uses a `ReentrantReadWriteLock` to protect all
operations.

**Alternative considered:** using `synchronized` methods or
`Collections.synchronizedList()`.

**Reason for rejection:** `ReentrantReadWriteLock` allows multiple concurrent
readers, which is a significant performance advantage for read-heavy workloads.
`synchronized` would serialize all operations, including reads, which is
unnecessarily restrictive. `Collections.synchronizedList()` uses a single
monitor and does not allow concurrent reads.

---

### Symmetric growing and shrinking strategies

Two strategy enums are provided: `GrowingStrategy` and `ShrinkingStrategy`.
The pairs `OVERFLOW_STRATEGY`/`UNDERFLOW_STRATEGY` and
`EXTEND_STRATEGY`/`DISAPPEAR_STRATEGY` are symmetric counterparts.

**Design rationale:** using a symmetric pair guarantees structural
reversibility — inserting and then removing the same element leaves the list
in its original chunk structure. This property is useful for predictable
memory behaviour and for testing.

**`setStrategies()`** sets both strategies atomically under a single lock in
`SynchronizedChunkyList`, preventing any operation from observing an
inconsistent intermediate state where the growing strategy has changed but the
shrinking strategy has not yet.

---

### `reorganize(boolean)` with blocking and non-blocking modes

`SynchronizedChunkyList` provides two overloads of `reorganize()`:
- `reorganize()` — holds the write lock for the full duration (safe default)
- `reorganize(false)` — uses a snapshot strategy: copy under read lock,
  reorganize without lock, swap under write lock

**Design rationale:** a full reorganization can be expensive on large lists.
The non-blocking mode allows reorganization to proceed without blocking readers,
at the cost of potentially losing modifications made between the snapshot and
the swap. This trade-off is documented explicitly in the Javadoc and left to
the caller to decide.

---

### Snapshot-based iterators in `SynchronizedChunkyList`

`iterator()`, `listIterator()`, `spliterator()`, and `stream()` in
`SynchronizedChunkyList` all operate on a snapshot of the list taken under a
read lock.

**Alternative considered:** fail-fast iterators protected by the read lock for
their entire duration.

**Reason for rejection:** holding a read lock for the entire duration of an
iteration would block all write operations for an unbounded time. Snapshot
iterators are fail-safe and allow writes to proceed concurrently, at the cost
of copying the list. This cost is documented in the Javadoc.

---

### Native `ChunkIterator` rather than delegation to `get(int)`

`UnsynchronizedChunkyList` provides a `ChunkIterator` that traverses the chunk
chain natively, maintaining a pointer to the current chunk and an index within
it. It overrides `iterator()` inherited from `AbstractList`.

**Alternative considered:** inheriting the default iterator from `AbstractList`,
which delegates to `get(int)`.

**Reason for rejection:** `AbstractList`'s default iterator calls `get(i)` on
each `next()` invocation, which traverses the chunk chain from the head each
time — O(n) per element, giving O(n²) for a full iteration. The native
`ChunkIterator` maintains a chunk pointer and advances in O(1) per element,
giving O(n) for a full iteration. Benchmarks confirmed an 8x improvement on
lists of 1000 elements with chunk size 100.

---

### Bidirectional traversal in `findChunk(int)`

`findChunk(int index)` is a private utility method used by `get`, `set`,
`add(int, E)`, and `remove(int)` to locate the chunk containing a given index.
It traverses from `firstChunk` if `index < size / 2`, or from `lastChunk`
backwards if `index >= size / 2`.

**Alternative considered:** always traversing from `firstChunk`.

**Reason for rejection:** always traversing from the front gives O(n/chunkSize)
average traversal cost. Bidirectional traversal halves the average cost to
O(n / (2 * chunkSize)) with no additional memory overhead.

---

### Optimized `addAll(Collection<? extends E> c)`

`addAll` converts the source collection to an array via `toArray()`, validates
all elements for `null`, then fills the last existing chunk and creates new
full chunks using `System.arraycopy` — one copy per chunk rather than one
`add` per element.

**Alternative considered:** inheriting `AbstractList.addAll`, which calls
`add(E)` for each element in a loop.

**Reason for rejection:** the inherited implementation incurs per-element
overhead: one `modCount++` increment, one null check, one potential
`handleFullChunk` call per element. The optimized implementation performs a
single null scan up front, a single `modCount++` at the end, and fills chunks
in bulk with `System.arraycopy`. Benchmarks show `ChunkyList.addAll` at 0.87 µs
vs `ArrayList.addAll` at 0.36 µs and `LinkedList.addAll` at 2.99 µs for 1000
elements — a 3.4x improvement over `LinkedList`.

---



`UnsynchronizedChunkyList` provides a `ChunkSpliterator` that traverses the
chunk chain natively, maintaining a pointer to the current chunk and an index
within it.

**Alternative considered:** inheriting the default `Spliterator` from
`AbstractList`, which delegates to `get(int)`.

**Reason for rejection:** `get(int)` on an unrolled linked list requires
traversing the chunk chain from the head on each call, giving O(n) access for
sequential iteration. The native `Spliterator` maintains a pointer to the
current chunk and advances in O(1) per element. `trySplit()` splits the chunk
range in half, enabling efficient parallel streams.

---

## SymmetricMap

### Interface + two implementations rather than a single class

`SymmetricMap<K, V>` is defined as an interface extending `Map<K, V>`, with
two concrete implementations: `UnsynchronizedSymmetricMap` and
`SynchronizedSymmetricMap`.

The interface declares the full symmetric contract:
- `getKey(Object value)` — reverse lookup
- `safePut(K key, V value)` — strict insertion
- `removeByValue(Object value)` — reverse removal
- `inverse()` — covariant return (see below)
- `values()` — overridden to return `Set<V>`

**Alternative considered:** a single class with an optional locking flag.

**Reason for rejection:** same rationale as `ChunkyList` — the thread-safety
contract should be explicit in the type system, not hidden behind a runtime
flag.

---

### Single bucket array with two collision chains rather than two maps

`UnsynchronizedSymmetricMap<K, V>` is backed by a single array of `Bucket`s.
Each `Bucket` holds two independent linked lists of `Entry` objects: one
chained by key hash (`firstByKey`), one chained by value hash
(`firstByValue`). Each `Entry` belongs to both chains simultaneously.

**Alternative considered:** two separate `HashMap`s — one mapping keys to
values, one mapping values to keys — kept in sync on every mutation.

**Reason for rejection:** two maps double the memory overhead and require
careful synchronization of every mutation to keep both maps consistent. The
single-array approach stores each entry exactly once while still providing O(1)
average lookup in both directions via `get()` and `getKey()`.

---

### `put` permissive + `safePut` strict

`put(K, V)` always overwrites — if the key or value already exists, the
conflicting entry is silently removed to maintain bijectivity.
`safePut(K, V)` throws `IllegalArgumentException` if the key or value already
exists.

**Alternative considered:** the Guava `BiMap` approach, where `put` throws on
value collision and `forcePut` overwrites.

**Reason for rejection:** a symmetric design is more consistent with the
bijective nature of the map. `put` is the permissive default (consistent with
`HashMap`), and `safePut` is the strict variant for callers who want to
guarantee the absence of collisions. The naming makes the intent explicit.

---

### `inverse()` — covariant return type, same thread-safety as the caller

`inverse()` is declared in the `SymmetricMap` interface with a return type of
`SymmetricMap<V, K>`. Each implementation overrides it with a covariant return:
- `UnsynchronizedSymmetricMap.inverse()` returns `UnsynchronizedSymmetricMap<V, K>`
- `SynchronizedSymmetricMap.inverse()` returns `SynchronizedSymmetricMap<V, K>`

In both cases, the returned map is an independent copy — modifications to the
inverse do not affect the original, and vice versa. In `SynchronizedSymmetricMap`,
the copy is taken under a read lock to guarantee a consistent snapshot.

**Alternative considered:** a live view, as Guava's `BiMap.inverse()` does.

**Reason for rejection:** a live view requires either a dedicated inverse class
or an `isInverse` flag that every method must check, significantly complicating
the implementation. Since `SymmetricMap` already provides O(1) lookup in both
directions via `get()` and `getKey()`, the primary use case for `inverse()` is
passing an inverted snapshot to a third-party API expecting a `Map<V, K>` — for
which an independent copy is sufficient.

---

### `values()` returns `Set<V>` rather than `Collection<V>`

`SymmetricMap.values()` overrides the `Map` contract by returning `Set<V>`
instead of `Collection<V>`.

**Design rationale:** in a bijective map, values are unique by definition —
they form a set, not merely a collection. Returning `Set<V>` expresses this
invariant in the type system and is consistent with Guava's `BiMap.values()`.
This is a valid covariant return type override in Java, since `Set` extends
`Collection`. The covariant return is declared in the `SymmetricMap` interface
and implemented in both concrete classes.

---

### `extends AbstractMap` in `UnsynchronizedSymmetricMap`

`UnsynchronizedSymmetricMap` extends `AbstractMap<K, V>`, which provides
correct implementations of `equals()`, `hashCode()`, `toString()`, and several
default methods based on `entrySet()`.

Methods where our implementation is more efficient than `AbstractMap`'s default
(which iterates over `entrySet()`) are overridden: `containsKey()`,
`containsValue()`, `get()`, `remove()`, `put()`, `keySet()`, `values()`,
`entrySet()`.

Methods that use `setValue()` internally (`replace()`, `replaceAll()`,
`merge()`, `compute()`, `computeIfPresent()`) previously required manual
overrides to use `put()` and `remove()` instead. Since `Entry.setValue()` now
delegates to `put()` and maintains bijectivity, these overrides are no longer
necessary for correctness, but are kept for clarity and to avoid relying on
`AbstractMap`'s internal behaviour.

`SynchronizedSymmetricMap` does not extend `AbstractMap` — it delegates all
operations to its inner `UnsynchronizedSymmetricMap` and protects them with a
`ReentrantReadWriteLock`.

---

### `ReentrantReadWriteLock` in `SynchronizedSymmetricMap`

`SynchronizedSymmetricMap` uses a `ReentrantReadWriteLock` to protect all
operations, following the same pattern as `SynchronizedChunkyList`.

**Design rationale:** multiple concurrent readers are allowed; writes are
exclusive. This is a significant performance advantage for read-heavy workloads
over a single `synchronized` monitor.

---

### `replaceAll()` is fully atomic under write lock in `SynchronizedSymmetricMap`

Unlike `reorganize()` in `SynchronizedChunkyList`, which offers a non-blocking
snapshot mode, `replaceAll()` in `SynchronizedSymmetricMap` holds the write
lock for its entire duration.

**Reason:** `replaceAll()` on a bijective map is inherently sensitive — the
remapping function may produce duplicate values, requiring conflicting entries
to be removed. Allowing reads or writes to interleave during this operation
could expose a transiently inconsistent state. The simpler, safer choice is to
hold the write lock throughout.

---

### Snapshot-based iterators in `SynchronizedSymmetricMap`

`iterator()` on `keySet()`, `values()`, and `entrySet()` in
`SynchronizedSymmetricMap` operates on a snapshot taken under a read lock.

**Alternative considered:** fail-fast iterators protected by the read lock for
their entire duration.

**Reason for rejection:** same rationale as `SynchronizedChunkyList` — holding
a read lock for the full duration of an iteration blocks all writes for an
unbounded time. Snapshot iterators allow writes to proceed concurrently, at
the cost of copying the entry set. This cost is documented in the Javadoc.

---

### `Entry` holds no reference to its map — `setValue()` delegated to `EntrySetView`

`UnsynchronizedSymmetricMap.Entry` does not store a reference to its enclosing
map. `setValue()` is instead implemented in the anonymous `Map.Entry` wrapper
returned by `EntrySetView.iterator()`, which accesses the map via
`UnsynchronizedSymmetricMap.this.put()`.

**Alternative considered:** storing a `final UnsynchronizedSymmetricMap<K, V> map`
reference in each `Entry`, as is common in `HashMap`-style implementations.

**Reason for rejection:** every `Entry` would carry an extra reference,
increasing memory overhead proportionally to the number of entries. Since
`setValue()` is only meaningful when an entry is obtained through `entrySet()`,
delegating it to the `EntrySetView` iterator is both more memory-efficient and
more consistent with the JDK's own `HashMap` design, where `Node` does not
reference its map.