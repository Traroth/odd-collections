/*
 * UnsynchronizedSymmetricMapWhiteBoxTest.java
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
 * White-box tests for {@link UnsynchronizedSymmetricMap}:
 * - Integrity of both collision chains (by key and by value)
 * - Internal invariants (bijectivity, no orphaned entries, chain consistency)
 * - Edge cases (hash collisions on keys and values, resize, remove, clear)
 */
public class UnsynchronizedSymmetricMapWhiteBoxTest {

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Traverses all key chains in the table and returns the set of all entries
     * reachable by key.
     */
    private Set<Object> entriesByKey(Object[] table) throws Exception {
        Set<Object> entries = new HashSet<>();
        for (Object bucket : table) {
            if (bucket == null) continue;
            Field firstByKeyField = bucket.getClass().getDeclaredField("firstByKey");
            firstByKeyField.setAccessible(true);
            Object entry = firstByKeyField.get(bucket);
            while (entry != null) {
                entries.add(entry);
                Field nextByKeyField = entry.getClass().getDeclaredField("nextByKey");
                nextByKeyField.setAccessible(true);
                entry = nextByKeyField.get(entry);
            }
        }
        return entries;
    }

    /**
     * Traverses all value chains in the table and returns the set of all entries
     * reachable by value.
     */
    private Set<Object> entriesByValue(Object[] table) throws Exception {
        Set<Object> entries = new HashSet<>();
        for (Object bucket : table) {
            if (bucket == null) continue;
            Field firstByValueField = bucket.getClass().getDeclaredField("firstByValue");
            firstByValueField.setAccessible(true);
            Object entry = firstByValueField.get(bucket);
            while (entry != null) {
                entries.add(entry);
                Field nextByValueField = entry.getClass().getDeclaredField("nextByValue");
                nextByValueField.setAccessible(true);
                entry = nextByValueField.get(entry);
            }
        }
        return entries;
    }

    /**
     * Returns the internal table of the given map via reflection.
     */
    private Object[] getTable(UnsynchronizedSymmetricMap<?, ?> map) throws Exception {
        Field tableField = UnsynchronizedSymmetricMap.class.getDeclaredField("table");
        tableField.setAccessible(true);
        return (Object[]) tableField.get(map);
    }

    /**
     * Asserts that both collision chains in the given table contain exactly
     * {@code expectedSize} entries and that they reference the same set of entries.
     */
    private void assertChainsConsistent(Object[] table, int expectedSize) throws Exception {
        Set<Object> byKey = entriesByKey(table);
        Set<Object> byValue = entriesByValue(table);
        assertEquals(expectedSize, byKey.size(),
                "Key chain must contain " + expectedSize + " entries");
        assertEquals(expectedSize, byValue.size(),
                "Value chain must contain " + expectedSize + " entries");
        assertEquals(byKey, byValue,
                "Key and value chains must reference the same set of entries");
    }

    // ─── Chain integrity after insertions ─────────────────────────────────────

    @Test
    public void testChainsIntegrityAfterInsertions() throws Exception {
        // Risk: every put() must insert the entry into both the key chain and
        // the value chain. A bug that inserts into only one chain would be
        // invisible through the public API for entries that are never removed.
        UnsynchronizedSymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);

        assertChainsConsistent(getTable(map), 4);
    }

    // ─── Chain integrity after removal ────────────────────────────────────────

    @Test
    public void testChainsIntegrityAfterRemoveByKey() throws Exception {
        // Risk: remove(key) must unlink the entry from both chains. A bug that
        // only removes from the key chain would leave an orphaned entry in the
        // value chain, making containsValue() return true for a removed entry.
        UnsynchronizedSymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.remove("b");

        assertChainsConsistent(getTable(map), 2);
        assertFalse(map.containsValue(2),
                "Value chain must not contain the value of a removed entry");
    }

    @Test
    public void testChainsIntegrityAfterRemoveByValue() throws Exception {
        // Risk: removeByValue() must unlink the entry from both chains, symmetric
        // to remove(key). A bug that only removes from the value chain would leave
        // an orphaned entry in the key chain, making containsKey() return true
        // for a removed entry.
        UnsynchronizedSymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.removeByValue(2);

        assertChainsConsistent(getTable(map), 2);
        assertFalse(map.containsKey("b"),
                "Key chain must not contain the key of a removed entry");
    }

    // ─── Chain integrity after put with conflict ───────────────────────────────

    @Test
    public void testChainsIntegrityAfterPut_KeyConflict() throws Exception {
        // Risk: when put() detects an existing key, it must remove the old entry
        // from both chains before inserting the new one. A partial removal would
        // leave the old value reachable via the value chain.
        UnsynchronizedSymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("a", 99); // replaces "a" -> 1 with "a" -> 99

        assertChainsConsistent(getTable(map), 2);
        assertFalse(map.containsValue(1),
                "Old value must not remain reachable in the value chain");
        assertEquals(99, map.get("a"));
    }

    @Test
    public void testChainsIntegrityAfterPut_ValueConflict() throws Exception {
        // Risk: when put() detects an existing value, it must remove the
        // conflicting entry from both chains before inserting the new one.
        // A partial removal would leave the old key reachable via the key chain.
        UnsynchronizedSymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 1); // value 1 already used by "a" — "a" must be removed

        assertChainsConsistent(getTable(map), 2);
        assertFalse(map.containsKey("a"),
                "Conflicting key must not remain reachable in the key chain");
        assertEquals(1, map.get("c"));
    }

    // ─── Chain integrity after clear ──────────────────────────────────────────

    @Test
    public void testChainsIntegrityAfterClear() throws Exception {
        // Risk: clear() must nullify firstByKey and firstByValue in every bucket.
        // A partial clear that only resets one chain would cause containsKey() or
        // containsValue() to return stale results after clear().
        UnsynchronizedSymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.clear();

        assertChainsConsistent(getTable(map), 0);
        assertEquals(0, map.size());
    }

    // ─── Hash collisions on keys ───────────────────────────────────────────────

    @Test
    public void testKeyHashCollisions_AllEntriesInSameBucket() throws Exception {
        // Risk: when multiple keys hash to the same bucket index, all entries
        // must be linked correctly in the key chain of that bucket. A bug in
        // the chain insertion or lookup could cause entries to shadow each other.
        class CollidingKey {
            private final int value;
            private final int hash;

            CollidingKey(int value, int hash) {
                this.value = value;
                this.hash = hash;
            }

            @Override
            public int hashCode() { return hash; }

            @Override
            public boolean equals(Object o) {
                if (!(o instanceof CollidingKey)) return false;
                return ((CollidingKey) o).value == this.value;
            }

            @Override
            public String toString() { return "Key" + value; }
        }

        UnsynchronizedSymmetricMap<CollidingKey, Integer> map =
                new UnsynchronizedSymmetricMap<>();
        // All three keys hash to 0 — they must all end up in the same bucket
        CollidingKey k1 = new CollidingKey(1, 0);
        CollidingKey k2 = new CollidingKey(2, 0);
        CollidingKey k3 = new CollidingKey(3, 0);

        map.put(k1, 10);
        map.put(k2, 20);
        map.put(k3, 30);

        assertEquals(10, map.get(k1));
        assertEquals(20, map.get(k2));
        assertEquals(30, map.get(k3));
        assertEquals(3, map.size());

        // All three entries must be linked in the key chain of bucket 0
        Object[] table = getTable(map);
        Object bucket = table[0];
        Field firstByKeyField = bucket.getClass().getDeclaredField("firstByKey");
        firstByKeyField.setAccessible(true);
        Set<Object> entriesInKeyChain = new HashSet<>();
        Object entry = firstByKeyField.get(bucket);
        while (entry != null) {
            entriesInKeyChain.add(entry);
            Field nextByKeyField = entry.getClass().getDeclaredField("nextByKey");
            nextByKeyField.setAccessible(true);
            entry = nextByKeyField.get(entry);
        }
        assertEquals(3, entriesInKeyChain.size(),
                "All three colliding keys must be in the key chain of bucket 0");
    }

    // ─── Hash collisions on values ─────────────────────────────────────────────

    @Test
    public void testValueHashCollisions_AllEntriesInSameBucket() throws Exception {
        // Risk: when multiple values hash to the same bucket index, all entries
        // must be linked correctly in the value chain of that bucket. This is the
        // symmetric risk of key hash collisions — a bug here would only be visible
        // via getKey() or removeByValue(), not via get().
        class CollidingValue {
            private final int value;
            private final int hash;

            CollidingValue(int value, int hash) {
                this.value = value;
                this.hash = hash;
            }

            @Override
            public int hashCode() { return hash; }

            @Override
            public boolean equals(Object o) {
                if (!(o instanceof CollidingValue)) return false;
                return ((CollidingValue) o).value == this.value;
            }

            @Override
            public String toString() { return "Value" + value; }
        }

        UnsynchronizedSymmetricMap<Integer, CollidingValue> map =
                new UnsynchronizedSymmetricMap<>();
        // All three values hash to 0 — they must all end up in the same bucket
        CollidingValue v1 = new CollidingValue(1, 0);
        CollidingValue v2 = new CollidingValue(2, 0);
        CollidingValue v3 = new CollidingValue(3, 0);

        map.put(10, v1);
        map.put(20, v2);
        map.put(30, v3);

        assertEquals(10, map.getKey(v1).get());
        assertEquals(20, map.getKey(v2).get());
        assertEquals(30, map.getKey(v3).get());
        assertEquals(3, map.size());

        // All three entries must be linked in the value chain of bucket 0
        Object[] table = getTable(map);
        Object bucket = table[0];
        Field firstByValueField = bucket.getClass().getDeclaredField("firstByValue");
        firstByValueField.setAccessible(true);
        Set<Object> entriesInValueChain = new HashSet<>();
        Object entry = firstByValueField.get(bucket);
        while (entry != null) {
            entriesInValueChain.add(entry);
            Field nextByValueField = entry.getClass().getDeclaredField("nextByValue");
            nextByValueField.setAccessible(true);
            entry = nextByValueField.get(entry);
        }
        assertEquals(3, entriesInValueChain.size(),
                "All three colliding values must be in the value chain of bucket 0");
    }

    // ─── Resize ────────────────────────────────────────────────────────────────

    @Test
    public void testResize_ChainsRebuiltCorrectly() throws Exception {
        // Risk: after a resize, every entry must be reinserted into both the key
        // chain and the value chain of the new table. A bug that only rebuilds
        // one chain would make half of the reverse lookups fail silently.
        UnsynchronizedSymmetricMap<Integer, Integer> map =
                new UnsynchronizedSymmetricMap<>(4, 0.75f);

        // Insert enough entries to trigger at least one resize
        for (int i = 0; i < 20; i++) {
            map.put(i, i + 100);
        }

        assertEquals(20, map.size());

        // All entries must remain reachable in both directions after resize
        for (int i = 0; i < 20; i++) {
            assertEquals(i + 100, map.get(i));
            assertEquals(i, map.getKey(i + 100).get());
        }

        assertChainsConsistent(getTable(map), 20);
    }
}