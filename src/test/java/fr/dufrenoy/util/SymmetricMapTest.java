/*
 * SymmetricMapTest.java
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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Interface contract tests for {@link SymmetricMap}, using a {@link MockSymmetricMap}
 * implementation. Tests the contract in isolation, without knowledge of any real
 * implementation.
 */
public class SymmetricMapTest {

    // ─── Constructors ─────────────────────────────────────────────────────────

    @Test
    public void testDefaultConstructor_EmptyMap() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    // ─── put / get ────────────────────────────────────────────────────────────

    @Test
    public void testPut_NewEntry_ReturnsNull() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        assertNull(map.put("a", 1));
    }

    @Test
    public void testPut_NewEntry_SizeIncreases() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        map.put("a", 1);
        assertEquals(1, map.size());
    }

    @Test
    public void testPut_ExistingKey_ReturnsPreviousValue() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        map.put("a", 1);
        assertEquals(1, map.put("a", 2));
    }

    @Test
    public void testPut_ExistingKey_UpdatesValue() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        map.put("a", 1);
        map.put("a", 2);
        assertEquals(2, map.get("a"));
    }

    @Test
    public void testPut_ExistingKey_SizeUnchanged() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        map.put("a", 1);
        map.put("a", 2);
        assertEquals(1, map.size());
    }

    @Test
    public void testPut_ExistingValue_RemovesConflictingEntry() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        map.put("a", 1);
        map.put("b", 1); // value 1 already used by "a"
        assertFalse(map.containsKey("a"));
        assertEquals(1, map.get("b"));
        assertEquals(1, map.size());
    }

    @Test
    public void testPut_SameKeyAndValue_IsNoOp() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        map.put("a", 1);
        Integer returned = map.put("a", 1);
        assertEquals(1, returned);
        assertEquals(1, map.size());
        assertEquals(1, map.get("a"));
    }

    @Test
    public void testGet_ExistingKey_ReturnsValue() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        map.put("a", 1);
        assertEquals(1, map.get("a"));
    }

    @Test
    public void testGet_AbsentKey_ReturnsNull() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        assertNull(map.get("absent"));
    }

    // ─── getKey ───────────────────────────────────────────────────────────────

    @Test
    public void testGetKey_ExistingValue_ReturnsKey() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        map.put("a", 1);
        assertEquals("a", map.getKey(1).get());
    }

    @Test
    public void testGetKey_AbsentValue_ReturnsEmpty() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        assertFalse(map.getKey(99).isPresent());
    }

    @Test
    public void testGetKey_SymmetricWithGet() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            assertEquals(e.getKey(), map.getKey(e.getValue()).get());
            assertEquals(e.getValue(), map.get(e.getKey()));
        }
    }

    // ─── containsKey / containsValue ──────────────────────────────────────────

    @Test
    public void testContainsKey_Present() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        map.put("a", 1);
        assertTrue(map.containsKey("a"));
    }

    @Test
    public void testContainsKey_Absent() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        assertFalse(map.containsKey("a"));
    }

    @Test
    public void testContainsValue_Present() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        map.put("a", 1);
        assertTrue(map.containsValue(1));
    }

    @Test
    public void testContainsValue_Absent() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        assertFalse(map.containsValue(1));
    }

    // ─── remove / removeByValue ───────────────────────────────────────────────

    @Test
    public void testRemove_ExistingKey_ReturnsValue() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        map.put("a", 1);
        assertEquals(1, map.remove("a"));
    }

    @Test
    public void testRemove_ExistingKey_SizeDecreases() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        map.put("a", 1);
        map.remove("a");
        assertEquals(0, map.size());
    }

    @Test
    public void testRemove_ExistingKey_EntryGone() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        map.put("a", 1);
        map.remove("a");
        assertFalse(map.containsKey("a"));
        assertFalse(map.containsValue(1));
    }

    @Test
    public void testRemove_AbsentKey_ReturnsNull() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        assertNull(map.remove("absent"));
    }

    @Test
    public void testRemoveByValue_ExistingValue_ReturnsKey() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        map.put("a", 1);
        assertEquals("a", map.removeByValue(1).get());
    }

    @Test
    public void testRemoveByValue_ExistingValue_EntryGone() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        map.put("a", 1);
        map.removeByValue(1);
        assertFalse(map.containsKey("a"));
        assertFalse(map.containsValue(1));
    }

    @Test
    public void testRemoveByValue_AbsentValue_ReturnsEmpty() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        assertFalse(map.removeByValue(99).isPresent());
    }

    @Test
    public void testRemoveByValue_SymmetricWithRemove() {
        SymmetricMap<String, Integer> map1 = new MockSymmetricMap<>();
        map1.put("a", 1);
        map1.put("b", 2);
        map1.remove("a");

        SymmetricMap<String, Integer> map2 = new MockSymmetricMap<>();
        map2.put("a", 1);
        map2.put("b", 2);
        map2.removeByValue(1);

        assertEquals(map1, map2);
    }

    // ─── safePut ──────────────────────────────────────────────────────────────

    @Test
    public void testSafePut_NewEntry_Succeeds() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        map.safePut("a", 1);
        assertEquals(1, map.get("a"));
    }

    @Test
    public void testSafePut_DuplicateKey_Throws() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        map.put("a", 1);
        assertThrows(IllegalArgumentException.class, () -> map.safePut("a", 2));
    }

    @Test
    public void testSafePut_DuplicateValue_Throws() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        map.put("a", 1);
        assertThrows(IllegalArgumentException.class, () -> map.safePut("b", 1));
    }

    @Test
    public void testSafePut_DuplicateKey_MapUnchanged() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        map.put("a", 1);
        try { map.safePut("a", 2); } catch (IllegalArgumentException ignored) {}
        assertEquals(1, map.get("a"));
        assertEquals(1, map.size());
    }

    // ─── inverse ──────────────────────────────────────────────────────────────

    @Test
    public void testInverse_SwapsKeysAndValues() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        map.put("a", 1);
        map.put("b", 2);
        SymmetricMap<Integer, String> inv = map.inverse();
        assertEquals("a", inv.get(1));
        assertEquals("b", inv.get(2));
    }

    @Test
    public void testInverse_SameSize() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(map.size(), map.inverse().size());
    }

    @Test
    public void testInverse_IsIndependent() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        map.put("a", 1);
        SymmetricMap<Integer, String> inv = map.inverse();
        map.put("b", 2);
        assertFalse(inv.containsKey(2));
    }

    @Test
    public void testInverse_OfInverse_EqualToOriginal() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(map, map.inverse().inverse());
    }

    @Test
    public void testInverse_ReturnsDifferentInstance() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        map.put("a", 1);
        assertNotSame(map, map.inverse().inverse());
    }

    // ─── clear ────────────────────────────────────────────────────────────────

    @Test
    public void testClear_EmptiesMap() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testClear_RemovesAllKeys() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        map.put("a", 1);
        map.clear();
        assertFalse(map.containsKey("a"));
        assertFalse(map.containsValue(1));
    }

    // ─── keySet ───────────────────────────────────────────────────────────────

    @Test
    public void testKeySet_ContainsAllKeys() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertTrue(map.keySet().contains("a"));
        assertTrue(map.keySet().contains("b"));
    }

    @Test
    public void testKeySet_Size() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(2, map.keySet().size());
    }

    @Test
    public void testKeySet_Remove_UpdatesMap() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        map.put("a", 1);
        map.keySet().remove("a");
        assertFalse(map.containsKey("a"));
        assertFalse(map.containsValue(1));
    }

    @Test
    public void testKeySet_IsLiveView() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        map.put("a", 1);
        Set<String> keys = map.keySet();
        map.put("b", 2);
        assertTrue(keys.contains("b"));
    }

    // ─── values ───────────────────────────────────────────────────────────────

    @Test
    public void testValues_ReturnsSet() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        map.put("a", 1);
        assertTrue(map.values() instanceof Set);
    }

    @Test
    public void testValues_ContainsAllValues() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertTrue(map.values().contains(1));
        assertTrue(map.values().contains(2));
    }

    @Test
    public void testValues_Remove_UpdatesMap() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        map.put("a", 1);
        map.values().remove(1);
        assertFalse(map.containsValue(1));
        assertFalse(map.containsKey("a"));
    }

    @Test
    public void testValues_IsLiveView() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        map.put("a", 1);
        Set<Integer> values = map.values();
        map.put("b", 2);
        assertTrue(values.contains(2));
    }

    // ─── entrySet ─────────────────────────────────────────────────────────────

    @Test
    public void testEntrySet_Size() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(2, map.entrySet().size());
    }

    @Test
    public void testEntrySet_ContainsEntry() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        map.put("a", 1);
        Map.Entry<String, Integer> entry = Map.entry("a", 1);
        assertTrue(map.entrySet().contains(entry));
    }

    @Test
    public void testEntrySet_Remove_UpdatesMap() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        map.put("a", 1);
        map.entrySet().remove(Map.entry("a", 1));
        assertFalse(map.containsKey("a"));
    }

    // ─── Views consistency ────────────────────────────────────────────────────

    @Test
    public void testViews_MutualConsistency() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        // The i-th key in keySet() corresponds to the i-th value in values()
        // and the i-th entry in entrySet() — verify by collecting all three
        Set<String> keys = new HashSet<>(map.keySet());
        Set<Integer> values = new HashSet<>(map.values());
        Set<Map.Entry<String, Integer>> entries = new HashSet<>(map.entrySet());

        assertEquals(keys.size(), values.size());
        assertEquals(keys.size(), entries.size());
        for (Map.Entry<String, Integer> e : entries) {
            assertTrue(keys.contains(e.getKey()));
            assertTrue(values.contains(e.getValue()));
        }
    }

    // ─── Null handling ────────────────────────────────────────────────────────

    @Test
    public void testPut_NullKey() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        map.put(null, 1);
        assertTrue(map.containsKey(null));
        assertEquals(1, map.get(null));
    }

    @Test
    public void testPut_NullValue() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        map.put("a", null);
        assertTrue(map.containsValue(null));
        assertNull(map.get("a"));
    }

    @Test
    public void testPut_NullKeyAndValue() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        map.put(null, null);
        assertTrue(map.containsKey(null));
        assertTrue(map.containsValue(null));
    }

    @Test
    public void testGetKey_NullValue() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        map.put("a", null);
        assertEquals("a", map.getKey(null).get());
    }

    @Test
    public void testRemoveByValue_NullValue() {
        SymmetricMap<String, Integer> map = new MockSymmetricMap<>();
        map.put("a", null);
        assertEquals("a", map.removeByValue(null).get());
        assertFalse(map.containsKey("a"));
    }

    // ─── equals / hashCode ────────────────────────────────────────────────────

    @Test
    public void testEquals_SameEntries() {
        SymmetricMap<String, Integer> map1 = new MockSymmetricMap<>();
        map1.put("a", 1);
        map1.put("b", 2);

        SymmetricMap<String, Integer> map2 = new MockSymmetricMap<>();
        map2.put("a", 1);
        map2.put("b", 2);

        assertEquals(map1, map2);
    }

    @Test
    public void testHashCode_EqualMaps_SameHashCode() {
        SymmetricMap<String, Integer> map1 = new MockSymmetricMap<>();
        map1.put("a", 1);

        SymmetricMap<String, Integer> map2 = new MockSymmetricMap<>();
        map2.put("a", 1);

        assertEquals(map1.hashCode(), map2.hashCode());
    }
}
