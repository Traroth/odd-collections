/*
 * UnsynchronizedMultiMap.java
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

import java.util.AbstractSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A non-thread-safe implementation of {@link MultiMap} backed by a
 * {@link HashMap}. Each {@code UnsynchronizedMultiMap<K, V>} associates keys
 * of type {@code K} to values of type {@code V}, where {@code V} may itself
 * be another {@code MultiMap}, enabling multi-level key hierarchies.
 *
 * <p>Iterators returned by the view methods ({@link #keySet()},
 * {@link #values()}, {@link #entrySet()}) are <strong>fail-fast</strong>:
 * if the map is structurally modified after the iterator is created, the
 * iterator throws {@link ConcurrentModificationException}.
 *
 * <p>Null keys and null values are forbidden. All methods that accept keys
 * or values throw {@link NullPointerException} if a null argument is passed.
 *
 * <p>This implementation is <strong>not thread-safe</strong>. For concurrent
 * access, use {@link SynchronizedMultiMap}.
 *
 * @param <K> the type of keys at this level
 * @param <V> the type of values at this level (may be another {@code MultiMap})
 *
 * @see MultiMap
 * @see SynchronizedMultiMap
 */
public class UnsynchronizedMultiMap<K, V> implements MultiMap<K, V> {

    /*@
      @ public invariant size() >= 0;
      @ public invariant size() == 0 <==> isEmpty();
      @ public invariant (\forall K k; containsKey(k); get(k) != null);
      @ public invariant (\forall K k; containsKey(k); getOpt(k).isPresent());
      @ public invariant (\forall K k; !containsKey(k); get(k) == null);
      @*/

    // ─── Instance fields ─────────────────────────────────────────────────────

    private final HashMap<K, V> map;
    private int modCount;

    // ─── Constructors ────────────────────────────────────────────────────────

    /**
     * Creates a new empty {@code UnsynchronizedMultiMap} with default initial
     * capacity.
     */
    //@ ensures size() == 0;
    public UnsynchronizedMultiMap() {
        this.map = new HashMap<>();
    }

    /**
     * Creates a new empty {@code UnsynchronizedMultiMap} with the specified
     * initial capacity.
     *
     * @param initialCapacity the initial capacity
     * @throws IllegalArgumentException if {@code initialCapacity} is negative
     */
    //@ requires initialCapacity >= 0;
    //@ ensures size() == 0;
    public UnsynchronizedMultiMap(int initialCapacity) {
        this.map = new HashMap<>(initialCapacity);
    }

    /**
     * Creates a new {@code UnsynchronizedMultiMap} containing the same
     * mappings as the specified map. Null keys and null values in the source
     * map cause a {@link NullPointerException}.
     *
     * @param source the map whose mappings are to be copied
     * @throws NullPointerException if {@code source} is null, or if it
     *                              contains null keys or null values
     */
    //@ requires source != null;
    //@ ensures size() == source.size();
    public UnsynchronizedMultiMap(Map<? extends K, ? extends V> source) {
        Objects.requireNonNull(source);
        this.map = new HashMap<>(source.size());
        for (Map.Entry<? extends K, ? extends V> entry : source.entrySet()) {
            Objects.requireNonNull(entry.getKey(), "null key");
            Objects.requireNonNull(entry.getValue(), "null value");
            map.put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Creates a new {@code UnsynchronizedMultiMap} containing the same
     * mappings as the specified {@code MultiMap}.
     *
     * @param source the multi-map whose mappings are to be copied
     * @throws NullPointerException if {@code source} is null
     */
    //@ requires source != null;
    //@ ensures size() == source.size();
    public UnsynchronizedMultiMap(MultiMap<? extends K, ? extends V> source) {
        Objects.requireNonNull(source);
        this.map = new HashMap<>(source.size());
        for (Map.Entry<? extends K, ? extends V> entry : source.entrySet()) {
            map.put(entry.getKey(), entry.getValue());
        }
    }

    // ─── Lookup ──────────────────────────────────────────────────────────────

    //@ also
    //@ requires key != null;
    //@ ensures containsKey(key) ==> \result != null;
    //@ ensures !containsKey(key) ==> \result == null;
    @Override
    public V get(K key) {
        Objects.requireNonNull(key);
        return map.get(key);
    }

    //@ also
    //@ requires key != null;
    //@ ensures containsKey(key) ==> \result.isPresent();
    //@ ensures !containsKey(key) ==> !\result.isPresent();
    @Override
    public /*@ pure @*/ Optional<V> getOpt(K key) {
        Objects.requireNonNull(key);
        return Optional.ofNullable(map.get(key));
    }

    //@ also
    //@ requires key != null;
    //@ requires factory != null;
    //@ ensures \result != null;
    //@ ensures containsKey(key);
    @Override
    public V getOrCreate(K key, Supplier<V> factory) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(factory);
        V value = map.get(key);
        if (value == null) {
            value = factory.get();
            Objects.requireNonNull(value, "factory returned null");
            map.put(key, value);
            modCount++;
        }
        return value;
    }

    //@ also
    //@ requires key != null;
    @Override
    public /*@ pure @*/ boolean containsKey(K key) {
        Objects.requireNonNull(key);
        return map.containsKey(key);
    }

    // ─── Modification ────────────────────────────────────────────────────────

    //@ also
    //@ requires key != null;
    //@ requires value != null;
    //@ ensures get(key) == value;
    //@ ensures containsKey(key);
    @Override
    public V put(K key, V value) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
        modCount++;
        return map.put(key, value);
    }

    //@ also
    //@ requires key != null;
    //@ ensures !containsKey(key);
    @Override
    public V remove(K key) {
        Objects.requireNonNull(key);
        V old = map.remove(key);
        if (old != null) {
            modCount++;
        }
        return old;
    }

    //@ also
    //@ ensures size() == 0;
    //@ ensures isEmpty();
    @Override
    public void clear() {
        if (!map.isEmpty()) {
            modCount++;
        }
        map.clear();
    }

    // ─── Size and status ─────────────────────────────────────────────────────

    //@ also
    //@ ensures \result >= 0;
    @Override
    public /*@ pure @*/ int size() {
        return map.size();
    }

    //@ also
    //@ ensures \result <==> (size() == 0);
    @Override
    public /*@ pure @*/ boolean isEmpty() {
        return map.isEmpty();
    }

    // ─── Views ───────────────────────────────────────────────────────────────

    @Override
    public Set<K> keySet() {
        return new FailFastKeySet();
    }

    @Override
    public Collection<V> values() {
        return new FailFastValues();
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return new FailFastEntrySet();
    }

    // ─── Object methods ──────────────────────────────────────────────────────

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof UnsynchronizedMultiMap)) {
            return false;
        }
        UnsynchronizedMultiMap<?, ?> other = (UnsynchronizedMultiMap<?, ?>) obj;
        return map.equals(other.map);
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    @Override
    public String toString() {
        return map.toString();
    }

    // ─── Package-private access for SynchronizedMultiMap ─────────────────────

    int getModCount() {
        return modCount;
    }

    // ─── Fail-fast view implementations ──────────────────────────────────────

    private class FailFastKeySet extends AbstractSet<K> {

        @Override
        public Iterator<K> iterator() {
            return new FailFastIterator<K>() {
                private final Iterator<K> delegate = map.keySet().iterator();

                @Override
                protected K advance() {
                    return delegate.next();
                }

                @Override
                protected boolean delegateHasNext() {
                    return delegate.hasNext();
                }
            };
        }

        @Override
        public int size() {
            return map.size();
        }

        @Override
        public boolean contains(Object o) {
            return map.containsKey(o);
        }
    }

    private class FailFastValues extends AbstractSet<V> {

        @Override
        public Iterator<V> iterator() {
            return new FailFastIterator<V>() {
                private final Iterator<V> delegate = map.values().iterator();

                @Override
                protected V advance() {
                    return delegate.next();
                }

                @Override
                protected boolean delegateHasNext() {
                    return delegate.hasNext();
                }
            };
        }

        @Override
        public int size() {
            return map.size();
        }

        @Override
        public boolean contains(Object o) {
            return map.containsValue(o);
        }
    }

    private class FailFastEntrySet extends AbstractSet<Map.Entry<K, V>> {

        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return new FailFastIterator<Map.Entry<K, V>>() {
                private final Iterator<Map.Entry<K, V>> delegate = map.entrySet().iterator();

                @Override
                protected Map.Entry<K, V> advance() {
                    return delegate.next();
                }

                @Override
                protected boolean delegateHasNext() {
                    return delegate.hasNext();
                }
            };
        }

        @Override
        public int size() {
            return map.size();
        }
    }

    private abstract class FailFastIterator<E> implements Iterator<E> {

        private final int expectedModCount = modCount;

        protected abstract E advance();

        protected abstract boolean delegateHasNext();

        @Override
        public boolean hasNext() {
            checkForComodification();
            return delegateHasNext();
        }

        @Override
        public E next() {
            checkForComodification();
            if (!delegateHasNext()) {
                throw new NoSuchElementException();
            }
            return advance();
        }

        private void checkForComodification() {
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
        }
    }
}
