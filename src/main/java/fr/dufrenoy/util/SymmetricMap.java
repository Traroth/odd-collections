/*
 * SymmetricMap.java
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

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A bijective map where both keys and values are unique.
 *
 * <p>In addition to the standard {@link Map} contract, this interface exposes
 * symmetric operations — lookup, removal, and insertion — that work equally
 * in both directions:
 * <ul>
 *   <li>{@link #get(Object)} — retrieve a value by key (inherited from {@link Map})</li>
 *   <li>{@link #getKey(Object)} — retrieve a key by value</li>
 *   <li>{@link #remove(Object)} — remove an entry by key (inherited from {@link Map})</li>
 *   <li>{@link #removeByValue(Object)} — remove an entry by value</li>
 *   <li>{@link #put(Object, Object)} — insert permissively (inherited from {@link Map})</li>
 *   <li>{@link #safePut(Object, Object)} — insert strictly</li>
 * </ul>
 *
 * <p>Because values are unique by definition, {@link #values()} returns a
 * {@link Set}{@code <V>} rather than a {@link java.util.Collection}{@code <V>}.
 *
 * <p>Two implementations are provided:
 * <ul>
 *   <li>{@link UnsynchronizedSymmetricMap} — not thread-safe.</li>
 *   <li>{@link SynchronizedSymmetricMap} — thread-safe, backed by a
 *       {@link java.util.concurrent.locks.ReentrantReadWriteLock}.</li>
 * </ul>
 *
 * @param <K> the type of keys
 * @param <V> the type of values
 */
public interface SymmetricMap<K, V> extends Map<K, V> {

    // ─── Symmetric lookups ────────────────────────────────────────────────────

    /**
     * Returns the key associated with the given value, or empty if not
     * found.
     *
     * @param value the value to look up
     * @return an {@code Optional} containing the key associated with {@code value},
     *         or empty if not found
     */
    /*@ pure @*/ Optional<K> getKey(Object value);

    // ─── Symmetric insertion ──────────────────────────────────────────────────

    /**
     * Associates the given key with the given value, throwing an exception if
     * the key or value already exists in this map.
     *
     * <p>Use {@link #put(Object, Object)} for a permissive insertion that
     * silently removes conflicting entries.
     *
     * @param key   the key
     * @param value the value
     * @throws IllegalArgumentException if the key or value already exists
     */
    void safePut(K key, V value);

    // ─── Symmetric removal ────────────────────────────────────────────────────

    /**
     * Removes the entry associated with the given value, if present.
     * Symmetric to {@link #remove(Object)} which removes by key.
     *
     * @param value the value whose entry is to be removed
     * @return an {@code Optional} containing the key that was associated with {@code value},
     *         or empty if not found
     */
    Optional<K> removeByValue(Object value);

    // ─── Inverse ──────────────────────────────────────────────────────────────

    /**
     * Returns an independent copy of this map with keys and values swapped.
     * Modifications to the returned map do not affect this map, and vice versa.
     *
     * <p>The returned map is of the same type as this map: an
     * {@link UnsynchronizedSymmetricMap} returns an
     * {@link UnsynchronizedSymmetricMap}, and a {@link SynchronizedSymmetricMap}
     * returns a {@link SynchronizedSymmetricMap}.
     *
     * @return a new {@code SymmetricMap<V, K>} with all entries inverted
     */
    SymmetricMap<V, K> inverse();

    // ─── Covariant views ──────────────────────────────────────────────────────

    /**
     * Returns a {@link Set} view of the values contained in this map.
     *
     * <p>This method narrows the return type of {@link Map#values()} from
     * {@link java.util.Collection} to {@link Set}, which is valid since values
     * in a bijective map are unique by definition.
     *
     * <p>The returned set is a live view of the map: changes to the map are
     * reflected in the set, and vice versa.
     *
     * @return a set view of the values contained in this map
     */
    @Override
    Set<V> values();
}