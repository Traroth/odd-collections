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

### Native `Spliterator` rather than delegation to `get(int)`

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

### Single bucket array with two collision chains rather than two maps

`SymmetricMap<K, V>` is backed by a single array of `Bucket`s. Each `Bucket`
holds two independent linked lists of `Entry` objects: one chained by key hash
(`firstByKey`), one chained by value hash (`firstByValue`). Each `Entry`
belongs to both chains simultaneously.

**Alternative considered:** two separate `HashMap`s — one mapping keys to
values, one mapping values to keys — kept in sync on every mutation.

**Reason for rejection:** two maps double the memory overhead and require
careful synchronization of every mutation to keep both maps consistent. The
single-array approach stores each entry exactly once while still providing O(1)
average lookup in both directions.

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

### `inverse()` returns an independent copy rather than a live view

`inverse()` returns a new `SymmetricMap<V, K>` with keys and values swapped,
independent of the original map.

**Alternative considered:** a live view, as Guava's `BiMap.inverse()` does,
where modifications to the view are reflected in the original and vice versa.

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
`Collection`.

---

### `extends AbstractMap` rather than `implements Map` directly

`SymmetricMap` extends `AbstractMap<K, V>`, which provides correct
implementations of `equals()`, `hashCode()`, `toString()`, and several default
methods based on `entrySet()`.

Methods where our implementation is more efficient than `AbstractMap`'s default
(which iterates over `entrySet()`) are overridden: `containsKey()`,
`containsValue()`, `get()`, `remove()`, `put()`, `keySet()`, `values()`,
`entrySet()`.

Methods that use `setValue()` internally (`replace()`, `replaceAll()`,
`merge()`, `compute()`, `computeIfPresent()`) are overridden to use `put()`
and `remove()` instead, since `Entry.setValue()` is not supported (it would
bypass the bijectivity invariant). Implementing `setValue()` correctly is
tracked in the backlog.