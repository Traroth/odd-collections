/*
 * SynchronizedMultiMapBlackBoxTest.java
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Black-box tests for {@link SynchronizedMultiMap}, based solely on the
 * public contract and Javadoc. Includes thread-safety tests.
 */
public class SynchronizedMultiMapBlackBoxTest {

    // ─── Constructors ────────────────────────────────────────────────────────

    @Test
    public void testDefaultConstructor_EmptyMap() {
        SynchronizedMultiMap<String, Integer> map = new SynchronizedMultiMap<>();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testConstructor_InitialCapacity() {
        SynchronizedMultiMap<String, Integer> map = new SynchronizedMultiMap<>(32);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testConstructor_FromMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);
        SynchronizedMultiMap<String, Integer> map = new SynchronizedMultiMap<>(source);
        assertEquals(2, map.size());
        assertEquals(1, map.get("a"));
        assertEquals(2, map.get("b"));
    }

    @Test
    public void testConstructor_FromMultiMap() {
        UnsynchronizedMultiMap<String, Integer> source = new UnsynchronizedMultiMap<>();
        source.put("a", 1);
        SynchronizedMultiMap<String, Integer> map = new SynchronizedMultiMap<>(source);
        assertEquals(1, map.size());
        assertEquals(1, map.get("a"));
    }

    // ─── put / get / remove ──────────────────────────────────────────────────

    @Test
    public void testPut_NewEntry_ReturnsNull() {
        SynchronizedMultiMap<String, Integer> map = new SynchronizedMultiMap<>();
        assertNull(map.put("a", 1));
    }

    @Test
    public void testPut_ExistingKey_ReturnsPreviousValue() {
        SynchronizedMultiMap<String, Integer> map = new SynchronizedMultiMap<>();
        map.put("a", 1);
        assertEquals(1, map.put("a", 2));
        assertEquals(2, map.get("a"));
    }

    @Test
    public void testRemove_ExistingKey_ReturnsPreviousValue() {
        SynchronizedMultiMap<String, Integer> map = new SynchronizedMultiMap<>();
        map.put("a", 1);
        assertEquals(1, map.remove("a"));
        assertTrue(map.isEmpty());
    }

    @Test
    public void testRemove_AbsentKey_ReturnsNull() {
        SynchronizedMultiMap<String, Integer> map = new SynchronizedMultiMap<>();
        assertNull(map.remove("missing"));
    }

    // ─── getOpt ──────────────────────────────────────────────────────────────

    @Test
    public void testGetOpt_ExistingKey_ReturnsPresent() {
        SynchronizedMultiMap<String, Integer> map = new SynchronizedMultiMap<>();
        map.put("a", 1);
        assertEquals(Optional.of(1), map.getOpt("a"));
    }

    @Test
    public void testGetOpt_AbsentKey_ReturnsEmpty() {
        SynchronizedMultiMap<String, Integer> map = new SynchronizedMultiMap<>();
        assertFalse(map.getOpt("missing").isPresent());
    }

    // ─── getOrCreate ─────────────────────────────────────────────────────────

    @Test
    public void testGetOrCreate_AbsentKey_CreatesValue() {
        SynchronizedMultiMap<String, Integer> map = new SynchronizedMultiMap<>();
        assertEquals(42, map.getOrCreate("a", () -> 42));
        assertEquals(42, map.get("a"));
    }

    @Test
    public void testGetOrCreate_ExistingKey_ReturnsExisting() {
        SynchronizedMultiMap<String, Integer> map = new SynchronizedMultiMap<>();
        map.put("a", 1);
        assertEquals(1, map.getOrCreate("a", () -> 42));
    }

    // ─── containsKey ─────────────────────────────────────────────────────────

    @Test
    public void testContainsKey_Present() {
        SynchronizedMultiMap<String, Integer> map = new SynchronizedMultiMap<>();
        map.put("a", 1);
        assertTrue(map.containsKey("a"));
    }

    @Test
    public void testContainsKey_Absent() {
        SynchronizedMultiMap<String, Integer> map = new SynchronizedMultiMap<>();
        assertFalse(map.containsKey("missing"));
    }

    // ─── clear ───────────────────────────────────────────────────────────────

    @Test
    public void testClear() {
        SynchronizedMultiMap<String, Integer> map = new SynchronizedMultiMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    // ─── Null rejection ──────────────────────────────────────────────────────

    @Test
    public void testPut_NullKey_ThrowsNPE() {
        SynchronizedMultiMap<String, Integer> map = new SynchronizedMultiMap<>();
        assertThrows(NullPointerException.class, () -> map.put(null, 1));
    }

    @Test
    public void testPut_NullValue_ThrowsNPE() {
        SynchronizedMultiMap<String, Integer> map = new SynchronizedMultiMap<>();
        assertThrows(NullPointerException.class, () -> map.put("a", null));
    }

    @Test
    public void testGet_NullKey_ThrowsNPE() {
        SynchronizedMultiMap<String, Integer> map = new SynchronizedMultiMap<>();
        assertThrows(NullPointerException.class, () -> map.get(null));
    }

    // ─── Views (snapshot-based) ──────────────────────────────────────────────

    @Test
    public void testKeySet_Snapshot_NotAffectedBySubsequentPut() {
        SynchronizedMultiMap<String, Integer> map = new SynchronizedMultiMap<>();
        map.put("a", 1);
        Set<String> keys = map.keySet();
        map.put("b", 2);
        assertEquals(1, keys.size());
        assertTrue(keys.contains("a"));
    }

    @Test
    public void testValues_Snapshot() {
        SynchronizedMultiMap<String, Integer> map = new SynchronizedMultiMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(2, map.values().size());
    }

    @Test
    public void testEntrySet_Snapshot() {
        SynchronizedMultiMap<String, Integer> map = new SynchronizedMultiMap<>();
        map.put("a", 1);
        Set<Map.Entry<String, Integer>> entries = map.entrySet();
        map.put("b", 2);
        assertEquals(1, entries.size());
    }

    // ─── equals / hashCode / toString ────────────────────────────────────────

    @Test
    public void testEquals_SameContent() {
        SynchronizedMultiMap<String, Integer> map1 = new SynchronizedMultiMap<>();
        map1.put("a", 1);
        SynchronizedMultiMap<String, Integer> map2 = new SynchronizedMultiMap<>();
        map2.put("a", 1);
        assertEquals(map1, map2);
    }

    @Test
    public void testEquals_DifferentContent() {
        SynchronizedMultiMap<String, Integer> map1 = new SynchronizedMultiMap<>();
        map1.put("a", 1);
        SynchronizedMultiMap<String, Integer> map2 = new SynchronizedMultiMap<>();
        map2.put("a", 2);
        assertNotEquals(map1, map2);
    }

    @Test
    public void testHashCode_SameContent() {
        SynchronizedMultiMap<String, Integer> map1 = new SynchronizedMultiMap<>();
        map1.put("a", 1);
        SynchronizedMultiMap<String, Integer> map2 = new SynchronizedMultiMap<>();
        map2.put("a", 1);
        assertEquals(map1.hashCode(), map2.hashCode());
    }

    @Test
    public void testToString_NonEmpty() {
        SynchronizedMultiMap<String, Integer> map = new SynchronizedMultiMap<>();
        map.put("a", 1);
        assertTrue(map.toString().contains("a"));
    }

    // ─── Multi-level recursive usage ─────────────────────────────────────────

    @Test
    public void testRecursive_ThreeLevels() {
        SynchronizedMultiMap<String, MultiMap<String, MultiMap<String, Integer>>> map =
            new SynchronizedMultiMap<>();
        map.getOrCreate("France", SynchronizedMultiMap::new)
           .getOrCreate("Paris", SynchronizedMultiMap::new)
           .put("Q1", 42);

        assertEquals(42, map.get("France").get("Paris").get("Q1"));
    }

    @Test
    public void testRecursive_SafeRead() {
        SynchronizedMultiMap<String, MultiMap<String, Integer>> map =
            new SynchronizedMultiMap<>();
        map.getOrCreate("a", SynchronizedMultiMap::new).put("x", 1);

        Optional<Integer> result = map.getOpt("a")
            .flatMap(m -> m.getOpt("x"));
        assertTrue(result.isPresent());
        assertEquals(1, result.get());

        Optional<Integer> absent = map.getOpt("missing")
            .flatMap(m -> m.getOpt("x"));
        assertFalse(absent.isPresent());
    }

    // ─── Concurrency ─────────────────────────────────────────────────────────

    @Test
    public void testConcurrentPuts_NoDataLoss() throws InterruptedException {
        SynchronizedMultiMap<Integer, Integer> map = new SynchronizedMultiMap<>();
        int threadCount = 8;
        int opsPerThread = 1000;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int t = 0; t < threadCount; t++) {
            int offset = t * opsPerThread;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < opsPerThread; i++) {
                        map.put(offset + i, offset + i);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        assertEquals(threadCount * opsPerThread, map.size());
    }

    @Test
    public void testConcurrentReadsAndWrites_NoException() throws InterruptedException {
        SynchronizedMultiMap<Integer, Integer> map = new SynchronizedMultiMap<>();
        int threadCount = 8;
        int opsPerThread = 500;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger errors = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < opsPerThread; i++) {
                        if (threadId % 2 == 0) {
                            map.put(i, i);
                        } else {
                            map.get(i);
                            map.containsKey(i);
                            map.size();
                        }
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        assertEquals(0, errors.get());
    }
}
