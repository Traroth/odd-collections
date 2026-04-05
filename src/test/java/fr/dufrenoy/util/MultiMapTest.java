/*
 * MultiMapTest.java
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

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Interface contract tests for {@link MultiMap}, using a {@link MockMultiMap}
 * implementation. Tests the contract in isolation, without knowledge of any real
 * implementation.
 */
public class MultiMapTest {

    // ─── Empty map ───────────────────────────────────────────────────────────

    @Test
    public void testNewMap_IsEmpty() {
        MultiMap<String, Integer> map = new MockMultiMap<>();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    // ─── put / get ───────────────────────────────────────────────────────────

    @Test
    public void testPut_NewEntry_ReturnsNull() {
        MultiMap<String, Integer> map = new MockMultiMap<>();
        assertNull(map.put("a", 1));
    }

    @Test
    public void testPut_NewEntry_SizeIncreases() {
        MultiMap<String, Integer> map = new MockMultiMap<>();
        map.put("a", 1);
        assertEquals(1, map.size());
        assertFalse(map.isEmpty());
    }

    @Test
    public void testPut_ExistingKey_ReturnsPreviousValue() {
        MultiMap<String, Integer> map = new MockMultiMap<>();
        map.put("a", 1);
        assertEquals(1, map.put("a", 2));
    }

    @Test
    public void testPut_ExistingKey_UpdatesValue() {
        MultiMap<String, Integer> map = new MockMultiMap<>();
        map.put("a", 1);
        map.put("a", 2);
        assertEquals(2, map.get("a"));
    }

    @Test
    public void testPut_ExistingKey_SizeUnchanged() {
        MultiMap<String, Integer> map = new MockMultiMap<>();
        map.put("a", 1);
        map.put("a", 2);
        assertEquals(1, map.size());
    }

    @Test
    public void testGet_ExistingKey_ReturnsValue() {
        MultiMap<String, Integer> map = new MockMultiMap<>();
        map.put("a", 1);
        assertEquals(1, map.get("a"));
    }

    @Test
    public void testGet_AbsentKey_ReturnsNull() {
        MultiMap<String, Integer> map = new MockMultiMap<>();
        assertNull(map.get("missing"));
    }

    // ─── getOpt ──────────────────────────────────────────────────────────────

    @Test
    public void testGetOpt_ExistingKey_ReturnsPresent() {
        MultiMap<String, Integer> map = new MockMultiMap<>();
        map.put("a", 1);
        Optional<Integer> result = map.getOpt("a");
        assertTrue(result.isPresent());
        assertEquals(1, result.get());
    }

    @Test
    public void testGetOpt_AbsentKey_ReturnsEmpty() {
        MultiMap<String, Integer> map = new MockMultiMap<>();
        assertFalse(map.getOpt("missing").isPresent());
    }

    // ─── getOrCreate ─────────────────────────────────────────────────────────

    @Test
    public void testGetOrCreate_AbsentKey_CreatesAndReturnsNewValue() {
        MultiMap<String, Integer> map = new MockMultiMap<>();
        Integer result = map.getOrCreate("a", () -> 42);
        assertEquals(42, result);
        assertEquals(42, map.get("a"));
    }

    @Test
    public void testGetOrCreate_ExistingKey_ReturnsExistingValue() {
        MultiMap<String, Integer> map = new MockMultiMap<>();
        map.put("a", 1);
        Integer result = map.getOrCreate("a", () -> 42);
        assertEquals(1, result);
    }

    @Test
    public void testGetOrCreate_AbsentKey_IncreasesSize() {
        MultiMap<String, Integer> map = new MockMultiMap<>();
        map.getOrCreate("a", () -> 42);
        assertEquals(1, map.size());
    }

    @Test
    public void testGetOrCreate_ExistingKey_SizeUnchanged() {
        MultiMap<String, Integer> map = new MockMultiMap<>();
        map.put("a", 1);
        map.getOrCreate("a", () -> 42);
        assertEquals(1, map.size());
    }

    // ─── containsKey ─────────────────────────────────────────────────────────

    @Test
    public void testContainsKey_Present_ReturnsTrue() {
        MultiMap<String, Integer> map = new MockMultiMap<>();
        map.put("a", 1);
        assertTrue(map.containsKey("a"));
    }

    @Test
    public void testContainsKey_Absent_ReturnsFalse() {
        MultiMap<String, Integer> map = new MockMultiMap<>();
        assertFalse(map.containsKey("missing"));
    }

    // ─── remove ──────────────────────────────────────────────────────────────

    @Test
    public void testRemove_ExistingKey_ReturnsPreviousValue() {
        MultiMap<String, Integer> map = new MockMultiMap<>();
        map.put("a", 1);
        assertEquals(1, map.remove("a"));
    }

    @Test
    public void testRemove_ExistingKey_DecreasesSize() {
        MultiMap<String, Integer> map = new MockMultiMap<>();
        map.put("a", 1);
        map.remove("a");
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
    }

    @Test
    public void testRemove_ExistingKey_GetReturnsNull() {
        MultiMap<String, Integer> map = new MockMultiMap<>();
        map.put("a", 1);
        map.remove("a");
        assertNull(map.get("a"));
    }

    @Test
    public void testRemove_AbsentKey_ReturnsNull() {
        MultiMap<String, Integer> map = new MockMultiMap<>();
        assertNull(map.remove("missing"));
    }

    // ─── clear ───────────────────────────────────────────────────────────────

    @Test
    public void testClear_NonEmptyMap_BecomesEmpty() {
        MultiMap<String, Integer> map = new MockMultiMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testClear_NonEmptyMap_GetReturnsNull() {
        MultiMap<String, Integer> map = new MockMultiMap<>();
        map.put("a", 1);
        map.clear();
        assertNull(map.get("a"));
    }

    // ─── Views ───────────────────────────────────────────────────────────────

    @Test
    public void testKeySet_ReflectsEntries() {
        MultiMap<String, Integer> map = new MockMultiMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Set<String> keys = map.keySet();
        assertEquals(2, keys.size());
        assertTrue(keys.contains("a"));
        assertTrue(keys.contains("b"));
    }

    @Test
    public void testValues_ReflectsEntries() {
        MultiMap<String, Integer> map = new MockMultiMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(2, map.values().size());
        assertTrue(map.values().contains(1));
        assertTrue(map.values().contains(2));
    }

    @Test
    public void testEntrySet_ReflectsEntries() {
        MultiMap<String, Integer> map = new MockMultiMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Set<Map.Entry<String, Integer>> entries = map.entrySet();
        assertEquals(2, entries.size());
    }

    // ─── Null rejection ──────────────────────────────────────────────────────

    @Test
    public void testGet_NullKey_ThrowsNPE() {
        MultiMap<String, Integer> map = new MockMultiMap<>();
        assertThrows(NullPointerException.class, () -> map.get(null));
    }

    @Test
    public void testGetOpt_NullKey_ThrowsNPE() {
        MultiMap<String, Integer> map = new MockMultiMap<>();
        assertThrows(NullPointerException.class, () -> map.getOpt(null));
    }

    @Test
    public void testGetOrCreate_NullKey_ThrowsNPE() {
        MultiMap<String, Integer> map = new MockMultiMap<>();
        assertThrows(NullPointerException.class, () -> map.getOrCreate(null, () -> 1));
    }

    @Test
    public void testGetOrCreate_NullFactory_ThrowsNPE() {
        MultiMap<String, Integer> map = new MockMultiMap<>();
        assertThrows(NullPointerException.class, () -> map.getOrCreate("a", null));
    }

    @Test
    public void testGetOrCreate_FactoryReturnsNull_ThrowsNPE() {
        MultiMap<String, Integer> map = new MockMultiMap<>();
        assertThrows(NullPointerException.class, () -> map.getOrCreate("a", () -> null));
    }

    @Test
    public void testPut_NullKey_ThrowsNPE() {
        MultiMap<String, Integer> map = new MockMultiMap<>();
        assertThrows(NullPointerException.class, () -> map.put(null, 1));
    }

    @Test
    public void testPut_NullValue_ThrowsNPE() {
        MultiMap<String, Integer> map = new MockMultiMap<>();
        assertThrows(NullPointerException.class, () -> map.put("a", null));
    }

    @Test
    public void testRemove_NullKey_ThrowsNPE() {
        MultiMap<String, Integer> map = new MockMultiMap<>();
        assertThrows(NullPointerException.class, () -> map.remove(null));
    }

    @Test
    public void testContainsKey_NullKey_ThrowsNPE() {
        MultiMap<String, Integer> map = new MockMultiMap<>();
        assertThrows(NullPointerException.class, () -> map.containsKey(null));
    }

    // ─── Multi-level recursive usage ─────────────────────────────────────────

    @Test
    public void testRecursive_TwoLevels_PutAndGet() {
        MultiMap<String, MultiMap<String, Integer>> outer = new MockMultiMap<>();
        MultiMap<String, Integer> inner = new MockMultiMap<>();
        inner.put("x", 42);
        outer.put("a", inner);
        assertEquals(42, outer.get("a").get("x"));
    }

    @Test
    public void testRecursive_TwoLevels_GetOrCreate() {
        MultiMap<String, MultiMap<String, Integer>> outer = new MockMultiMap<>();
        outer.getOrCreate("a", MockMultiMap::new).put("x", 42);
        assertEquals(42, outer.get("a").get("x"));
    }

    @Test
    public void testRecursive_ThreeLevels_ChainedWrite() {
        MultiMap<String, MultiMap<String, MultiMap<String, Integer>>> map = new MockMultiMap<>();
        map.getOrCreate("France", MockMultiMap::new)
           .getOrCreate("Paris", MockMultiMap::new)
           .put("Q1", 42);
        assertEquals(42, map.get("France").get("Paris").get("Q1"));
    }

    @Test
    public void testRecursive_ThreeLevels_SafeRead() {
        MultiMap<String, MultiMap<String, MultiMap<String, Integer>>> map = new MockMultiMap<>();
        map.getOrCreate("France", MockMultiMap::new)
           .getOrCreate("Paris", MockMultiMap::new)
           .put("Q1", 42);

        Optional<Integer> result = map.getOpt("France")
            .flatMap(m -> m.getOpt("Paris"))
            .flatMap(m -> m.getOpt("Q1"));

        assertTrue(result.isPresent());
        assertEquals(42, result.get());
    }

    @Test
    public void testRecursive_ThreeLevels_SafeRead_AbsentKey() {
        MultiMap<String, MultiMap<String, MultiMap<String, Integer>>> map = new MockMultiMap<>();

        Optional<Integer> result = map.getOpt("France")
            .flatMap(m -> m.getOpt("Paris"))
            .flatMap(m -> m.getOpt("Q1"));

        assertFalse(result.isPresent());
    }

    // ─── Multiple entries ────────────────────────────────────────────────────

    @Test
    public void testMultipleEntries_IndependentKeys() {
        MultiMap<String, Integer> map = new MockMultiMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertEquals(3, map.size());
        assertEquals(1, map.get("a"));
        assertEquals(2, map.get("b"));
        assertEquals(3, map.get("c"));
    }

    @Test
    public void testMultipleEntries_RemoveOne_OthersUnchanged() {
        MultiMap<String, Integer> map = new MockMultiMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.remove("b");
        assertEquals(2, map.size());
        assertEquals(1, map.get("a"));
        assertNull(map.get("b"));
        assertEquals(3, map.get("c"));
    }
}
