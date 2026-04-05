/*
 * UnsynchronizedMultiMapWhiteBoxTest.java
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

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * White-box tests for {@link UnsynchronizedMultiMap}:
 * - Internal {@code modCount} tracking
 * - Delegation integrity to internal {@code HashMap}
 * - Fail-fast iterator mechanics
 */
public class UnsynchronizedMultiMapWhiteBoxTest {

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private int getModCount(UnsynchronizedMultiMap<?, ?> map) {
        return map.getModCount();
    }

    @SuppressWarnings("unchecked")
    private <K, V> HashMap<K, V> getInternalMap(UnsynchronizedMultiMap<K, V> map)
            throws Exception {
        Field field = UnsynchronizedMultiMap.class.getDeclaredField("map");
        field.setAccessible(true);
        return (HashMap<K, V>) field.get(map);
    }

    // ─── modCount tracking ───────────────────────────────────────────────────

    @Test
    public void testModCount_InitiallyZero() {
        UnsynchronizedMultiMap<String, Integer> map = new UnsynchronizedMultiMap<>();
        assertEquals(0, getModCount(map));
    }

    @Test
    public void testModCount_IncrementedOnPut() {
        UnsynchronizedMultiMap<String, Integer> map = new UnsynchronizedMultiMap<>();
        map.put("a", 1);
        assertEquals(1, getModCount(map));
    }

    @Test
    public void testModCount_IncrementedOnEachPut() {
        UnsynchronizedMultiMap<String, Integer> map = new UnsynchronizedMultiMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(2, getModCount(map));
    }

    @Test
    public void testModCount_IncrementedOnOverwrite() {
        UnsynchronizedMultiMap<String, Integer> map = new UnsynchronizedMultiMap<>();
        map.put("a", 1);
        map.put("a", 2);
        assertEquals(2, getModCount(map));
    }

    @Test
    public void testModCount_IncrementedOnRemove() {
        UnsynchronizedMultiMap<String, Integer> map = new UnsynchronizedMultiMap<>();
        map.put("a", 1);
        int before = getModCount(map);
        map.remove("a");
        assertEquals(before + 1, getModCount(map));
    }

    @Test
    public void testModCount_NotIncrementedOnRemoveAbsentKey() {
        UnsynchronizedMultiMap<String, Integer> map = new UnsynchronizedMultiMap<>();
        map.put("a", 1);
        int before = getModCount(map);
        map.remove("missing");
        assertEquals(before, getModCount(map));
    }

    @Test
    public void testModCount_IncrementedOnClear() {
        UnsynchronizedMultiMap<String, Integer> map = new UnsynchronizedMultiMap<>();
        map.put("a", 1);
        int before = getModCount(map);
        map.clear();
        assertEquals(before + 1, getModCount(map));
    }

    @Test
    public void testModCount_NotIncrementedOnClearEmpty() {
        UnsynchronizedMultiMap<String, Integer> map = new UnsynchronizedMultiMap<>();
        int before = getModCount(map);
        map.clear();
        assertEquals(before, getModCount(map));
    }

    @Test
    public void testModCount_IncrementedOnGetOrCreate_NewKey() {
        UnsynchronizedMultiMap<String, Integer> map = new UnsynchronizedMultiMap<>();
        int before = getModCount(map);
        map.getOrCreate("a", () -> 1);
        assertEquals(before + 1, getModCount(map));
    }

    @Test
    public void testModCount_NotIncrementedOnGetOrCreate_ExistingKey() {
        UnsynchronizedMultiMap<String, Integer> map = new UnsynchronizedMultiMap<>();
        map.put("a", 1);
        int before = getModCount(map);
        map.getOrCreate("a", () -> 2);
        assertEquals(before, getModCount(map));
    }

    // ─── Delegation integrity ────────────────────────────────────────────────

    @Test
    public void testInternalMap_ReflectsSize() throws Exception {
        UnsynchronizedMultiMap<String, Integer> map = new UnsynchronizedMultiMap<>();
        map.put("a", 1);
        map.put("b", 2);
        HashMap<String, Integer> internal = getInternalMap(map);
        assertEquals(2, internal.size());
        assertEquals(map.size(), internal.size());
    }

    @Test
    public void testInternalMap_ReflectsContent() throws Exception {
        UnsynchronizedMultiMap<String, Integer> map = new UnsynchronizedMultiMap<>();
        map.put("a", 1);
        map.put("b", 2);
        HashMap<String, Integer> internal = getInternalMap(map);
        assertEquals(Integer.valueOf(1), internal.get("a"));
        assertEquals(Integer.valueOf(2), internal.get("b"));
    }

    @Test
    public void testInternalMap_EmptyAfterClear() throws Exception {
        UnsynchronizedMultiMap<String, Integer> map = new UnsynchronizedMultiMap<>();
        map.put("a", 1);
        map.clear();
        HashMap<String, Integer> internal = getInternalMap(map);
        assertTrue(internal.isEmpty());
    }

    // ─── Fail-fast iterator: next() also checks ──────────────────────────────

    @Test
    public void testFailFastIterator_NextThrowsCME() {
        UnsynchronizedMultiMap<String, Integer> map = new UnsynchronizedMultiMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Iterator<String> it = map.keySet().iterator();
        it.next();
        map.put("c", 3);
        assertThrows(ConcurrentModificationException.class, it::next);
    }

    @Test
    public void testFailFastIterator_ValuesNextThrowsCME() {
        UnsynchronizedMultiMap<String, Integer> map = new UnsynchronizedMultiMap<>();
        map.put("a", 1);
        Iterator<Integer> it = map.values().iterator();
        map.remove("a");
        assertThrows(ConcurrentModificationException.class, it::hasNext);
    }

    @Test
    public void testFailFastIterator_EntrySetNextThrowsCME() {
        UnsynchronizedMultiMap<String, Integer> map = new UnsynchronizedMultiMap<>();
        map.put("a", 1);
        Iterator<?> it = map.entrySet().iterator();
        map.put("b", 2);
        assertThrows(ConcurrentModificationException.class, it::next);
    }

    // ─── Constructor from Map copies data ────────────────────────────────────

    @Test
    public void testConstructor_FromMap_CopiesIntoInternalMap() throws Exception {
        HashMap<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);
        UnsynchronizedMultiMap<String, Integer> map = new UnsynchronizedMultiMap<>(source);
        HashMap<String, Integer> internal = getInternalMap(map);
        assertEquals(2, internal.size());
        // Modifying source does not affect internal map
        source.put("c", 3);
        assertEquals(2, internal.size());
    }
}
