/*
 * UnsynchronizedSymmetricMapBlackBoxTest.java
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Black-box tests for {@link SymmetricMap}, based solely on the public contract
 * and Javadoc. No knowledge of the internal implementation is assumed.
 */
public class UnsynchronizedSymmetricMapBlackBoxTest {

    // ─── Constructors ─────────────────────────────────────────────────────────

    @Test
    public void testDefaultConstructor_EmptyMap() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testConstructor_InitialCapacity() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>(32);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testConstructor_InitialCapacityAndLoadFactor() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>(32, 0.5f);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testConstructor_InvalidCapacity_Zero() {
        assertThrows(IllegalArgumentException.class, () -> new UnsynchronizedSymmetricMap<>(0));
    }

    @Test
    public void testConstructor_InvalidCapacity_Negative() {
        assertThrows(IllegalArgumentException.class, () -> new UnsynchronizedSymmetricMap<>(-1));
    }

    @Test
    public void testConstructor_InvalidLoadFactor_Zero() {
        assertThrows(IllegalArgumentException.class, () -> new UnsynchronizedSymmetricMap<>(16, 0f));
    }

    @Test
    public void testConstructor_InvalidLoadFactor_Negative() {
        assertThrows(IllegalArgumentException.class, () -> new UnsynchronizedSymmetricMap<>(16, -0.5f));
    }

    @Test
    public void testConstructor_InvalidLoadFactor_NaN() {
        assertThrows(IllegalArgumentException.class, () -> new UnsynchronizedSymmetricMap<>(16, Float.NaN));
    }

    // ─── put / get ────────────────────────────────────────────────────────────

    @Test
    public void testPut_NewEntry_ReturnsNull() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        assertNull(map.put("a", 1));
    }

    @Test
    public void testPut_NewEntry_SizeIncreases() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        assertEquals(1, map.size());
    }

    @Test
    public void testPut_ExistingKey_ReturnsPreviousValue() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        assertEquals(1, map.put("a", 2));
    }

    @Test
    public void testPut_ExistingKey_UpdatesValue() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        map.put("a", 2);
        assertEquals(2, map.get("a"));
    }

    @Test
    public void testPut_ExistingKey_SizeUnchanged() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        map.put("a", 2);
        assertEquals(1, map.size());
    }

    @Test
    public void testPut_ExistingValue_RemovesConflictingEntry() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        map.put("b", 1); // value 1 already used by "a"
        assertFalse(map.containsKey("a"));
        assertEquals(1, map.get("b"));
        assertEquals(1, map.size());
    }

    @Test
    public void testPut_SameKeyAndValue_IsNoOp() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        Integer returned = map.put("a", 1);
        assertEquals(1, returned);
        assertEquals(1, map.size());
        assertEquals(1, map.get("a"));
    }

    @Test
    public void testGet_ExistingKey_ReturnsValue() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        assertEquals(1, map.get("a"));
    }

    @Test
    public void testGet_AbsentKey_ReturnsNull() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        assertNull(map.get("absent"));
    }

    // ─── getKey ───────────────────────────────────────────────────────────────

    @Test
    public void testGetKey_ExistingValue_ReturnsKey() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        assertEquals("a", map.getKey(1).get());
    }

    @Test
    public void testGetKey_AbsentValue_ReturnsEmpty() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        assertTrue(map.getKey(99).isEmpty());
    }

    @Test
    public void testGetKey_SymmetricWithGet() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
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
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        assertTrue(map.containsKey("a"));
    }

    @Test
    public void testContainsKey_Absent() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        assertFalse(map.containsKey("a"));
    }

    @Test
    public void testContainsValue_Present() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        assertTrue(map.containsValue(1));
    }

    @Test
    public void testContainsValue_Absent() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        assertFalse(map.containsValue(1));
    }

    // ─── remove / removeByValue ───────────────────────────────────────────────

    @Test
    public void testRemove_ExistingKey_ReturnsValue() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        assertEquals(1, map.remove("a"));
    }

    @Test
    public void testRemove_ExistingKey_SizeDecreases() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        map.remove("a");
        assertEquals(0, map.size());
    }

    @Test
    public void testRemove_ExistingKey_EntryGone() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        map.remove("a");
        assertFalse(map.containsKey("a"));
        assertFalse(map.containsValue(1));
    }

    @Test
    public void testRemove_AbsentKey_ReturnsNull() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        assertNull(map.remove("absent"));
    }

    @Test
    public void testRemoveByValue_ExistingValue_ReturnsKey() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        assertEquals("a", map.removeByValue(1).get());
    }

    @Test
    public void testRemoveByValue_ExistingValue_EntryGone() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        map.removeByValue(1);
        assertFalse(map.containsKey("a"));
        assertFalse(map.containsValue(1));
    }

    @Test
    public void testRemoveByValue_AbsentValue_ReturnsNull() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        assertTrue(map.removeByValue(99).isEmpty());
    }

    @Test
    public void testRemoveByValue_SymmetricWithRemove() {
        SymmetricMap<String, Integer> map1 = new UnsynchronizedSymmetricMap<>();
        map1.put("a", 1);
        map1.put("b", 2);
        map1.remove("a");

        SymmetricMap<String, Integer> map2 = new UnsynchronizedSymmetricMap<>();
        map2.put("a", 1);
        map2.put("b", 2);
        map2.removeByValue(1);

        assertEquals(map1, map2);
    }

    // ─── safePut ──────────────────────────────────────────────────────────────

    @Test
    public void testSafePut_NewEntry_Succeeds() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.safePut("a", 1);
        assertEquals(1, map.get("a"));
    }

    @Test
    public void testSafePut_DuplicateKey_Throws() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        assertThrows(IllegalArgumentException.class, () -> map.safePut("a", 2));
    }

    @Test
    public void testSafePut_DuplicateValue_Throws() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        assertThrows(IllegalArgumentException.class, () -> map.safePut("b", 1));
    }

    @Test
    public void testSafePut_DuplicateKey_MapUnchanged() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        try { map.safePut("a", 2); } catch (IllegalArgumentException ignored) {}
        assertEquals(1, map.get("a"));
        assertEquals(1, map.size());
    }

    // ─── replace ──────────────────────────────────────────────────────────────

    @Test
    public void testReplace_ExistingKey_ReturnsOldValue() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        assertEquals(1, map.replace("a", 2));
    }

    @Test
    public void testReplace_ExistingKey_UpdatesValue() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        map.replace("a", 2);
        assertEquals(2, map.get("a"));
    }

    @Test
    public void testReplace_AbsentKey_ReturnsNull() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        assertNull(map.replace("absent", 1));
    }

    @Test
    public void testReplace_AbsentKey_MapUnchanged() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.replace("absent", 1);
        assertFalse(map.containsKey("absent"));
        assertEquals(0, map.size());
    }

    @Test
    public void testReplace_ExistingValue_RemovesConflict() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.replace("a", 2); // value 2 already belongs to "b"
        assertFalse(map.containsKey("b"));
        assertEquals(2, map.get("a"));
        assertEquals(1, map.size());
    }

    @Test
    public void testReplace_Conditional_MatchingOldValue_Succeeds() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        assertTrue(map.replace("a", 1, 2));
        assertEquals(2, map.get("a"));
    }

    @Test
    public void testReplace_Conditional_NonMatchingOldValue_Fails() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        assertFalse(map.replace("a", 99, 2));
        assertEquals(1, map.get("a"));
    }

    // ─── replaceAll ───────────────────────────────────────────────────────────

    @Test
    public void testReplaceAll_AppliesFunction() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.replaceAll((k, v) -> v * 10);
        assertEquals(10, map.get("a"));
        assertEquals(20, map.get("b"));
    }

    @Test
    public void testReplaceAll_MaintainsBijectivity() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.replaceAll((k, v) -> 42); // produces duplicate values
        assertEquals(1, map.size());
        assertEquals(42, map.get(map.containsKey("a") ? "a" : "b"));
    }

    // ─── merge ────────────────────────────────────────────────────────────────

    @Test
    public void testMerge_AbsentKey_InsertsValue() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.merge("a", 1, Integer::sum);
        assertEquals(1, map.get("a"));
    }

    @Test
    public void testMerge_ExistingKey_AppliesFunction() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        map.merge("a", 2, Integer::sum);
        assertEquals(3, map.get("a"));
    }

    @Test
    public void testMerge_FunctionReturnsNull_RemovesEntry() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        map.merge("a", 1, (v1, v2) -> null);
        assertFalse(map.containsKey("a"));
    }

    // ─── compute ──────────────────────────────────────────────────────────────

    @Test
    public void testCompute_AbsentKey_InsertsValue() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.compute("a", (k, v) -> 1);
        assertEquals(1, map.get("a"));
    }

    @Test
    public void testCompute_ExistingKey_UpdatesValue() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        map.compute("a", (k, v) -> v + 10);
        assertEquals(11, map.get("a"));
    }

    @Test
    public void testCompute_FunctionReturnsNull_RemovesEntry() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        map.compute("a", (k, v) -> null);
        assertFalse(map.containsKey("a"));
    }

    // ─── computeIfPresent ─────────────────────────────────────────────────────

    @Test
    public void testComputeIfPresent_ExistingKey_UpdatesValue() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        map.computeIfPresent("a", (k, v) -> v + 10);
        assertEquals(11, map.get("a"));
    }

    @Test
    public void testComputeIfPresent_AbsentKey_DoesNothing() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.computeIfPresent("absent", (k, v) -> 99);
        assertFalse(map.containsKey("absent"));
    }

    @Test
    public void testComputeIfPresent_FunctionReturnsNull_RemovesEntry() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        map.computeIfPresent("a", (k, v) -> null);
        assertFalse(map.containsKey("a"));
    }

    // ─── inverse ──────────────────────────────────────────────────────────────

    @Test
    public void testInverse_SwapsKeysAndValues() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        map.put("b", 2);
        SymmetricMap<Integer, String> inv = map.inverse();
        assertEquals("a", inv.get(1));
        assertEquals("b", inv.get(2));
    }

    @Test
    public void testInverse_SameSize() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(map.size(), map.inverse().size());
    }

    @Test
    public void testInverse_IsIndependent() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        SymmetricMap<Integer, String> inv = map.inverse();
        map.put("b", 2);
        assertFalse(inv.containsKey(2));
    }

    @Test
    public void testInverse_OfInverse_EqualToOriginal() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(map, map.inverse().inverse());
    }

    @Test
    public void testInverse_ReturnsDifferentInstance() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        assertNotSame(map, map.inverse().inverse());
    }

    // ─── clear ────────────────────────────────────────────────────────────────

    @Test
    public void testClear_EmptiesMap() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testClear_RemovesAllKeys() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        map.clear();
        assertFalse(map.containsKey("a"));
        assertFalse(map.containsValue(1));
    }

    // ─── keySet ───────────────────────────────────────────────────────────────

    @Test
    public void testKeySet_ContainsAllKeys() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertTrue(map.keySet().contains("a"));
        assertTrue(map.keySet().contains("b"));
    }

    @Test
    public void testKeySet_Size() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(2, map.keySet().size());
    }

    @Test
    public void testKeySet_Remove_UpdatesMap() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        map.keySet().remove("a");
        assertFalse(map.containsKey("a"));
        assertFalse(map.containsValue(1));
    }

    @Test
    public void testKeySet_IsLiveView() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        Set<String> keys = map.keySet();
        map.put("b", 2);
        assertTrue(keys.contains("b"));
    }

    @Test
    public void testKeySet_Iterator_CoversAllKeys() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        Set<String> visited = new HashSet<>();
        for (String k : map.keySet()) visited.add(k);
        assertEquals(Set.of("a", "b", "c"), visited);
    }

    // ─── values ───────────────────────────────────────────────────────────────

    @Test
    public void testValues_ReturnsSet() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        assertTrue(map.values() instanceof Set);
    }

    @Test
    public void testValues_ContainsAllValues() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertTrue(map.values().contains(1));
        assertTrue(map.values().contains(2));
    }

    @Test
    public void testValues_Remove_UpdatesMap() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        map.values().remove(1);
        assertFalse(map.containsValue(1));
        assertFalse(map.containsKey("a"));
    }

    @Test
    public void testValues_IsLiveView() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        Set<Integer> values = map.values();
        map.put("b", 2);
        assertTrue(values.contains(2));
    }

    // ─── entrySet ─────────────────────────────────────────────────────────────

    @Test
    public void testEntrySet_Size() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(2, map.entrySet().size());
    }

    @Test
    public void testEntrySet_ContainsEntry() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        Map.Entry<String, Integer> entry = Map.entry("a", 1);
        assertTrue(map.entrySet().contains(entry));
    }

    @Test
    public void testEntrySet_Remove_UpdatesMap() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        map.entrySet().remove(Map.entry("a", 1));
        assertFalse(map.containsKey("a"));
    }

    @Test
    public void testEntrySet_setValue_WorksAndMaintainsBijectivity() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        Map.Entry<String, Integer> entry = map.entrySet().iterator().next();
        Integer oldValue = entry.setValue(2);
        assertEquals(1, oldValue);
        assertEquals(2, map.get("a"));
        assertEquals("a", map.getKey(2).get());
        assertFalse(map.containsValue(1));
    }

    @Test
    public void testEntrySet_setValue_HandlesConflicts() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>(); // ou SynchronizedSymmetricMap
        map.put("a", 1);
        map.put("b", 2);

        // Find the entry for "a" explicitly, regardless of iteration order
        Map.Entry<String, Integer> entry = null;
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            if ("a".equals(e.getKey())) {
                entry = e;
                break;
            }
        }
        assertNotNull(entry);

        // Set value to 2, which conflicts with "b" -> 2
        Integer oldValue = entry.setValue(2);
        assertEquals(1, oldValue);
        assertEquals(2, map.get("a"));
        assertFalse(map.containsKey("b"));
        assertEquals(1, map.size());
    }

    @Test
    public void testEntrySet_IsLiveView() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        Set<Map.Entry<String, Integer>> entries = map.entrySet();
        map.put("b", 2);
        assertEquals(2, entries.size());
    }

    // ─── Views consistency ────────────────────────────────────────────────────

    @Test
    public void testViews_MutualConsistency() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
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
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put(null, 1);
        assertTrue(map.containsKey(null));
        assertEquals(1, map.get(null));
    }

    @Test
    public void testPut_NullValue() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", null);
        assertTrue(map.containsValue(null));
        assertNull(map.get("a"));
    }

    @Test
    public void testPut_NullKeyAndValue() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put(null, null);
        assertTrue(map.containsKey(null));
        assertTrue(map.containsValue(null));
    }

    @Test
    public void testGetKey_NullValue() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", null);
        assertEquals("a", map.getKey(null).get());
    }

    @Test
    public void testRemoveByValue_NullValue() {
        SymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", null);
        assertEquals("a", map.removeByValue(null).get());
        assertFalse(map.containsKey("a"));
    }

    // ─── equals / hashCode ────────────────────────────────────────────────────

    @Test
    public void testEquals_SameEntries() {
        SymmetricMap<String, Integer> map1 = new UnsynchronizedSymmetricMap<>();
        map1.put("a", 1);
        map1.put("b", 2);

        SymmetricMap<String, Integer> map2 = new UnsynchronizedSymmetricMap<>();
        map2.put("a", 1);
        map2.put("b", 2);

        assertEquals(map1, map2);
    }

    @Test
    public void testHashCode_EqualMaps_SameHashCode() {
        SymmetricMap<String, Integer> map1 = new UnsynchronizedSymmetricMap<>();
        map1.put("a", 1);

        SymmetricMap<String, Integer> map2 = new UnsynchronizedSymmetricMap<>();
        map2.put("a", 1);

        assertEquals(map1.hashCode(), map2.hashCode());
    }

    // ─── Large map (triggers resize) ──────────────────────────────────────────

    @Test
    public void testLargeMap_AllEntriesRetrievable() {
        SymmetricMap<Integer, Integer> map = new UnsynchronizedSymmetricMap<>();
        int n = 200;
        for (int i = 0; i < n; i++) map.put(i, i + 1000);
        assertEquals(n, map.size());
        for (int i = 0; i < n; i++) {
            assertEquals(i + 1000, map.get(i));
            assertEquals(i, map.getKey(i + 1000).get());
        }
    }

    @Test
    public void testLargeMap_BijectivityPreserved() {
        SymmetricMap<Integer, Integer> map = new UnsynchronizedSymmetricMap<>();
        int n = 200;
        for (int i = 0; i < n; i++) map.put(i, i + 1000);
        // Every value maps back to exactly one key
        Set<Integer> seenKeys = new HashSet<>();
        for (int i = 0; i < n; i++) {
            Integer key = map.getKey(i + 1000).get();
            assertTrue(seenKeys.add(key), "Duplicate key found for value " + (i + 1000));
        }
    }

    // ─── Serialization ───────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    public void testSerializationRoundTrip() throws Exception {
        UnsynchronizedSymmetricMap<String, Integer> original = new UnsynchronizedSymmetricMap<>();
        original.put("A", 1);
        original.put("B", 2);
        original.put("C", 3);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(original);
        }

        UnsynchronizedSymmetricMap<String, Integer> restored;
        try (ObjectInputStream ois = new ObjectInputStream(
                new ByteArrayInputStream(baos.toByteArray()))) {
            restored = (UnsynchronizedSymmetricMap<String, Integer>) ois.readObject();
        }

        assertEquals(original.size(), restored.size());
        assertEquals(Integer.valueOf(1), restored.get("A"));
        assertEquals(Integer.valueOf(2), restored.get("B"));
        assertEquals(Integer.valueOf(3), restored.get("C"));
        assertEquals("A", restored.getKey(1).get());
        assertEquals("B", restored.getKey(2).get());

        restored.put("D", 4);
        assertEquals(4, restored.size());
    }
}

