# INVARIANTS.md

This document defines the structural invariants expected from the data
structures implemented in **odd-collections**.

An invariant is a condition that must **always hold for any observable
state** of a class. Public methods must preserve these invariants.

Whenever possible, invariants should also be expressed in the code using
**JML `@invariant` annotations** so they can be checked by static
analysis tools.

------------------------------------------------------------------------

# General principles

All structures in the library must respect the following principles:

-   Internal state must never become externally inconsistent
-   Mutation operations must restore invariants before returning
-   Iterators must never expose invalid intermediate states
-   Public methods must not violate documented complexity guarantees

If an invariant must be temporarily broken inside a method, it must be
restored before the method returns.

------------------------------------------------------------------------

# Structural invariants

## Size consistency

For all collections:

    size >= 0

If the structure uses internal storage:

    size <= capacity

If the structure maintains multiple internal representations (for
example a node graph + an index):

    all representations must contain the same number of elements

------------------------------------------------------------------------

## Null handling

Unless explicitly documented otherwise:

    null elements are forbidden

If a structure explicitly allows null values, the invariant must state:

    null elements are permitted

------------------------------------------------------------------------

## Internal container integrity

For structures relying on arrays, lists, or node chains:

-   Internal containers must never contain dangling references
-   Unused slots must be null or clearly marked as empty
-   Links between nodes must be bidirectionally consistent

Example for a doubly linked list:

    node.next.prev == node
    node.prev.next == node

------------------------------------------------------------------------

# Hash-based structures

For hash-based structures:

    0 <= bucketIndex < bucketCount

All entries must reside in the bucket corresponding to their hash.

If load factor constraints exist:

    size / bucketCount <= maxLoadFactor

------------------------------------------------------------------------

# Ordered structures

If a structure maintains ordering:

    elements[i] <= elements[i+1]

(or according to the provided comparator)

If duplicates are forbidden:

    no two elements are equal

------------------------------------------------------------------------

# Symmetric structures

For structures with bidirectional relationships
(e.g. `SymmetricMap<K,V>`):

    getKey(get(k)).get() == k    (for all k where containsKey(k))
    get(getKey(v).get())  == v   (for all v where containsValue(v))

Mappings must remain perfectly mirrored.

------------------------------------------------------------------------

# Chunked / segmented structures

For segmented collections (such as `ChunkyList`):

    each chunk size <= chunkCapacity

Optional balancing constraint:

    chunkCapacity / 2 <= chunkSize <= chunkCapacity

(except possibly for the first or last chunk)

------------------------------------------------------------------------

# Iterator invariants

Iterators must respect the following:

-   No element is returned twice
-   No element is skipped
-   Concurrent structural modifications must trigger
    `ConcurrentModificationException` if the structure is fail-fast

------------------------------------------------------------------------

# JML integration

Whenever feasible, invariants should be expressed directly in the code.

Example:

    //@ invariant size >= 0;
    //@ invariant size <= elements.length;

For symmetric maps:

    //@ invariant (\forall K k; map.containsKey(k); reverse.get(map.get(k)) == k);

This allows automated verification using JML tools.

------------------------------------------------------------------------

# Concrete invariants per class

------------------------------------------------------------------------

## SymmetricMap

> **Note:** JML annotations on `SymmetricMap` were added retroactively,
> after the implementation was complete. The invariants below were derived
> from the existing code and tests, not used to drive the design.

### Null keys and values

`SymmetricMap` explicitly allows `null` as both a key and a value.
`null` behaves as a regular entry: `put(null, 1)` is valid, and
`getKey(null)` performs a lookup by null value.

    null elements are permitted

### Bijectivity

The core invariant: no two entries share the same key, and no two
entries share the same value.

    //@ invariant (\forall K k; containsKey(k); getKey(get(k)).isPresent());
    //@ invariant (\forall K k; containsKey(k); Objects.equals(getKey(get(k)).get(), k));
    //@ invariant (\forall V v; containsValue(v); Objects.equals(get(getKey(v).get()), v));

### Table structure (`UnsynchronizedSymmetricMap`)

    table != null
    table.length >= 1
    (\forall int i; 0 <= i < table.length; table[i] != null)
    threshold == (int)(table.length * loadFactor)
    size >= 0

### Load factor constraint

Maintained by `resize()` before every `insertEntry()`:

    size <= threshold

### Hash chain placement

Each entry resides in the key bucket corresponding to its key hash,
and in the value bucket corresponding to its value hash:

    //@ invariant (\forall Entry e; reachable(e);
    //@     e is in table[Math.abs(e.keyHash) % table.length].firstByKey chain);
    //@ invariant (\forall Entry e; reachable(e);
    //@     e is in table[Math.abs(e.valueHash) % table.length].firstByValue chain);

Stored hashes match the actual hash of the key and value at insertion
time. This invariant relies on `hashCode()` being stable for the
lifetime of the entry in the map (i.e. mutable keys or values whose
hash changes after insertion will silently break lookup):

    e.keyHash   == Objects.hashCode(e.key)
    e.valueHash == Objects.hashCode(e.value)

### Entry chain membership

Each entry appears exactly once in one key chain and exactly once in
one value chain. `size` equals the total number of entries across all
key chains:

    //@ invariant size == (\sum int i; 0 <= i < table.length; chain_length(table[i].firstByKey));

------------------------------------------------------------------------

## ChunkyList

> **Note:** JML annotations on `ChunkyList` were added retroactively,
> after the implementation was complete. The invariants below were derived
> from the existing code and tests, not used to drive the design.

### Null elements

`ChunkyList` does **not** allow null elements (contrary to the general
library default). `add()`, `set()`, and `addAll()` reject `null` with
`IllegalArgumentException`.

    //@ invariant (\forall Chunk c; reachable(c);
    //@     (\forall int i; 0 <= i < c.nbElements; c.elements[i] != null));

### Size consistency

`size` equals the sum of `nbElements` across all chunks in the chain:

    //@ invariant size >= 0;
    //@ invariant size == (\sum Chunk c; reachable(c); c.nbElements);

### Empty list

    //@ invariant size == 0 <==> (firstChunk == null && lastChunk == null);

### Chunk chain structure

When non-empty, the chain is a doubly-linked list with no dangling
terminal references:

    firstChunk.previousChunk == null
    lastChunk.nextChunk      == null

Bidirectional links are consistent:

    //@ invariant (\forall Chunk c; reachable(c) && c.nextChunk != null;
    //@     c.nextChunk.previousChunk == c);
    //@ invariant (\forall Chunk c; reachable(c) && c.previousChunk != null;
    //@     c.previousChunk.nextChunk == c);

### Chunk content bounds

    //@ invariant (\forall Chunk c; reachable(c); 0 <= c.nbElements && c.nbElements <= chunkSize);

Unused array slots are null (cleared on removal):

    //@ invariant (\forall Chunk c; reachable(c);
    //@     (\forall int i; c.nbElements <= i < chunkSize; c.elements[i] == null));

### Configuration

    chunkSize >= 1                      // enforced in constructor, final field
    currentGrowingStrategy  != null
    currentShrinkingStrategy != null

------------------------------------------------------------------------

## TreeList

> **Note:** `TreeList` is the first structure in this project developed
> **JML-first**: invariants and method contracts were written before
> implementation and used to drive the design, tests, and verification.

### Null elements

`TreeList` does not allow null elements. `add()` and constructors reject
`null` with `NullPointerException`.

    //@ invariant (\forall int i; 0 <= i < size(); get(i) != null);

### Strict sort order and no duplicates

Elements are in strictly ascending order according to the comparator (or
natural ordering). Strict inequality implies no duplicates.

    //@ invariant (\forall int i; 0 <= i < size() - 1;
    //@     compare(get(i), get(i + 1)) < 0);

### Size consistency

`size` equals `root.subtreeSize` when the tree is non-empty, and 0 when
the root is null:

    //@ invariant size >= 0;
    //@ invariant root == null ==> size == 0;
    //@ invariant root != null ==> size == root.subtreeSize;

### BST property (`UnsynchronizedTreeList`)

For every node `n`, all elements in its left subtree compare strictly less
than `n.element`, and all elements in its right subtree compare strictly
greater:

    //@ invariant (\forall Node n; reachable(n);
    //@     (\forall Node l; inLeftSubtree(l, n);  compare(l.element, n.element) < 0));
    //@ invariant (\forall Node n; reachable(n);
    //@     (\forall Node r; inRightSubtree(r, n); compare(r.element, n.element) > 0));

### Red-black properties (`UnsynchronizedTreeList`)

Standard red-black tree invariants, maintained after every rotation and
colour flip:

    root.color == BLACK                              // root is always black
    (\forall Node n; reachable(n) && n.color == RED; // no two consecutive red nodes
        n.parent.color == BLACK)
    (\forall Node n; reachable(n);                   // equal black-height on all paths
        blackHeight(n.left) == blackHeight(n.right))

### Subtree size augmentation (`UnsynchronizedTreeList`)

Every node stores the number of nodes in its subtree (including itself).
This invariant must be restored after every rotation and structural change:

    //@ invariant (\forall Node n; reachable(n);
    //@     n.subtreeSize == 1
    //@         + (n.left  != null ? n.left.subtreeSize  : 0)
    //@         + (n.right != null ? n.right.subtreeSize : 0));

------------------------------------------------------------------------

# When adding a new structure

Whenever a new class is introduced:

1.  Identify its core structural invariants
2.  Add them to the class using JML annotations
3.  Document any non-obvious invariants in this file
4.  Ensure mutation methods preserve them

------------------------------------------------------------------------

# Philosophy

A correct data structure is not defined only by its methods, but by the
**invariants that always hold between them**.

The goal of this document is to make those invariants **explicit,
verifiable, and maintainable**.
