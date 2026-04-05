/*
 * SynchronizedMultiMap.java
 *
 * Version 1.0-SNAPSHOT
 *
 * odd-collections - A collection of unconventional Java data structures
 * Copyright (C) 2026  Dufrenoy
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, see
 * <https://www.gnu.org/licenses/>.
 */

package fr.dufrenoy.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * A thread-safe implementation of {@link MultiMap}, backed by an
 * {@link UnsynchronizedMultiMap} and protected by a
 * {@link ReentrantReadWriteLock}.
 *
 * <p>Multiple threads may read concurrently. Write operations are exclusive:
 * a write blocks until all ongoing reads have completed, and blocks any new
 * reads until the write is done.
 *
 * <p>The iterators returned by the views of this map ({@link #keySet()},
 * {@link #values()}, {@link #entrySet()}) are <em>fail-safe</em>: they operate
 * on a snapshot of the map taken at the time of the call, under a read lock.
 * Subsequent modifications to the map are not reflected in the snapshot.
 *
 * <p><strong>Atomicity note:</strong> each operation is atomic at the current
 * level only. In a recursive multi-level structure, operations on sub-maps
 * are independent — there is no cross-level locking. Callers requiring
 * atomicity across multiple levels must provide their own synchronization.
 *
 * <p><strong>Memory note:</strong> snapshot-based iterators copy the entire
 * entry set at the time of the call. Avoid iterating over very large maps in
 * memory-constrained environments.
 *
 * <p>Null keys and null values are forbidden. All methods that accept keys
 * or values throw {@link NullPointerException} if a null argument is passed.
 *
 * @param <K> the type of keys at this level
 * @param <V> the type of values at this level (may be another {@code MultiMap})
 *
 * @see MultiMap
 * @see UnsynchronizedMultiMap
 */
public class SynchronizedMultiMap<K, V> implements MultiMap<K, V> {

    /*@
      @ public invariant size() >= 0;
      @ public invariant size() == 0 <==> isEmpty();
      @ public invariant (\forall K k; containsKey(k); get(k) != null);
      @ public invariant (\forall K k; containsKey(k); getOpt(k).isPresent());
      @ public invariant (\forall K k; !containsKey(k); get(k) == null);
      @*/

    // ─── Instance fields ─────────────────────────────────────────────────────

    private final UnsynchronizedMultiMap<K, V> inner;
    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = rwl.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = rwl.writeLock();

    // ─── Constructors ────────────────────────────────────────────────────────

    /**
     * Creates a new empty {@code SynchronizedMultiMap} with default initial
     * capacity.
     */
    //@ ensures size() == 0;
    public SynchronizedMultiMap() {
        this.inner = new UnsynchronizedMultiMap<>();
    }

    /**
     * Creates a new empty {@code SynchronizedMultiMap} with the specified
     * initial capacity.
     *
     * @param initialCapacity the initial capacity
     * @throws IllegalArgumentException if {@code initialCapacity} is negative
     */
    //@ requires initialCapacity >= 0;
    //@ ensures size() == 0;
    public SynchronizedMultiMap(int initialCapacity) {
        this.inner = new UnsynchronizedMultiMap<>(initialCapacity);
    }

    /**
     * Creates a new {@code SynchronizedMultiMap} containing the same
     * mappings as the specified map. Null keys and null values in the source
     * map cause a {@link NullPointerException}.
     *
     * @param source the map whose mappings are to be copied
     * @throws NullPointerException if {@code source} is null, or if it
     *                              contains null keys or null values
     */
    //@ requires source != null;
    //@ ensures size() == source.size();
    public SynchronizedMultiMap(Map<? extends K, ? extends V> source) {
        this.inner = new UnsynchronizedMultiMap<>(source);
    }

    /**
     * Creates a new {@code SynchronizedMultiMap} containing the same
     * mappings as the specified {@code MultiMap}.
     *
     * @param source the multi-map whose mappings are to be copied
     * @throws NullPointerException if {@code source} is null
     */
    //@ requires source != null;
    //@ ensures size() == source.size();
    public SynchronizedMultiMap(MultiMap<? extends K, ? extends V> source) {
        this.inner = new UnsynchronizedMultiMap<>(source);
    }

    // ─── Lookup ──────────────────────────────────────────────────────────────

    //@ also
    //@ requires key != null;
    //@ ensures containsKey(key) ==> \result != null;
    //@ ensures !containsKey(key) ==> \result == null;
    @Override
    public V get(K key) {
        readLock.lock();
        try {
            return inner.get(key);
        } finally {
            readLock.unlock();
        }
    }

    //@ also
    //@ requires key != null;
    //@ ensures containsKey(key) ==> \result.isPresent();
    //@ ensures !containsKey(key) ==> !\result.isPresent();
    @Override
    public /*@ pure @*/ Optional<V> getOpt(K key) {
        readLock.lock();
        try {
            return inner.getOpt(key);
        } finally {
            readLock.unlock();
        }
    }

    //@ also
    //@ requires key != null;
    //@ requires factory != null;
    //@ ensures \result != null;
    //@ ensures containsKey(key);
    @Override
    public V getOrCreate(K key, Supplier<V> factory) {
        writeLock.lock();
        try {
            return inner.getOrCreate(key, factory);
        } finally {
            writeLock.unlock();
        }
    }

    //@ also
    //@ requires key != null;
    @Override
    public /*@ pure @*/ boolean containsKey(K key) {
        readLock.lock();
        try {
            return inner.containsKey(key);
        } finally {
            readLock.unlock();
        }
    }

    // ─── Modification ────────────────────────────────────────────────────────

    //@ also
    //@ requires key != null;
    //@ requires value != null;
    //@ ensures get(key) == value;
    //@ ensures containsKey(key);
    @Override
    public V put(K key, V value) {
        writeLock.lock();
        try {
            return inner.put(key, value);
        } finally {
            writeLock.unlock();
        }
    }

    //@ also
    //@ requires key != null;
    //@ ensures !containsKey(key);
    @Override
    public V remove(K key) {
        writeLock.lock();
        try {
            return inner.remove(key);
        } finally {
            writeLock.unlock();
        }
    }

    //@ also
    //@ ensures size() == 0;
    //@ ensures isEmpty();
    @Override
    public void clear() {
        writeLock.lock();
        try {
            inner.clear();
        } finally {
            writeLock.unlock();
        }
    }

    // ─── Size and status ─────────────────────────────────────────────────────

    //@ also
    //@ ensures \result >= 0;
    @Override
    public /*@ pure @*/ int size() {
        readLock.lock();
        try {
            return inner.size();
        } finally {
            readLock.unlock();
        }
    }

    //@ also
    //@ ensures \result <==> (size() == 0);
    @Override
    public /*@ pure @*/ boolean isEmpty() {
        readLock.lock();
        try {
            return inner.isEmpty();
        } finally {
            readLock.unlock();
        }
    }

    // ─── Views (snapshot-based) ──────────────────────────────────────────────

    @Override
    public Set<K> keySet() {
        readLock.lock();
        try {
            return new ArrayList<>(inner.keySet())
                .stream()
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Collection<V> values() {
        readLock.lock();
        try {
            return new ArrayList<>(inner.values());
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        readLock.lock();
        try {
            return new java.util.LinkedHashSet<>(inner.entrySet());
        } finally {
            readLock.unlock();
        }
    }

    // ─── Object methods ──────────────────────────────────────────────────────

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SynchronizedMultiMap)) {
            return false;
        }
        SynchronizedMultiMap<?, ?> other = (SynchronizedMultiMap<?, ?>) obj;
        readLock.lock();
        try {
            other.readLock.lock();
            try {
                return inner.equals(other.inner);
            } finally {
                other.readLock.unlock();
            }
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public int hashCode() {
        readLock.lock();
        try {
            return inner.hashCode();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public String toString() {
        readLock.lock();
        try {
            return inner.toString();
        } finally {
            readLock.unlock();
        }
    }
}
