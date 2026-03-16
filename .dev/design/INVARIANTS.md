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

    null elements are allowed

If a structure forbids null values, the invariant must state:

    no stored element is null

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

    getKey(getValue(k)) == k
    getValue(getKey(v)) == v

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
