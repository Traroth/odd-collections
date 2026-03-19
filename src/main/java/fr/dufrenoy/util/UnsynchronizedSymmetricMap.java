/*
 * UnsynchronizedSymmetricMap.java
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

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * A bijective map where both keys and values are unique, backed by a single
 * array of {@link Bucket}s with two independent collision chains per bucket —
 * one indexed by key hash, one indexed by value hash.
 *
 * <p>This allows O(1) average lookup in both directions:
 * <ul>
 *   <li>{@link #get(Object)} — retrieve a value by key</li>
 *   <li>{@link #getKey(Object)} — retrieve a key by value</li>
 * </ul>
 *
 * <p>Two insertion modes are provided:
 * <ul>
 *   <li>{@link #put(Object, Object)} — always overwrites; if the key or value
 *       already exists, the conflicting entry is silently removed.</li>
 *   <li>{@link #safePut(Object, Object)} — throws {@link IllegalArgumentException}
 *       if the key or value already exists.</li>
 * </ul>
 *
 * <p>Additional symmetric operations:
 * <ul>
 *   <li>{@link #removeByValue(Object)} — remove an entry by value, symmetric to
 *       {@link #remove(Object)} which removes by key.</li>
 *   <li>{@link #inverse()} — return an independent copy of this map with keys
 *       and values swapped, as an {@link UnsynchronizedSymmetricMap}.</li>
 * </ul>
 *
 * <p><strong>Iteration order:</strong> this map makes no guarantee on the order
 * of elements returned by {@link #keySet()}, {@link #values()}, and
 * {@link #entrySet()}. However, all three views iterate over the same internal
 * key-indexed chain, so their iteration orders are mutually consistent — the
 * i-th key in {@code keySet()} corresponds to the i-th value in {@code values()}
 * and the i-th entry in {@code entrySet()}. This order may change after a resize.
 *
 * <p>This implementation is <strong>not thread-safe</strong>. For concurrent
 * access, use {@link SynchronizedSymmetricMap}.
 *
 * @param <K> the type of keys
 * @param <V> the type of values
 */
public class UnsynchronizedSymmetricMap<K, V> extends AbstractMap<K, V>
        implements SymmetricMap<K, V> {

    // ─── Constants ────────────────────────────────────────────────────────────

    private static final int DEFAULT_INITIAL_CAPACITY = 16;
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;

    // ─── Instance fields ──────────────────────────────────────────────────────

    private Bucket<K, V>[] table;
    private int size;
    private final float loadFactor;
    private int threshold;

    // ─── Constructors ─────────────────────────────────────────────────────────

    /**
     * Creates a new {@code UnsynchronizedSymmetricMap} with the default initial
     * capacity (16) and load factor (0.75).
     */
    public UnsynchronizedSymmetricMap() {
        this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Creates a new {@code UnsynchronizedSymmetricMap} with the given initial
     * capacity and the default load factor (0.75).
     *
     * @param initialCapacity the initial capacity; must be at least 1
     * @throws IllegalArgumentException if {@code initialCapacity} is less than 1
     */
    public UnsynchronizedSymmetricMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Creates a new {@code UnsynchronizedSymmetricMap} with the given initial
     * capacity and load factor.
     *
     * @param initialCapacity the initial capacity; must be at least 1
     * @param loadFactor      the load factor; must be positive
     * @throws IllegalArgumentException if {@code initialCapacity} is less than 1
     *                                  or {@code loadFactor} is not positive
     */
    public UnsynchronizedSymmetricMap(int initialCapacity, float loadFactor) {
        if (initialCapacity < 1)
            throw new IllegalArgumentException("initialCapacity must be at least 1");
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("loadFactor must be positive");
        this.loadFactor = loadFactor;
        this.table = newTable(initialCapacity);
        this.threshold = (int) (initialCapacity * loadFactor);
    }

    // ─── Public methods (Map contract) ────────────────────────────────────────

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean containsKey(Object key) {
        return getEntry(key) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        return getEntryByValue(value) != null;
    }

    @Override
    public V get(Object key) {
        Entry<K, V> e = getEntry(key);
        return e == null ? null : e.value;
    }

    /**
     * {@inheritDoc}
     *
     * @return an {@code Optional} containing the key associated with {@code value},
     *         or empty if not found
     */
    @Override
    public Optional<K> getKey(Object value) {
        Entry<K, V> e = getEntryByValue(value);
        return e == null ? Optional.empty() : Optional.of(e.key);
    }

    /**
     * Associates the given key with the given value. If the key or value already
     * exists, the conflicting entry is silently removed to maintain bijectivity.
     *
     * <p>If the exact same key-value pair already exists, this method is a no-op
     * and returns the existing value.
     *
     * @param key   the key
     * @param value the value
     * @return the previous value associated with {@code key}, or {@code null}
     */
    @Override
    public V put(K key, V value) {
        Entry<K, V> existingByKey = getEntry(key);

        // Same exact pair — no-op
        if (existingByKey != null && Objects.equals(existingByKey.value, value))
            return value;

        // Remove any existing entry with the same value (to maintain bijectivity)
        Entry<K, V> existingByValue = getEntryByValue(value);
        if (existingByValue != null) removeEntry(existingByValue);

        // Remove any existing entry with the same key
        V oldValue = existingByKey == null ? null : existingByKey.value;
        if (existingByKey != null) removeEntry(existingByKey);

        insertEntry(key, value);
        return oldValue;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if the key or value already exists
     */
    @Override
    public void safePut(K key, V value) {
        if (containsKey(key))
            throw new IllegalArgumentException("Key already exists: " + key);
        if (containsValue(value))
            throw new IllegalArgumentException("Value already exists: " + value);
        insertEntry(key, value);
    }

    @Override
    public V remove(Object key) {
        Entry<K, V> e = getEntry(key);
        if (e == null) return null;
        removeEntry(e);
        return e.value;
    }

    /**
     * {@inheritDoc}
     *
     * @return an {@code Optional} containing the key that was associated with {@code value},
     *         or empty if not found
     */
    @Override
    public Optional<K> removeByValue(Object value) {
        Entry<K, V> e = getEntryByValue(value);
        if (e == null) return Optional.empty();
        removeEntry(e);
        return Optional.of(e.key);
    }


    /**
     * Returns an independent copy of this map with keys and values swapped,
     * as an {@link UnsynchronizedSymmetricMap}.
     * Modifications to the returned map do not affect this map, and vice versa.
     *
     * @return a new {@code UnsynchronizedSymmetricMap<V, K>} with all entries inverted
     */
    @Override
    public UnsynchronizedSymmetricMap<V, K> inverse() {
        UnsynchronizedSymmetricMap<V, K> inv =
                new UnsynchronizedSymmetricMap<>(table.length, loadFactor);
        for (Bucket b : table) {
            Entry<K, V> e = b.firstByKey;
            while (e != null) {
                inv.insertEntry(e.value, e.key);
                e = e.nextByKey;
            }
        }
        return inv;
    }

    @Override
    public void clear() {
        for (Bucket b : table) {
            b.firstByKey = null;
            b.firstByValue = null;
        }
        size = 0;
    }

    @Override
    public Set<K> keySet() {
        return new KeySetView();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Because values in a bijective map are unique by definition, this method
     * returns a {@link Set}{@code <V>} rather than a
     * {@link Collection}{@code <V>}.
     */
    @Override
    public Set<V> values() {
        return new ValueSetView();
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return new EntrySetView();
    }

    // ─── Private methods ──────────────────────────────────────────────────────

    private int keyIndex(int keyHash) {
        return Math.abs(keyHash) % table.length;
    }

    private int valueIndex(int valueHash) {
        return Math.abs(valueHash) % table.length;
    }

    private Entry<K, V> getEntry(Object key) {
        int idx = keyIndex(Objects.hashCode(key));
        Entry<K, V> e = table[idx].firstByKey;
        while (e != null) {
            if (Objects.equals(e.key, key)) return e;
            e = e.nextByKey;
        }
        return null;
    }

    private Entry<K, V> getEntryByValue(Object value) {
        int idx = valueIndex(Objects.hashCode(value));
        Entry<K, V> e = table[idx].firstByValue;
        while (e != null) {
            if (Objects.equals(e.value, value)) return e;
            e = e.nextByValue;
        }
        return null;
    }

    /**
     * Inserts a new entry for the given key and value, resizing if necessary.
     *
     * <p>Package-private to allow delegation from {@link SynchronizedSymmetricMap}.
     * Callers are responsible for ensuring bijectivity before calling this method.
     *
     * @param key   the key
     * @param value the value
     */
    void insertEntry(K key, V value) {
        if (size >= threshold) resize();

        int kHash = Objects.hashCode(key);
        int vHash = Objects.hashCode(value);
        Entry<K, V> e = new Entry<>(key, kHash, value, vHash);

        int kIdx = keyIndex(kHash);
        e.nextByKey = table[kIdx].firstByKey;
        table[kIdx].firstByKey = e;

        int vIdx = valueIndex(vHash);
        e.nextByValue = table[vIdx].firstByValue;
        table[vIdx].firstByValue = e;

        size++;
    }

    /**
     * Removes the given entry from both the key chain and the value chain.
     *
     * <p>Package-private to allow delegation from {@link SynchronizedSymmetricMap}.
     *
     * @param entry the entry to remove
     */
    void removeEntry(Entry<K, V> entry) {
        // Remove from key chain
        int kIdx = keyIndex(entry.keyHash);
        Entry<K, V> prev = null;
        Entry<K, V> cur = table[kIdx].firstByKey;
        while (cur != null) {
            if (cur == entry) {
                if (prev == null) table[kIdx].firstByKey = cur.nextByKey;
                else prev.nextByKey = cur.nextByKey;
                break;
            }
            prev = cur;
            cur = cur.nextByKey;
        }

        // Remove from value chain
        int vIdx = valueIndex(entry.valueHash);
        prev = null;
        cur = table[vIdx].firstByValue;
        while (cur != null) {
            if (cur == entry) {
                if (prev == null) table[vIdx].firstByValue = cur.nextByValue;
                else prev.nextByValue = cur.nextByValue;
                break;
            }
            prev = cur;
            cur = cur.nextByValue;
        }

        size--;
    }

    @SuppressWarnings("unchecked")
    private Bucket<K, V>[] newTable(int capacity) {
        Bucket[] t = new Bucket[capacity];
        for (int i = 0; i < capacity; i++) {
            t[i] = new Bucket();
        }
        return t;
    }

    private void resize() {
        int newCapacity = table.length * 2;
        Bucket[] newTable = newTable(newCapacity);

        // Reinsert all entries into the new table
        for (Bucket b : table) {
            Entry<K, V> e = b.firstByKey;
            while (e != null) {
                Entry<K, V> next = e.nextByKey;
                // Clear links
                e.nextByKey = null;
                e.nextByValue = null;

                // Insert in key chain
                int kIdx = Math.abs(e.keyHash) % newCapacity;
                e.nextByKey = newTable[kIdx].firstByKey;
                newTable[kIdx].firstByKey = e;

                // Insert in value chain
                int vIdx = Math.abs(e.valueHash) % newCapacity;
                e.nextByValue = newTable[vIdx].firstByValue;
                newTable[vIdx].firstByValue = e;

                e = next;
            }
        }

        table = newTable;
        threshold = (int) (newCapacity * loadFactor);
    }

    // ─── Inner class: Entry ───────────────────────────────────────────────────

    /**
     * A map entry holding a key, a value, and their respective hash codes,
     * belonging simultaneously to a key chain and a value chain.
     *
     * <p>Package-private to allow access from {@link SynchronizedSymmetricMap}.
     */
    static class Entry<K, V> implements Map.Entry<K, V> {

        final K key;
        final int keyHash;
        V value;
        final int valueHash;
        Entry<K, V> nextByKey;
        Entry<K, V> nextByValue;

        Entry(K key, int keyHash, V value, int valueHash) {
            this.key = key;
            this.keyHash = keyHash;
            this.value = value;
            this.valueHash = valueHash;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(key) ^ Objects.hashCode(value);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry)) return false;
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            return Objects.equals(key, e.getKey()) && Objects.equals(value, e.getValue());
        }

        @Override
        public K getKey() { return key; }

        @Override
        public V getValue() { return value; }

        /**
         * Not supported on raw entries — use the entry obtained from
         * {@link UnsynchronizedSymmetricMap#entrySet()} to get setValue() support.
         *
         * @throws UnsupportedOperationException always
         */
        @Override
        public V setValue(V newValue) {
            throw new UnsupportedOperationException(
                    "setValue is not supported on raw entries");
        }
    }

    // ─── Inner class: KeySetView ──────────────────────────────────────────────

    private class KeySetView extends AbstractSet<K> {

        @Override
        public int size() { return UnsynchronizedSymmetricMap.this.size(); }

        @Override
        public boolean contains(Object o) { return containsKey(o); }

        @Override
        public boolean remove(Object o) {
            return UnsynchronizedSymmetricMap.this.remove(o) != null;
        }

        @Override
        public void clear() { UnsynchronizedSymmetricMap.this.clear(); }

        @Override
        public Iterator<K> iterator() {
            return new Iterator<K>() {
                private int bucketIndex = 0;
                private Entry<K, V> next = advanceToNext();
                private Entry<K, V> current = null;

                private Entry<K, V> advanceToNext() {
                    while (bucketIndex < table.length) {
                        Entry<K, V> e = table[bucketIndex].firstByKey;
                        if (e != null) return e;
                        bucketIndex++;
                    }
                    return null;
                }

                @Override
                public boolean hasNext() { return next != null; }

                @Override
                public K next() {
                    if (next == null) throw new NoSuchElementException();
                    current = next;
                    if (current.nextByKey != null) {
                        next = current.nextByKey;
                    } else {
                        bucketIndex++;
                        next = advanceToNext();
                    }
                    return current.key;
                }

                @Override
                public void remove() {
                    if (current == null) throw new IllegalStateException();
                    removeEntry(current);
                    current = null;
                }
            };
        }
    }

    // ─── Inner class: ValueSetView ────────────────────────────────────────────

    private class ValueSetView extends AbstractSet<V> {

        @Override
        public int size() { return UnsynchronizedSymmetricMap.this.size(); }

        @Override
        public boolean contains(Object o) { return containsValue(o); }

        @Override
        public boolean remove(Object o) {
            return removeByValue(o).isPresent();
        }

        @Override
        public void clear() { UnsynchronizedSymmetricMap.this.clear(); }

        @Override
        public Iterator<V> iterator() {
            return new Iterator<V>() {
                private int bucketIndex = 0;
                private Entry<K, V> next = advanceToNext();
                private Entry<K, V> current = null;

                private Entry<K, V> advanceToNext() {
                    while (bucketIndex < table.length) {
                        Entry<K, V> e = table[bucketIndex].firstByKey;
                        if (e != null) return e;
                        bucketIndex++;
                    }
                    return null;
                }

                @Override
                public boolean hasNext() { return next != null; }

                @Override
                public V next() {
                    if (next == null) throw new NoSuchElementException();
                    current = next;
                    if (current.nextByKey != null) {
                        next = current.nextByKey;
                    } else {
                        bucketIndex++;
                        next = advanceToNext();
                    }
                    return current.value;
                }

                @Override
                public void remove() {
                    if (current == null) throw new IllegalStateException();
                    removeEntry(current);
                    current = null;
                }
            };
        }
    }

    // ─── Inner class: EntrySetView ────────────────────────────────────────────

    private class EntrySetView extends AbstractSet<Map.Entry<K, V>> {

        @Override
        public int size() { return UnsynchronizedSymmetricMap.this.size(); }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry)) return false;
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            Entry<K, V> found = getEntry(e.getKey());
            return found != null && Objects.equals(found.value, e.getValue());
        }

        @Override
        public boolean remove(Object o) {
            if (!(o instanceof Map.Entry)) return false;
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            return UnsynchronizedSymmetricMap.this.remove(e.getKey()) != null;
        }

        @Override
        public void clear() { UnsynchronizedSymmetricMap.this.clear(); }

        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return new Iterator<Map.Entry<K, V>>() {
                private int bucketIndex = 0;
                private Entry<K, V> next = advanceToNext();
                private Entry<K, V> current = null;

                private Entry<K, V> advanceToNext() {
                    while (bucketIndex < table.length) {
                        Entry<K, V> e = table[bucketIndex].firstByKey;
                        if (e != null) return e;
                        bucketIndex++;
                    }
                    return null;
                }

                @Override
                public boolean hasNext() { return next != null; }

                @Override
                public Map.Entry<K, V> next() {
                    if (next == null) throw new NoSuchElementException();
                    current = next;
                    if (current.nextByKey != null) {
                        next = current.nextByKey;
                    } else {
                        bucketIndex++;
                        next = advanceToNext();
                    }
                    // Wrap the raw entry to provide setValue() support
                    final Entry<K, V> snapshot = current;
                    return new Map.Entry<K, V>() {
                        @Override
                        public K getKey() { return snapshot.key; }

                        @Override
                        public V getValue() { return snapshot.value; }

                        @Override
                        public V setValue(V newValue) {
                            V oldValue = snapshot.value;
                            UnsynchronizedSymmetricMap.this.put(snapshot.key, newValue);
                            return oldValue;
                        }

                        @Override
                        public int hashCode() { return snapshot.hashCode(); }

                        @Override
                        public boolean equals(Object o) {
                            if (!(o instanceof Map.Entry)) {
                                return false;
                            }
                            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
                            return Objects.equals(snapshot.key, e.getKey())
                                    && Objects.equals(snapshot.value, e.getValue());
                        }
                    };
                }

                @Override
                public void remove() {
                    if (current == null) throw new IllegalStateException();
                    removeEntry(current);
                    current = null;
                }
            };
        }
    }

    // ─── Inner class: Bucket ──────────────────────────────────────────────────

    /**
     * A bucket holding the head of two independent collision chains —
     * one indexed by key hash, one indexed by value hash.
     *
     * <p>Package-private to allow access from {@link SynchronizedSymmetricMap}.
     */
    static class Bucket<K, V> {
        Entry<K, V> firstByKey;
        Entry<K, V> firstByValue;
    }
}
