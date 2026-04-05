/*
 * MultiMap.java
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

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A recursive multi-dimensional map. Each {@code MultiMap<K, V>} associates
 * keys of type {@code K} to values of type {@code V}, where {@code V} may
 * itself be another {@code MultiMap}, enabling multi-level key hierarchies
 * with heterogeneous key types per dimension.
 *
 * <p>A partial lookup (stopping before the deepest level) returns a sub-map
 * of reduced dimensionality. A complete lookup returns the terminal value.
 *
 * <p>Example with three levels:
 * <pre>{@code
 * MultiMap<String, MultiMap<String, MultiMap<String, Integer>>> map = ...;
 *
 * // Chained write
 * map.getOrCreate("France", UnsynchronizedMultiMap::new)
 *    .getOrCreate("Paris", UnsynchronizedMultiMap::new)
 *    .put("Q1", 42);
 *
 * // Chained read (nullable)
 * Integer val = map.get("France").get("Paris").get("Q1");
 *
 * // Safe read
 * Optional<Integer> safe = map.getOpt("France")
 *     .flatMap(m -> m.getOpt("Paris"))
 *     .flatMap(m -> m.getOpt("Q1"));
 * }</pre>
 *
 * <p>This interface does not extend {@link java.util.Map} because the
 * recursive semantics (partial lookups, {@code getOrCreate}) are incompatible
 * with the full {@code Map} contract. However, the API intentionally mirrors
 * {@code Map} where applicable.
 *
 * <p>Null keys and null values are forbidden. Implementations must reject
 * them with {@link NullPointerException}.
 *
 * <p>Two implementations are provided:
 * <ul>
 *   <li>{@link UnsynchronizedMultiMap} — not thread-safe, fail-fast iterators</li>
 *   <li>{@link SynchronizedMultiMap} — thread-safe, snapshot-based iterators</li>
 * </ul>
 *
 * @param <K> the type of keys at this level
 * @param <V> the type of values at this level (may be another {@code MultiMap})
 *
 * @see UnsynchronizedMultiMap
 * @see SynchronizedMultiMap
 */
public interface MultiMap<K, V> {

    // ─── Lookup ──────────────────────────────────────────────────────────────

    /**
     * Returns the value associated with the specified key, or {@code null} if
     * no mapping exists. This method is designed for chained lookups across
     * multiple levels.
     *
     * @param key the key whose associated value is to be returned
     * @return the value associated with {@code key}, or {@code null}
     * @throws NullPointerException if {@code key} is {@code null}
     */
    V get(K key);

    /**
     * Returns the value associated with the specified key wrapped in an
     * {@link Optional}, or an empty {@code Optional} if no mapping exists.
     *
     * @param key the key whose associated value is to be returned
     * @return an {@code Optional} containing the value, or empty
     * @throws NullPointerException if {@code key} is {@code null}
     */
    /*@ pure @*/ Optional<V> getOpt(K key);

    /**
     * Returns the value associated with the specified key. If no mapping
     * exists, creates one using the given factory, inserts it, and returns
     * the new value. This method is designed for chained writes across
     * multiple levels.
     *
     * @param key     the key whose associated value is to be returned or created
     * @param factory the supplier used to create a new value if absent
     * @return the existing or newly created value
     * @throws NullPointerException if {@code key} or {@code factory} is
     *                              {@code null}, or if the factory returns
     *                              {@code null}
     */
    V getOrCreate(K key, Supplier<V> factory);

    /**
     * Returns {@code true} if this map contains a mapping for the specified key.
     *
     * @param key the key whose presence is to be tested
     * @return {@code true} if this map contains a mapping for {@code key}
     * @throws NullPointerException if {@code key} is {@code null}
     */
    /*@ pure @*/ boolean containsKey(K key);

    // ─── Modification ────────────────────────────────────────────────────────

    /**
     * Associates the specified value with the specified key. If a mapping
     * already exists for this key, the old value is replaced.
     *
     * @param key   the key with which the value is to be associated
     * @param value the value to associate
     * @return the previous value associated with {@code key}, or {@code null}
     *         if there was no mapping
     * @throws NullPointerException if {@code key} or {@code value} is {@code null}
     */
    V put(K key, V value);

    /**
     * Removes the mapping for the specified key, if present.
     *
     * @param key the key whose mapping is to be removed
     * @return the previous value associated with {@code key}, or {@code null}
     *         if there was no mapping
     * @throws NullPointerException if {@code key} is {@code null}
     */
    V remove(K key);

    /**
     * Removes all mappings from this map.
     */
    void clear();

    // ─── Size and status ─────────────────────────────────────────────────────

    /**
     * Returns the number of key-value mappings at this level.
     *
     * @return the number of mappings
     */
    /*@ pure @*/ int size();

    /**
     * Returns {@code true} if this map contains no mappings.
     *
     * @return {@code true} if empty
     */
    /*@ pure @*/ boolean isEmpty();

    // ─── Views ───────────────────────────────────────────────────────────────

    /**
     * Returns a {@link Set} view of the keys contained in this map.
     *
     * @return a set view of the keys
     */
    Set<K> keySet();

    /**
     * Returns a {@link Collection} view of the values contained in this map.
     *
     * @return a collection view of the values
     */
    Collection<V> values();

    /**
     * Returns a {@link Set} view of the mappings contained in this map.
     * Each element is a {@link Map.Entry Map.Entry&lt;K, V&gt;}.
     *
     * @return a set view of the mappings
     */
    Set<Map.Entry<K, V>> entrySet();
}
