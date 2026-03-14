/*
 * SynchronizedSymmetricMap.java
 *
 * Version 1.0
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

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiFunction;

/**
 * A thread-safe implementation of {@link SymmetricMap}, backed by an
 * {@link UnsynchronizedSymmetricMap} and protected by a
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
 * <p><strong>Memory note:</strong> snapshot-based iterators copy the entire
 * entry set at the time of the call. Avoid iterating over very large maps in
 * memory-constrained environments.
 *
 * @param <K> the type of keys
 * @param <V> the type of values
 */
public class SynchronizedSymmetricMap<K, V> implements SymmetricMap<K, V> {

    // ─── Instance fields ──────────────────────────────────────────────────────

    private final UnsynchronizedSymmetricMap<K, V> inner;
    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = rwl.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = rwl.writeLock();

    // ─── Constructors ─────────────────────────────────────────────────────────

    /**
     * Creates a new {@code SynchronizedSymmetricMap} with the default initial
     * capacity (16) and load factor (0.75).
     */
    public SynchronizedSymmetricMap() {
        this.inner = new UnsynchronizedSymmetricMap<>();
    }

    /**
     * Creates a new {@code SynchronizedSymmetricMap} with the given initial
     * capacity and the default load factor (0.75).
     *
     * @param initialCapacity the initial capacity; must be at least 1
     * @throws IllegalArgumentException if {@code initialCapacity} is less than 1
     */
    public SynchronizedSymmetricMap(int initialCapacity) {
        this.inner = new UnsynchronizedSymmetricMap<>(initialCapacity);
    }

    /**
     * Creates a new {@code SynchronizedSymmetricMap} with the given initial
     * capacity and load factor.
     *
     * @param initialCapacity the initial capacity; must be at least 1
     * @param loadFactor      the load factor; must be positive
     * @throws IllegalArgumentException if {@code initialCapacity} is less than 1
     *                                  or {@code loadFactor} is not positive
     */
    public SynchronizedSymmetricMap(int initialCapacity, float loadFactor) {
        this.inner = new UnsynchronizedSymmetricMap<>(initialCapacity, loadFactor);
    }

    // ─── SymmetricMap contract ────────────────────────────────────────────────

    @Override
    public K getKey(Object value) {
        readLock.lock();
        try {
            return inner.getKey(value);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void safePut(K key, V value) {
        writeLock.lock();
        try {
            inner.safePut(key, value);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public K removeByValue(Object value) {
        writeLock.lock();
        try {
            return inner.removeByValue(value);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Returns an independent copy of this map with keys and values swapped,
     * as a {@link SynchronizedSymmetricMap}. The copy is taken under a read
     * lock to guarantee a consistent snapshot.
     *
     * @return a new {@code SynchronizedSymmetricMap<V, K>} with all entries inverted
     */
    @Override
    public SynchronizedSymmetricMap<V, K> inverse() {
        readLock.lock();
        try {
            SynchronizedSymmetricMap<V, K> inv = new SynchronizedSymmetricMap<>();
            for (Map.Entry<K, V> e : inner.entrySet()) {
                inv.inner.insertEntry(e.getValue(), e.getKey());
            }
            return inv;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>The returned set is a snapshot-based view: its iterator operates on a
     * copy of the map taken under a read lock at the time {@link Set#iterator()}
     * is called.
     *
     * <p><strong>Memory note:</strong> each call to {@link Set#iterator()} copies
     * the entire entry set.
     */
    @Override
    public Set<V> values() {
        return new SnapshotValueSetView();
    }

    // ─── Map contract ─────────────────────────────────────────────────────────

    @Override
    public int size() {
        readLock.lock();
        try {
            return inner.size();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        readLock.lock();
        try {
            return inner.isEmpty();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean containsKey(Object key) {
        readLock.lock();
        try {
            return inner.containsKey(key);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean containsValue(Object value) {
        readLock.lock();
        try {
            return inner.containsValue(value);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public V get(Object key) {
        readLock.lock();
        try {
            return inner.get(key);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public V put(K key, V value) {
        writeLock.lock();
        try {
            return inner.put(key, value);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public V remove(Object key) {
        writeLock.lock();
        try {
            return inner.remove(key);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        writeLock.lock();
        try {
            inner.putAll(m);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void clear() {
        writeLock.lock();
        try {
            inner.clear();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public V replace(K key, V value) {
        writeLock.lock();
        try {
            return inner.replace(key, value);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        writeLock.lock();
        try {
            return inner.replace(key, oldValue, newValue);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Replaces each value with the result of the given function applied to its
     * key and current value. This operation is fully atomic — the write lock is
     * held for its entire duration.
     *
     * <p>Maintains bijectivity: if the function produces a duplicate value, the
     * conflicting entry is silently removed.
     *
     * @param function the remapping function
     */
    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Objects.requireNonNull(function);
        writeLock.lock();
        try {
            // Collect a snapshot of entries under the write lock, then reinsert
            List<Map.Entry<K, V>> snapshot = new ArrayList<>(inner.entrySet());
            for (Map.Entry<K, V> e : snapshot) {
                inner.put(e.getKey(), function.apply(e.getKey(), e.getValue()));
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public V merge(K key, V value,
                   BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        writeLock.lock();
        try {
            return inner.merge(key, value, remappingFunction);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public V compute(K key,
                     BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        writeLock.lock();
        try {
            return inner.compute(key, remappingFunction);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public V computeIfPresent(K key,
                              BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        writeLock.lock();
        try {
            return inner.computeIfPresent(key, remappingFunction);
        } finally {
            writeLock.unlock();
        }
    }

    // ─── Snapshot-based views ─────────────────────────────────────────────────

    /**
     * Returns a {@link Set} view of the keys contained in this map.
     *
     * <p>The returned set is a snapshot-based view: its iterator operates on a
     * copy of the map taken under a read lock at the time {@link Set#iterator()}
     * is called.
     *
     * <p><strong>Memory note:</strong> each call to {@link Set#iterator()} copies
     * the entire entry set.
     */
    @Override
    public Set<K> keySet() {
        return new SnapshotKeySetView();
    }

    /**
     * Returns a {@link Set} view of the entries contained in this map.
     *
     * <p>The returned set is a snapshot-based view: its iterator operates on a
     * copy of the map taken under a read lock at the time {@link Set#iterator()}
     * is called.
     *
     * <p><strong>Memory note:</strong> each call to {@link Set#iterator()} copies
     * the entire entry set.
     */
    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return new SnapshotEntrySetView();
    }

    // ─── equals / hashCode / toString ─────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        readLock.lock();
        try {
            return inner.equals(o);
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

    // ─── Private helpers ──────────────────────────────────────────────────────

    /**
     * Returns a snapshot of the inner map's entry set, taken under a read lock.
     */
    private List<Map.Entry<K, V>> snapshot() {
        readLock.lock();
        try {
            return new ArrayList<>(inner.entrySet());
        } finally {
            readLock.unlock();
        }
    }

    // ─── Inner class: SnapshotKeySetView ──────────────────────────────────────

    private class SnapshotKeySetView extends AbstractSet<K> {

        @Override
        public int size() { return SynchronizedSymmetricMap.this.size(); }

        @Override
        public boolean contains(Object o) { return containsKey(o); }

        @Override
        public boolean remove(Object o) {
            return SynchronizedSymmetricMap.this.remove(o) != null;
        }

        @Override
        public void clear() { SynchronizedSymmetricMap.this.clear(); }

        @Override
        public Iterator<K> iterator() {
            List<Map.Entry<K, V>> snap = snapshot();
            return new Iterator<K>() {
                private final Iterator<Map.Entry<K, V>> it = snap.iterator();

                @Override
                public boolean hasNext() { return it.hasNext(); }

                @Override
                public K next() { return it.next().getKey(); }
            };
        }
    }

    // ─── Inner class: SnapshotValueSetView ────────────────────────────────────

    private class SnapshotValueSetView extends AbstractSet<V> {

        @Override
        public int size() { return SynchronizedSymmetricMap.this.size(); }

        @Override
        public boolean contains(Object o) { return containsValue(o); }

        @Override
        public boolean remove(Object o) {
            return removeByValue(o) != null;
        }

        @Override
        public void clear() { SynchronizedSymmetricMap.this.clear(); }

        @Override
        public Iterator<V> iterator() {
            List<Map.Entry<K, V>> snap = snapshot();
            return new Iterator<V>() {
                private final Iterator<Map.Entry<K, V>> it = snap.iterator();

                @Override
                public boolean hasNext() { return it.hasNext(); }

                @Override
                public V next() { return it.next().getValue(); }
            };
        }
    }

    // ─── Inner class: SnapshotEntrySetView ────────────────────────────────────

    private class SnapshotEntrySetView extends AbstractSet<Map.Entry<K, V>> {

        @Override
        public int size() { return SynchronizedSymmetricMap.this.size(); }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry)) return false;
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            readLock.lock();
            try {
                if (!inner.containsKey(e.getKey())) return false;
                return Objects.equals(inner.get(e.getKey()), e.getValue());
            } finally {
                readLock.unlock();
            }
        }

        @Override
        public boolean remove(Object o) {
            if (!(o instanceof Map.Entry)) return false;
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            return SynchronizedSymmetricMap.this.remove(e.getKey()) != null;
        }

        @Override
        public void clear() { SynchronizedSymmetricMap.this.clear(); }

        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            List<Map.Entry<K, V>> snap = snapshot();
            return snap.iterator();
        }
    }
}