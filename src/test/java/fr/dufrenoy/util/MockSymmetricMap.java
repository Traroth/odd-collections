/*
 * MockSymmetricMap.java
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Mock implementation of {@link SymmetricMap} for testing the interface contract
 * in isolation. Delegates to two {@link HashMap}s to maintain bijectivity.
 * Not intended for production use.
 */
class MockSymmetricMap<K, V> extends AbstractMap<K, V> implements SymmetricMap<K, V> {

    private final Map<K, V> forward = new HashMap<>();
    private final Map<V, K> reverse = new HashMap<>();

    // ─── SymmetricMap contract ────────────────────────────────────────────────

    @Override
    public Optional<K> getKey(Object value) {
        return Optional.ofNullable(reverse.get(value));
    }

    @Override
    public void safePut(K key, V value) {
        if (forward.containsKey(key)) {
            throw new IllegalArgumentException("Key already exists: " + key);
        }
        if (reverse.containsKey(value)) {
            throw new IllegalArgumentException("Value already exists: " + value);
        }
        forward.put(key, value);
        reverse.put(value, key);
    }

    @Override
    public Optional<K> removeByValue(Object value) {
        K key = reverse.remove(value);
        if (key != null) {
            forward.remove(key);
            return Optional.of(key);
        }
        return Optional.empty();
    }

    @Override
    public MockSymmetricMap<V, K> inverse() {
        MockSymmetricMap<V, K> inv = new MockSymmetricMap<>();
        inv.forward.putAll(reverse);
        inv.reverse.putAll(forward);
        return inv;
    }

    // ─── Map contract ─────────────────────────────────────────────────────────

    @Override
    public V put(K key, V value) {
        V oldValue = forward.remove(key);
        if (oldValue != null) {
            reverse.remove(oldValue);
        }
        K oldKey = reverse.remove(value);
        if (oldKey != null) {
            forward.remove(oldKey);
        }
        forward.put(key, value);
        reverse.put(value, key);
        return oldValue;
    }

    @Override
    public V remove(Object key) {
        V value = forward.remove(key);
        if (value != null) {
            reverse.remove(value);
        }
        return value;
    }

    @Override
    public void clear() {
        forward.clear();
        reverse.clear();
    }

    @Override
    public Set<K> keySet() {
        return new AbstractSet<K>() {
            @Override
            public Iterator<K> iterator() {
                return forward.keySet().iterator();
            }

            @Override
            public int size() {
                return forward.size();
            }

            @Override
            public boolean contains(Object o) {
                return forward.containsKey(o);
            }

            @Override
            public boolean remove(Object o) {
                if (forward.containsKey(o)) {
                    V value = forward.remove(o);
                    reverse.remove(value);
                    return true;
                }
                return false;
            }
        };
    }

    @Override
    public Set<V> values() {
        return new AbstractSet<V>() {
            @Override
            public Iterator<V> iterator() {
                return reverse.keySet().iterator();
            }

            @Override
            public int size() {
                return reverse.size();
            }

            @Override
            public boolean contains(Object o) {
                return reverse.containsKey(o);
            }

            @Override
            public boolean remove(Object o) {
                if (reverse.containsKey(o)) {
                    K key = reverse.remove(o);
                    forward.remove(key);
                    return true;
                }
                return false;
            }
        };
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return new AbstractSet<Map.Entry<K, V>>() {
            @Override
            public Iterator<Map.Entry<K, V>> iterator() {
                return new Iterator<Map.Entry<K, V>>() {
                    private final Iterator<Map.Entry<K, V>> it = forward.entrySet().iterator();

                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override
                    public Map.Entry<K, V> next() {
                        return new Map.Entry<K, V>() {
                            private final Map.Entry<K, V> entry = it.next();

                            @Override
                            public K getKey() {
                                return entry.getKey();
                            }

                            @Override
                            public V getValue() {
                                return entry.getValue();
                            }

                            @Override
                            public V setValue(V value) {
                                throw new UnsupportedOperationException();
                            }

                            @Override
                            public boolean equals(Object o) {
                                return entry.equals(o);
                            }

                            @Override
                            public int hashCode() {
                                return entry.hashCode();
                            }
                        };
                    }
                };
            }

            @Override
            public int size() {
                return forward.size();
            }

            @Override
            public boolean contains(Object o) {
                if (!(o instanceof Map.Entry)) return false;
                Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
                return Objects.equals(forward.get(e.getKey()), e.getValue());
            }

            @Override
            public boolean remove(Object o) {
                if (!(o instanceof Map.Entry)) return false;
                Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
                if (Objects.equals(forward.get(e.getKey()), e.getValue())) {
                    forward.remove(e.getKey());
                    reverse.remove(e.getValue());
                    return true;
                }
                return false;
            }
        };
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }
}
