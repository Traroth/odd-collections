/*
 * UnsynchronizedMultiMapBlackBoxTest.java
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
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Black-box tests for {@link UnsynchronizedMultiMap}, based solely on the
 * public contract and Javadoc. No knowledge of the internal implementation
 * is assumed.
 */
public class UnsynchronizedMultiMapBlackBoxTest {

    // ─── Constructors ────────────────────────────────────────────────────────

    @Test
    public void testDefaultConstructor_EmptyMap() {
        UnsynchronizedMultiMap<String, Integer> map = new UnsynchronizedMultiMap<>();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testConstructor_InitialCapacity() {
        UnsynchronizedMultiMap<String, Integer> map = new UnsynchronizedMultiMap<>(32);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testConstructor_FromMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);
        UnsynchronizedMultiMap<String, Integer> map = new UnsynchronizedMultiMap<>(source);
        assertEquals(2, map.size());
        assertEquals(1, map.get("a"));
        assertEquals(2, map.get("b"));
    }

    @Test
    public void testConstructor_FromMap_NullKey_ThrowsNPE() {
        Map<String, Integer> source = new HashMap<>();
        source.put(null, 1);
        assertThrows(NullPointerException.class, () -> new UnsynchronizedMultiMap<>(source));
    }

    @Test
    public void testConstructor_FromMap_NullValue_ThrowsNPE() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", null);
        assertThrows(NullPointerException.class, () -> new UnsynchronizedMultiMap<>(source));
    }

    @Test
    public void testConstructor_FromMultiMap() {
        UnsynchronizedMultiMap<String, Integer> source = new UnsynchronizedMultiMap<>();
        source.put("a", 1);
        source.put("b", 2);
        UnsynchronizedMultiMap<String, Integer> copy = new UnsynchronizedMultiMap<>(source);
        assertEquals(2, copy.size());
        assertEquals(1, copy.get("a"));
        assertEquals(2, copy.get("b"));
    }

    @Test
    public void testConstructor_FromMultiMap_IndependentCopy() {
        UnsynchronizedMultiMap<String, Integer> source = new UnsynchronizedMultiMap<>();
        source.put("a", 1);
        UnsynchronizedMultiMap<String, Integer> copy = new UnsynchronizedMultiMap<>(source);
        copy.put("b", 2);
        assertFalse(source.containsKey("b"));
    }

    // ─── put / get ───────────────────────────────────────────────────────────

    @Test
    public void testPut_MultipleEntries() {
        UnsynchronizedMultiMap<String, Integer> map = new UnsynchronizedMultiMap<>();
        for (int i = 0; i < 100; i++) {
            map.put("key" + i, i);
        }
        assertEquals(100, map.size());
        for (int i = 0; i < 100; i++) {
            assertEquals(i, map.get("key" + i));
        }
    }

    // ─── getOrCreate ─────────────────────────────────────────────────────────

    @Test
    public void testGetOrCreate_FactoryNotCalledForExistingKey() {
        UnsynchronizedMultiMap<String, Integer> map = new UnsynchronizedMultiMap<>();
        map.put("a", 1);
        boolean[] called = {false};
        map.getOrCreate("a", () -> {
            called[0] = true;
            return 42;
        });
        assertFalse(called[0]);
    }

    // ─── getOpt ──────────────────────────────────────────────────────────────

    @Test
    public void testGetOpt_ConsistentWithGet() {
        UnsynchronizedMultiMap<String, Integer> map = new UnsynchronizedMultiMap<>();
        map.put("a", 1);
        assertEquals(Optional.of(1), map.getOpt("a"));
        assertEquals(Optional.empty(), map.getOpt("missing"));
    }

    // ─── Fail-fast iterators ─────────────────────────────────────────────────

    @Test
    public void testKeySetIterator_FailFast_OnPut() {
        UnsynchronizedMultiMap<String, Integer> map = new UnsynchronizedMultiMap<>();
        map.put("a", 1);
        Iterator<String> it = map.keySet().iterator();
        map.put("b", 2);
        assertThrows(ConcurrentModificationException.class, it::hasNext);
    }

    @Test
    public void testKeySetIterator_FailFast_OnRemove() {
        UnsynchronizedMultiMap<String, Integer> map = new UnsynchronizedMultiMap<>();
        map.put("a", 1);
        Iterator<String> it = map.keySet().iterator();
        map.remove("a");
        assertThrows(ConcurrentModificationException.class, it::hasNext);
    }

    @Test
    public void testKeySetIterator_FailFast_OnClear() {
        UnsynchronizedMultiMap<String, Integer> map = new UnsynchronizedMultiMap<>();
        map.put("a", 1);
        Iterator<String> it = map.keySet().iterator();
        map.clear();
        assertThrows(ConcurrentModificationException.class, it::hasNext);
    }

    @Test
    public void testKeySetIterator_FailFast_OnGetOrCreate() {
        UnsynchronizedMultiMap<String, Integer> map = new UnsynchronizedMultiMap<>();
        Iterator<String> it = map.keySet().iterator();
        map.getOrCreate("a", () -> 1);
        assertThrows(ConcurrentModificationException.class, it::hasNext);
    }

    @Test
    public void testValuesIterator_FailFast_OnPut() {
        UnsynchronizedMultiMap<String, Integer> map = new UnsynchronizedMultiMap<>();
        map.put("a", 1);
        Iterator<Integer> it = map.values().iterator();
        map.put("b", 2);
        assertThrows(ConcurrentModificationException.class, it::hasNext);
    }

    @Test
    public void testEntrySetIterator_FailFast_OnPut() {
        UnsynchronizedMultiMap<String, Integer> map = new UnsynchronizedMultiMap<>();
        map.put("a", 1);
        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        map.put("b", 2);
        assertThrows(ConcurrentModificationException.class, it::hasNext);
    }

    @Test
    public void testKeySetIterator_NoModification_NoException() {
        UnsynchronizedMultiMap<String, Integer> map = new UnsynchronizedMultiMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Iterator<String> it = map.keySet().iterator();
        assertTrue(it.hasNext());
        it.next();
        assertTrue(it.hasNext());
        it.next();
        assertFalse(it.hasNext());
    }

    // ─── Views ───────────────────────────────────────────────────────────────

    @Test
    public void testKeySet_Size() {
        UnsynchronizedMultiMap<String, Integer> map = new UnsynchronizedMultiMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(2, map.keySet().size());
    }

    @Test
    public void testKeySet_Contains() {
        UnsynchronizedMultiMap<String, Integer> map = new UnsynchronizedMultiMap<>();
        map.put("a", 1);
        assertTrue(map.keySet().contains("a"));
        assertFalse(map.keySet().contains("b"));
    }

    @Test
    public void testValues_Size() {
        UnsynchronizedMultiMap<String, Integer> map = new UnsynchronizedMultiMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(2, map.values().size());
    }

    @Test
    public void testValues_Contains() {
        UnsynchronizedMultiMap<String, Integer> map = new UnsynchronizedMultiMap<>();
        map.put("a", 1);
        assertTrue(map.values().contains(1));
        assertFalse(map.values().contains(99));
    }

    @Test
    public void testEntrySet_Size() {
        UnsynchronizedMultiMap<String, Integer> map = new UnsynchronizedMultiMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(2, map.entrySet().size());
    }

    // ─── equals / hashCode / toString ────────────────────────────────────────

    @Test
    public void testEquals_SameContent() {
        UnsynchronizedMultiMap<String, Integer> map1 = new UnsynchronizedMultiMap<>();
        map1.put("a", 1);
        map1.put("b", 2);
        UnsynchronizedMultiMap<String, Integer> map2 = new UnsynchronizedMultiMap<>();
        map2.put("a", 1);
        map2.put("b", 2);
        assertEquals(map1, map2);
    }

    @Test
    public void testEquals_DifferentContent() {
        UnsynchronizedMultiMap<String, Integer> map1 = new UnsynchronizedMultiMap<>();
        map1.put("a", 1);
        UnsynchronizedMultiMap<String, Integer> map2 = new UnsynchronizedMultiMap<>();
        map2.put("a", 2);
        assertNotEquals(map1, map2);
    }

    @Test
    public void testHashCode_SameContent() {
        UnsynchronizedMultiMap<String, Integer> map1 = new UnsynchronizedMultiMap<>();
        map1.put("a", 1);
        UnsynchronizedMultiMap<String, Integer> map2 = new UnsynchronizedMultiMap<>();
        map2.put("a", 1);
        assertEquals(map1.hashCode(), map2.hashCode());
    }

    @Test
    public void testToString_NonEmpty() {
        UnsynchronizedMultiMap<String, Integer> map = new UnsynchronizedMultiMap<>();
        map.put("a", 1);
        String str = map.toString();
        assertTrue(str.contains("a"));
        assertTrue(str.contains("1"));
    }

    // ─── Multi-level recursive usage ─────────────────────────────────────────

    @Test
    public void testRecursive_ThreeLevels_WriteAndRead() {
        UnsynchronizedMultiMap<String, MultiMap<String, MultiMap<String, Integer>>> map =
            new UnsynchronizedMultiMap<>();
        map.getOrCreate("France", UnsynchronizedMultiMap::new)
           .getOrCreate("Paris", UnsynchronizedMultiMap::new)
           .put("Q1", 42);
        map.getOrCreate("France", UnsynchronizedMultiMap::new)
           .getOrCreate("Paris", UnsynchronizedMultiMap::new)
           .put("Q2", 73);
        map.getOrCreate("France", UnsynchronizedMultiMap::new)
           .getOrCreate("Lyon", UnsynchronizedMultiMap::new)
           .put("Q1", 10);

        assertEquals(42, map.get("France").get("Paris").get("Q1"));
        assertEquals(73, map.get("France").get("Paris").get("Q2"));
        assertEquals(10, map.get("France").get("Lyon").get("Q1"));
        assertEquals(1, map.size());
        assertEquals(2, map.get("France").size());
    }

    @Test
    public void testRecursive_ThreeLevels_SafeRead() {
        UnsynchronizedMultiMap<String, MultiMap<String, MultiMap<String, Integer>>> map =
            new UnsynchronizedMultiMap<>();
        map.getOrCreate("France", UnsynchronizedMultiMap::new)
           .getOrCreate("Paris", UnsynchronizedMultiMap::new)
           .put("Q1", 42);

        Optional<Integer> result = map.getOpt("France")
            .flatMap(m -> m.getOpt("Paris"))
            .flatMap(m -> m.getOpt("Q1"));
        assertTrue(result.isPresent());
        assertEquals(42, result.get());

        Optional<Integer> absent = map.getOpt("Germany")
            .flatMap(m -> m.getOpt("Berlin"))
            .flatMap(m -> m.getOpt("Q1"));
        assertFalse(absent.isPresent());
    }

    @Test
    public void testRecursive_PartialLookup_ReturnsSubMap() {
        UnsynchronizedMultiMap<String, MultiMap<String, Integer>> map =
            new UnsynchronizedMultiMap<>();
        map.getOrCreate("a", UnsynchronizedMultiMap::new).put("x", 1);
        map.getOrCreate("a", UnsynchronizedMultiMap::new).put("y", 2);

        MultiMap<String, Integer> subMap = map.get("a");
        assertEquals(2, subMap.size());
        assertEquals(1, subMap.get("x"));
        assertEquals(2, subMap.get("y"));
    }

    // ─── Serialization ───────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    public void testSerializationRoundTrip() throws Exception {
        UnsynchronizedMultiMap<String, Integer> original = new UnsynchronizedMultiMap<>();
        original.put("A", 1);
        original.put("B", 2);
        original.put("C", 3);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(original);
        }

        UnsynchronizedMultiMap<String, Integer> restored;
        try (ObjectInputStream ois = new ObjectInputStream(
                new ByteArrayInputStream(baos.toByteArray()))) {
            restored = (UnsynchronizedMultiMap<String, Integer>) ois.readObject();
        }

        assertEquals(original.size(), restored.size());
        assertEquals(Integer.valueOf(1), restored.get("A"));
        assertEquals(Integer.valueOf(2), restored.get("B"));
        assertEquals(Integer.valueOf(3), restored.get("C"));

        restored.put("D", 4);
        assertEquals(4, restored.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSerializationRoundTrip_Recursive() throws Exception {
        UnsynchronizedMultiMap<String, Object> root = new UnsynchronizedMultiMap<>();
        UnsynchronizedMultiMap<String, Integer> sub = new UnsynchronizedMultiMap<>();
        sub.put("x", 1);
        sub.put("y", 2);
        root.put("sub", sub);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(root);
        }

        UnsynchronizedMultiMap<String, Object> restored;
        try (ObjectInputStream ois = new ObjectInputStream(
                new ByteArrayInputStream(baos.toByteArray()))) {
            restored = (UnsynchronizedMultiMap<String, Object>) ois.readObject();
        }

        assertEquals(1, restored.size());
        UnsynchronizedMultiMap<String, Integer> restoredSub =
                (UnsynchronizedMultiMap<String, Integer>) restored.get("sub");
        assertEquals(2, restoredSub.size());
        assertEquals(Integer.valueOf(1), restoredSub.get("x"));
    }
}
