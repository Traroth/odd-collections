/*
 * MockMultiMap.java
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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Mock implementation of {@link MultiMap} for testing the interface contract
 * in isolation. Delegates to a {@link HashMap}. Not intended for production use.
 */
class MockMultiMap<K, V> implements MultiMap<K, V> {

    private final HashMap<K, V> map = new HashMap<>();

    // ─── Lookup ──────────────────────────────────────────────────────────────

    @Override
    public V get(K key) {
        Objects.requireNonNull(key);
        return map.get(key);
    }

    @Override
    public Optional<V> getOpt(K key) {
        Objects.requireNonNull(key);
        return Optional.ofNullable(map.get(key));
    }

    @Override
    public V getOrCreate(K key, Supplier<V> factory) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(factory);
        V value = map.get(key);
        if (value == null) {
            value = factory.get();
            Objects.requireNonNull(value, "factory returned null");
            map.put(key, value);
        }
        return value;
    }

    @Override
    public boolean containsKey(K key) {
        Objects.requireNonNull(key);
        return map.containsKey(key);
    }

    // ─── Modification ────────────────────────────────────────────────────────

    @Override
    public V put(K key, V value) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
        return map.put(key, value);
    }

    @Override
    public V remove(K key) {
        Objects.requireNonNull(key);
        return map.remove(key);
    }

    @Override
    public void clear() {
        map.clear();
    }

    // ─── Size and status ─────────────────────────────────────────────────────

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    // ─── Views ───────────────────────────────────────────────────────────────

    @Override
    public Set<K> keySet() {
        return map.keySet();
    }

    @Override
    public Collection<V> values() {
        return map.values();
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return map.entrySet();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MockMultiMap)) return false;
        MockMultiMap<?, ?> other = (MockMultiMap<?, ?>) obj;
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
}
