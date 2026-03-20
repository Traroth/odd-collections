/*
 * SynchronizedSymmetricMapWhiteBoxTest.java
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
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * White-box tests for SynchronizedSymmetricMap:
 * - Integrity of key and value collision chains in the internal map
 * - Internal invariants (bijectivity, absence of duplicates, chain consistency)
 * - Edge cases (hash collisions, resize, Entry reuse)
 */
public class SynchronizedSymmetricMapWhiteBoxTest {

    @Test
    public void testChainsIntegrityAfterInsertions() throws Exception {
        SynchronizedSymmetricMap<String, Integer> map = new SynchronizedSymmetricMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);

        // Access the inner map via reflection
        Field innerField = SynchronizedSymmetricMap.class.getDeclaredField("inner");
        innerField.setAccessible(true);
        UnsynchronizedSymmetricMap<String, Integer> inner =
                (UnsynchronizedSymmetricMap<String, Integer>) innerField.get(map);

        // Access the internal table via reflection
        Field tableField = UnsynchronizedSymmetricMap.class.getDeclaredField("table");
        tableField.setAccessible(true);
        Object[] table = (Object[]) tableField.get(inner);

        Set<Object> entriesByKey = new HashSet<>();
        Set<Object> entriesByValue = new HashSet<>();

        // Walk key chains
        for (Object bucket : table) {
            if (bucket == null) {
                continue;
            }
            Field firstByKeyField = bucket.getClass().getDeclaredField("firstByKey");
            firstByKeyField.setAccessible(true);
            Object entry = firstByKeyField.get(bucket);
            while (entry != null) {
                entriesByKey.add(entry);
                Field nextByKeyField = entry.getClass().getDeclaredField("nextByKey");
                nextByKeyField.setAccessible(true);
                entry = nextByKeyField.get(entry);
            }
        }

        // Walk value chains
        for (Object bucket : table) {
            if (bucket == null) {
                continue;
            }
            Field firstByValueField = bucket.getClass().getDeclaredField("firstByValue");
            firstByValueField.setAccessible(true);
            Object entry = firstByValueField.get(bucket);
            while (entry != null) {
                entriesByValue.add(entry);
                Field nextByValueField = entry.getClass().getDeclaredField("nextByValue");
                nextByValueField.setAccessible(true);
                entry = nextByValueField.get(entry);
            }
        }

        // Both sets must be equal and of the correct size
        assertEquals(4, entriesByKey.size());
        assertEquals(4, entriesByValue.size());
        assertEquals(entriesByKey, entriesByValue);
    }

    @Test
    public void testKeyHashCollisions_AllEntriesInSameBucket() throws Exception {
        // Create keys with the same hash to force collisions
        class CollidingKey {
            private final int value;
            private final int hash;

            CollidingKey(int value, int hash) {
                this.value = value;
                this.hash = hash;
            }

            @Override
            public int hashCode() {
                return hash;
            }

            @Override
            public boolean equals(Object o) {
                if (!(o instanceof CollidingKey)) {
                    return false;
                }
                return ((CollidingKey) o).value == this.value;
            }

            @Override
            public String toString() {
                return "Key" + value;
            }
        }

        SynchronizedSymmetricMap<CollidingKey, Integer> map = new SynchronizedSymmetricMap<>();
        // All these keys share the same hash (0)
        CollidingKey k1 = new CollidingKey(1, 0);
        CollidingKey k2 = new CollidingKey(2, 0);
        CollidingKey k3 = new CollidingKey(3, 0);

        map.put(k1, 10);
        map.put(k2, 20);
        map.put(k3, 30);

        // Verify that all entries are present and accessible
        assertEquals(10, map.get(k1));
        assertEquals(20, map.get(k2));
        assertEquals(30, map.get(k3));
        assertEquals(3, map.size());

        // Access the inner map via reflection
        Field innerField = SynchronizedSymmetricMap.class.getDeclaredField("inner");
        innerField.setAccessible(true);
        UnsynchronizedSymmetricMap<CollidingKey, Integer> inner =
                (UnsynchronizedSymmetricMap<CollidingKey, Integer>) innerField.get(map);

        // Verify internal chain integrity
        Field tableField = UnsynchronizedSymmetricMap.class.getDeclaredField("table");
        tableField.setAccessible(true);
        Object[] table = (Object[]) tableField.get(inner);

        // All entries should be in the same bucket (index 0)
        Object bucket = table[0];
        Field firstByKeyField = bucket.getClass().getDeclaredField("firstByKey");
        firstByKeyField.setAccessible(true);
        Set<Object> entriesInChain = new HashSet<>();
        Object entry = firstByKeyField.get(bucket);
        while (entry != null) {
            entriesInChain.add(entry);
            Field nextByKeyField = entry.getClass().getDeclaredField("nextByKey");
            nextByKeyField.setAccessible(true);
            entry = nextByKeyField.get(entry);
        }
        assertEquals(3, entriesInChain.size());
    }

    @Test
    public void testResize_ChainsRebuiltCorrectly() throws Exception {
        // Small initial capacity to trigger resize early
        SynchronizedSymmetricMap<Integer, Integer> map = new SynchronizedSymmetricMap<>(4, 0.75f);

        // Add enough entries to trigger a resize
        for (int i = 0; i < 20; i++) {
            map.put(i, i + 100);
        }

        // Verify that the size is correct
        assertEquals(20, map.size());

        // Verify that all entries are accessible
        for (int i = 0; i < 20; i++) {
            assertEquals(i + 100, map.get(i));
            assertEquals(i, map.getKey(i + 100).get());
        }

        // Access the inner map via reflection
        Field innerField = SynchronizedSymmetricMap.class.getDeclaredField("inner");
        innerField.setAccessible(true);
        UnsynchronizedSymmetricMap<Integer, Integer> inner =
                (UnsynchronizedSymmetricMap<Integer, Integer>) innerField.get(map);

        // Verify internal chain integrity after resize
        Field tableField = UnsynchronizedSymmetricMap.class.getDeclaredField("table");
        tableField.setAccessible(true);
        Object[] table = (Object[]) tableField.get(inner);

        Set<Object> entriesByKey = new HashSet<>();
        Set<Object> entriesByValue = new HashSet<>();

        // Walk key chains
        for (Object bucket : table) {
            if (bucket == null) {
                continue;
            }
            Field firstByKeyField = bucket.getClass().getDeclaredField("firstByKey");
            firstByKeyField.setAccessible(true);
            Object entry = firstByKeyField.get(bucket);
            while (entry != null) {
                entriesByKey.add(entry);
                Field nextByKeyField = entry.getClass().getDeclaredField("nextByKey");
                nextByKeyField.setAccessible(true);
                entry = nextByKeyField.get(entry);
            }
        }

        // Walk value chains
        for (Object bucket : table) {
            if (bucket == null) {
                continue;
            }
            Field firstByValueField = bucket.getClass().getDeclaredField("firstByValue");
            firstByValueField.setAccessible(true);
            Object entry = firstByValueField.get(bucket);
            while (entry != null) {
                entriesByValue.add(entry);
                Field nextByValueField = entry.getClass().getDeclaredField("nextByValue");
                nextByValueField.setAccessible(true);
                entry = nextByValueField.get(entry);
            }
        }

        // Both sets must be equal and of the correct size
        assertEquals(20, entriesByKey.size());
        assertEquals(20, entriesByValue.size());
        assertEquals(entriesByKey, entriesByValue);
    }
}