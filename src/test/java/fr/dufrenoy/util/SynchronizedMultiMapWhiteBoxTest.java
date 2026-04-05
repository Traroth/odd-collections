/*
 * SynchronizedMultiMapWhiteBoxTest.java
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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * White-box tests for {@link SynchronizedMultiMap}:
 * - Inner {@code UnsynchronizedMultiMap} delegation integrity
 * - Lock presence and type
 * - Snapshot isolation of views
 * - Concurrent getOrCreate atomicity
 */
public class SynchronizedMultiMapWhiteBoxTest {

    // ─── Helpers ─────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private <K, V> UnsynchronizedMultiMap<K, V> getInner(SynchronizedMultiMap<K, V> map)
            throws Exception {
        Field field = SynchronizedMultiMap.class.getDeclaredField("inner");
        field.setAccessible(true);
        return (UnsynchronizedMultiMap<K, V>) field.get(map);
    }

    private ReentrantReadWriteLock getRwl(SynchronizedMultiMap<?, ?> map) throws Exception {
        Field field = SynchronizedMultiMap.class.getDeclaredField("rwl");
        field.setAccessible(true);
        return (ReentrantReadWriteLock) field.get(map);
    }

    // ─── Inner delegation ────────────────────────────────────────────────────

    @Test
    public void testInnerMap_ReflectsSameSize() throws Exception {
        SynchronizedMultiMap<String, Integer> map = new SynchronizedMultiMap<>();
        map.put("a", 1);
        map.put("b", 2);
        UnsynchronizedMultiMap<String, Integer> inner = getInner(map);
        assertEquals(2, inner.size());
        assertEquals(map.size(), inner.size());
    }

    @Test
    public void testInnerMap_ReflectsSameContent() throws Exception {
        SynchronizedMultiMap<String, Integer> map = new SynchronizedMultiMap<>();
        map.put("a", 1);
        map.put("b", 2);
        UnsynchronizedMultiMap<String, Integer> inner = getInner(map);
        assertEquals(Integer.valueOf(1), inner.get("a"));
        assertEquals(Integer.valueOf(2), inner.get("b"));
    }

    @Test
    public void testInnerMap_EmptyAfterClear() throws Exception {
        SynchronizedMultiMap<String, Integer> map = new SynchronizedMultiMap<>();
        map.put("a", 1);
        map.clear();
        UnsynchronizedMultiMap<String, Integer> inner = getInner(map);
        assertTrue(inner.isEmpty());
    }

    // ─── Lock structure ──────────────────────────────────────────────────────

    @Test
    public void testLock_IsReentrantReadWriteLock() throws Exception {
        SynchronizedMultiMap<String, Integer> map = new SynchronizedMultiMap<>();
        ReentrantReadWriteLock rwl = getRwl(map);
        assertNotNull(rwl);
        assertFalse(rwl.isFair());
    }

    // ─── Snapshot isolation ──────────────────────────────────────────────────

    @Test
    public void testKeySet_IsSnapshot_IndependentOfSubsequentRemove() {
        SynchronizedMultiMap<String, Integer> map = new SynchronizedMultiMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Set<String> keys = map.keySet();
        map.remove("a");
        assertEquals(2, keys.size());
        assertTrue(keys.contains("a"));
    }

    @Test
    public void testEntrySet_IsSnapshot_IndependentOfSubsequentClear() {
        SynchronizedMultiMap<String, Integer> map = new SynchronizedMultiMap<>();
        map.put("a", 1);
        Set<Map.Entry<String, Integer>> entries = map.entrySet();
        map.clear();
        assertEquals(1, entries.size());
    }

    @Test
    public void testValues_IsSnapshot_IndependentOfSubsequentPut() {
        SynchronizedMultiMap<String, Integer> map = new SynchronizedMultiMap<>();
        map.put("a", 1);
        var values = map.values();
        map.put("b", 2);
        assertEquals(1, values.size());
    }

    @Test
    public void testKeySet_Snapshot_IteratorDoesNotThrowCME() {
        SynchronizedMultiMap<String, Integer> map = new SynchronizedMultiMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Set<String> keys = map.keySet();
        map.put("c", 3);
        // No ConcurrentModificationException — snapshot is independent
        Iterator<String> it = keys.iterator();
        int count = 0;
        while (it.hasNext()) {
            it.next();
            count++;
        }
        assertEquals(2, count);
    }

    // ─── Concurrent getOrCreate atomicity ────────────────────────────────────

    @Test
    public void testGetOrCreate_ConcurrentAccess_SingleCreation() throws InterruptedException {
        SynchronizedMultiMap<String, Integer> map = new SynchronizedMultiMap<>();
        int threadCount = 16;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        int[] factoryCalls = {0};
        Object factoryLock = new Object();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    map.getOrCreate("shared", () -> {
                        synchronized (factoryLock) {
                            factoryCalls[0]++;
                        }
                        return 42;
                    });
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(42, map.get("shared"));
        assertEquals(1, map.size());
        // Factory should be called exactly once since getOrCreate holds write lock
        assertEquals(1, factoryCalls[0]);
    }

    // ─── Constructor copies ──────────────────────────────────────────────────

    @Test
    public void testConstructor_FromMultiMap_IndependentInner() throws Exception {
        UnsynchronizedMultiMap<String, Integer> source = new UnsynchronizedMultiMap<>();
        source.put("a", 1);
        SynchronizedMultiMap<String, Integer> map = new SynchronizedMultiMap<>(source);

        // Modify source — should not affect the synchronized copy
        source.put("b", 2);
        assertEquals(1, map.size());
        UnsynchronizedMultiMap<String, Integer> inner = getInner(map);
        assertFalse(inner.containsKey("b"));
    }
}
