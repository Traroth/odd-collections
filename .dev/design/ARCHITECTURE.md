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
in bulk with `System.arraycopy`. Benchmarks show `ChunkyList.addAll` at
~84 µs vs `ArrayList.addAll` at ~43 µs and `LinkedList.addAll` at ~295 µs
for 100,000 elements with chunk size 500.

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

### Performance profile and recommended configuration

JMH benchmarks comparing `UnsynchronizedChunkyList` against `ArrayList` and
`LinkedList` across sizes from 100 to 100,000 elements and chunk sizes from
10 to 500 reveal the following performance profile.

**Strengths**

- `addAtEnd` — consistently faster than `ArrayList` at all sizes and chunk
  sizes. With chunk size 500: ~0.013 µs vs ~0.030 µs for `ArrayList`.
- `addAtMiddle` with `EXTEND_STRATEGY` — dramatically faster than
  `OVERFLOW_STRATEGY` (which cascades overflow across chunks). With chunk size
  500 and 100,000 elements: 1.35 µs vs 8.8 µs for `ArrayList` and 100 µs for
  `LinkedList`.
- `removeAtMiddle` with `DISAPPEAR_STRATEGY` + large chunk size — with chunk
  size 500 and 100,000 elements: 0.42 µs, beating `ArrayList` (3.6 µs) and
  crushing `LinkedList` (109 µs).
- `parallelStream` — significantly better than `LinkedList` due to natural
  chunk-based splitting in `trySplit()`.

**Weaknesses**

- `getByIndex` — structurally O(n/chunkSize); `ArrayList` is always faster.
  With chunk size 100 and 100,000 elements: 2.54 µs vs 0.002 µs for
  `ArrayList`. Chunk size 500 reduces this to 0.35 µs but `ArrayList` remains
  orders of magnitude faster.
- `addAll` — approximately 2x slower than `ArrayList` due to chunk allocation
  overhead, though 3.5x faster than `LinkedList`.
- `iterate` and `stream` — between `ArrayList` and `LinkedList`.

**Recommended configuration**

For optimal performance on large lists with frequent insertions and removals:
- Strategy: `EXTEND_STRATEGY` / `DISAPPEAR_STRATEGY`
- Chunk size: ≥ 100 for lists > 10,000 elements; 500 for lists > 50,000 elements

**Use ChunkyList when:**
- The dominant operations are `addAtEnd`, `addAtMiddle`, or `removeAtMiddle`
- Random access by index (`get`) is infrequent
- Parallel stream processing is important

**Prefer ArrayList when:**
- Random access by index is the dominant operation
- `addAll` is called frequently with large collections

**Prefer LinkedList over neither** — `ChunkyList` outperforms `LinkedList` on
virtually every operation at every size.

---

## TreeList

### Interface + two implementations rather than a single class

`TreeList<E>` is defined as an interface extending `List<E>`, with two concrete
implementations: `UnsynchronizedTreeList` and `SynchronizedTreeList`.

**Alternative considered:** a single class with an optional locking flag.

**Reason for rejection:** same rationale as `ChunkyList` and `SymmetricMap` —
the thread-safety contract should be explicit in the type system, not hidden
behind a runtime flag.

---

### Red-black tree augmented with subtree sizes (order-statistic tree)

`UnsynchronizedTreeList` is backed by a red-black tree where each node stores
the size of its subtree (`subtreeSize`). This augmentation allows index-based
operations (`get(int)`, `remove(int)`) to run in O(log n) by computing the
rank of a node from the subtree sizes of its children.

**Alternative considered:** a plain red-black tree without augmentation.

**Reason for rejection:** without subtree sizes, `get(int)` requires a full
in-order traversal — O(n). The augmentation adds one integer per node and
requires updating `subtreeSize` on every rotation and structural change, but
keeps all operations at O(log n).

**Alternative considered:** an `ArrayList` kept sorted via `Collections.sort()`
or binary search insertion.

**Reason for rejection:** insertion and removal in an `ArrayList` are O(n) due
to element shifting. The red-black tree gives O(log n) for all structural
operations.

**Alternative considered:** a `TreeMap<E, Void>` wrapper.

**Reason for rejection:** `TreeMap` provides no index-based access. `get(int)`
would require a full traversal — O(n). The augmented tree is the only approach
that achieves O(log n) for both sorted lookup and indexed access.

---

### `add(int, E)`, `set(int, E)`, and `subList(int, int)` throw `UnsupportedOperationException`

The position of each element is determined by its sort order, not by the
caller. Allowing positional insertion or replacement would silently break the
sorted invariant.

**Alternative considered:** ignoring the index and inserting in sorted position.

**Reason for rejection:** silently ignoring a parameter violates the principle
of least surprise and the `List` contract, which states that `add(int, E)`
inserts at the specified position.

`subList(int, int)` is not supported in the current version. A live view over
a tree-backed structure requires careful bookkeeping of structural modifications
and is deferred to a future implementation (see `BACKLOG.md`).

---

### No duplicates — `add(E)` returns `false` for existing elements

Two elements are considered duplicates if `compare(a, b) == 0`. Duplicate
insertions are silently rejected: `add(E)` returns `false` without modifying
the list.

This deviates from the `List.add(E)` contract, which always returns `true`,
but is consistent with the `Collection.add(E)` specification: "returns `true`
if this collection changed as a result of the call."

---

### `removeAll()` and `retainAll()` explicitly overridden in `UnsynchronizedTreeList`

`AbstractCollection.removeAll()` and `retainAll()` iterate via
`iterator().remove()`. Since the `TreeList` iterator does not support `remove()`
(modifications must go through the tree to preserve the red-black invariants),
both methods are overridden to call `remove(Object)` directly.

---

### `equals()` and `hashCode()` explicitly overridden in `SynchronizedTreeList`

`SynchronizedTreeList` does not extend `AbstractList`, so `Object.equals()`
(reference equality) would apply by default, violating the `List` contract.
Both methods are explicitly overridden to delegate to the inner
`UnsynchronizedTreeList` under a read lock.

---

### `iterator()` performs an in-order traversal of the red-black tree

The iterator maintains a pointer to the current node and advances to the
in-order successor on each `next()` call — O(1) per step, O(n) for a full
iteration. It does not support `remove()`.

**Alternative considered:** inheriting the default iterator from `AbstractList`,
which delegates to `get(int)`.

**Reason for rejection:** `get(int)` on an augmented tree is O(log n), giving
O(n log n) for a full iteration. The native in-order traversal is O(n).

---

### Snapshot-based iterators in `SynchronizedTreeList`

`iterator()`, `listIterator()`, and `listIterator(int)` in `SynchronizedTreeList`
operate on a snapshot of the list taken under a read lock.

**Alternative considered:** fail-fast iterators protected by the read lock for
their entire duration.

**Reason for rejection:** same rationale as `SynchronizedChunkyList` and
`SynchronizedSymmetricMap` — holding a read lock for the full duration of an
iteration blocks all writes for an unbounded time.

---

### `ReentrantReadWriteLock` in `SynchronizedTreeList`

`SynchronizedTreeList` uses a `ReentrantReadWriteLock` to protect all
operations, following the same pattern as `SynchronizedChunkyList` and
`SynchronizedSymmetricMap`.

**Design rationale:** multiple concurrent readers are allowed; writes are
exclusive. This is a significant performance advantage for read-heavy workloads
over a single `synchronized` monitor.

---

### `subList(int, int)` — live view bounded by element values

`UnsynchronizedTreeList.subList(fromIndex, toIndex)` returns a `SubList`, a
private inner class that implements `TreeList<E>` and extends `AbstractList<E>`.
The return type is covariant: `TreeList<E>` instead of `List<E>`.

The view is bounded by **element values**, not integer offsets. At creation time,
the elements at positions `fromIndex` and `toIndex` are captured as `fromElement`
(inclusive) and `toElement` (exclusive). All subsequent operations are defined in
terms of "elements in the range `[fromElement, toElement)`".

**Alternative considered:** index-bounded view (like `AbstractList.SubList`),
tracking `offset` and `size`.

**Reason for rejection:** an index-bounded view is invalidated by any structural
modification to the parent — even mutations within the view's logical range —
because indices shift. This forces the view to throw
`ConcurrentModificationException` on every external mutation, which is overly
restrictive for a sorted list where `add(E)` may insert anywhere. A
value-bounded view tolerates external mutations naturally: the range
`[fromElement, toElement)` is well-defined regardless of index shifts.

---

### `add(E)` on `SubList` — range-checked, following `TreeMap.subMap` convention

`SubList.add(E)` delegates to the parent's `add(E)` if the element falls within
`[fromElement, toElement)`, and throws `IllegalArgumentException` otherwise.

**Design rationale:** this follows the `SortedMap.subMap()` convention, where
`put` throws `IllegalArgumentException` for out-of-range keys. The alternative —
silently ignoring out-of-range adds — would violate the principle of least
surprise.

---

### Covariant return type `TreeList<E>` for `subList`

The `TreeList` interface declares `subList` with return type `TreeList<E>`
instead of `List<E>`. The sublist of a sorted, duplicate-free list is itself
sorted and duplicate-free — it satisfies the `TreeList` contract by Liskov
Substitution Principle.

**Alternative considered:** returning `List<E>`.

**Reason for rejection:** `List<E>` loses the information that the sublist is
sorted and duplicate-free. `TreeList<E>` gives the caller access to
`comparator()` and the type guarantee that the list is sorted. The covariant
return type is valid in Java (since Java 5).

---

### Fail-fast via `modCount` in `SubList`

`SubList` stores `expectedModCount` at creation time. Each access checks
`modCount == expectedModCount`. Mutations through the `SubList` (add, remove,
clear) delegate to the parent, then update `expectedModCount`. External
mutations to the parent will cause the next `SubList` operation to throw
`ConcurrentModificationException`.

**Design rationale:** same fail-fast pattern as `AbstractList.SubList` and
`TreeListIterator`. Lightweight, no per-mutation overhead beyond a single
integer comparison.

---

### Nested `subList` — flat, not chained

`SubList.subList(fromIndex, toIndex)` creates a new `SubList` with narrowed
value bounds, directly backed by the root `UnsynchronizedTreeList`. It does
not create a SubList-on-SubList chain.

**Design rationale:** chaining would add O(depth) overhead per operation, as
each call would traverse the SubList chain. The flat approach ensures every
SubList operation is O(log n) relative to the parent tree, regardless of
nesting depth.

---

### `removeAll` and `retainAll` explicitly overridden in `SubList`

`AbstractList.removeAll()` and `retainAll()` iterate via `iterator().remove()`.
Since the `SubList` iterator does not support `remove()`, both methods are
overridden to iterate over the range and call `remove(Object)` directly on
the parent.

**Design rationale:** same situation and same solution as
`UnsynchronizedTreeList.removeAll()` / `retainAll()`.

---

### Snapshot-based `subList` in `SynchronizedTreeList`

`SynchronizedTreeList.subList(fromIndex, toIndex)` returns a snapshot: the
elements in the range are copied into an independent `UnsynchronizedTreeList`
under a read lock.

**Alternative considered:** a live view wrapping the parent's `SubList` with
synchronized access.

**Reason for rejection:** a live view would require the returned `TreeList` to
hold a reference to the parent's `ReentrantReadWriteLock` and acquire it on
every operation — "lock leaking". The caller could hold references across lock
boundaries, leading to subtle concurrency bugs. The snapshot approach is safe,
simple, and consistent with how `SynchronizedTreeList` handles iterators.

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

---

## MultiMap

### Custom interface rather than extending `java.util.Map`

`MultiMap<K, V>` is defined as a standalone interface, not an extension of
`java.util.Map<K, V>`. The API intentionally mirrors `Map` where applicable
(method names, signatures, use of `Map.Entry`).

**Alternative considered:** extending `Map<K, V>` and adding the recursive
methods (`getOrCreate`, `getOpt`).

**Reason for rejection:** the `Map` contract is incompatible with the recursive
semantics. `getOrCreate` has no equivalent in `Map`. Partial lookups (returning
a sub-map of reduced dimensionality) are not part of the `Map` contract. Methods
like `containsValue` have no clear semantics in a recursive context — should they
search the current level only, or recursively? Inheriting `Map` would force
answers to these questions that are better left out of scope.

---

### Recursive generic typing rather than arity-specific classes

The multi-dimensional structure is expressed via recursive generics:
`MultiMap<K, V>` where `V` may itself be another `MultiMap`. A three-level
map is typed as `MultiMap<K1, MultiMap<K2, MultiMap<K3, V>>>`.

**Alternative considered:** dedicated classes per arity —
`MultiMap2<K1, K2, V>`, `MultiMap3<K1, K2, K3, V>`, etc.

**Reason for rejection:** combinatorial explosion (one class per arity),
incompatible with dynamic depth. The recursive approach naturally handles
variable depth, heterogeneous key types per level, and partial lookups that
return a sub-map of reduced dimensionality.

---

### Delegation to `HashMap` rather than a custom hash table

`UnsynchronizedMultiMap` is backed by a `HashMap<K, V>`. All storage,
hashing, and resizing are delegated to the standard `HashMap`.

**Alternative considered:** a custom hash table, as `UnsynchronizedSymmetricMap`
uses.

**Reason for rejection:** `MultiMap` has no structural constraint that requires
a custom table (no bijectivity, no dual hash chains). `HashMap` provides the
necessary performance characteristics without additional complexity. The
delegation pattern keeps the implementation simple and benefits from ongoing
JDK optimizations.

---

### `get(K)` returns nullable — deliberate exception to the project Optional convention

`get(K)` returns `V` (nullable), while `getOpt(K)` returns `Optional<V>`.
`put(K, V)` and `remove(K)` also return the previous value as nullable `V`.

The project convention (`JAVA_STANDARDS.md` §5) prescribes `Optional<T>` for
any return value that may be absent. `MultiMap` deliberately deviates from this
convention for `get(K)`, `put(K, V)`, and `remove(K)`.

**Reason:** the primary use case for `MultiMap` is chained multi-level lookups:
`map.get("a").get("b").get("c")`. Using `Optional` exclusively would require
`flatMap` at every level, making the common case verbose:

```java
// With Optional only — verbose for the common case
map.getOpt("a").flatMap(m -> m.getOpt("b")).flatMap(m -> m.getOpt("c"))

// With nullable get — concise chaining
map.get("a").get("b").get("c")
```

`getOpt(K)` is provided as the safe alternative for cases where a key may be
absent and the caller wants to handle it explicitly. This dual API mirrors the
JDK's own `Map.get()` / `Optional` philosophy.

---

### Per-level atomicity in `SynchronizedMultiMap`

Each operation in `SynchronizedMultiMap` is atomic at the current level only,
not across the full depth of a recursive chain.

**Alternative considered:** hierarchical locking — acquiring locks across
multiple levels for a single chained operation.

**Reason for rejection:** impractical with the recursive typing model. Each
level is an independent `MultiMap` instance with its own lock. Multi-level
locking would require an external coordinator, which is outside the scope of
the collection itself. This limitation is documented explicitly in the Javadoc.
Callers requiring cross-level atomicity must provide their own synchronization.

---

### Interface + two implementations rather than a single class

`MultiMap<K, V>` is defined as an interface, with two concrete implementations:
`UnsynchronizedMultiMap` and `SynchronizedMultiMap`.

**Design rationale:** same as `ChunkyList`, `TreeList`, and `SymmetricMap` —
the thread-safety contract is explicit in the type system.

---

### `ReentrantReadWriteLock` and snapshot-based iterators in `SynchronizedMultiMap`

`SynchronizedMultiMap` uses a `ReentrantReadWriteLock` to protect all operations
and returns snapshot-based iterators for `keySet()`, `values()`, and
`entrySet()`.

**Design rationale:** same pattern as the other synchronized collections in the
project. Multiple concurrent readers are allowed; writes are exclusive. Snapshot
iterators avoid holding a read lock for the duration of an iteration.