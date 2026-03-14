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
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * White-box tests pour UnsynchronizedSymmetricMap :
 * - Vérification de l'intégrité des chaînes de collisions (par clé et par valeur)
 * - Invariants internes (bijectivité, absence de doublons, cohérence des chaînes)
 * - Cas limites (collisions de hash, redimensionnement, réutilisation d'Entry)
 */
public class UnsynchronizedSymmetricMapWhiteBoxTest {
    // ...tests à implémenter...

    @Test
    public void testChainsIntegrityAfterInsertions() throws Exception {
        UnsynchronizedSymmetricMap<String, Integer> map = new UnsynchronizedSymmetricMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);

        // Accès à la table interne via la réflexion
        Field tableField = UnsynchronizedSymmetricMap.class.getDeclaredField("table");
        tableField.setAccessible(true);
        Object[] table = (Object[]) tableField.get(map);

        Set<Object> entriesByKey = new HashSet<>();
        Set<Object> entriesByValue = new HashSet<>();

        // Parcours des chaînes par clé
        for (Object bucket : table) {
            if (bucket == null) continue;
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
        // Parcours des chaînes par valeur
        for (Object bucket : table) {
            if (bucket == null) continue;
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
        // Les deux ensembles doivent être égaux et de la bonne taille
        assertEquals(4, entriesByKey.size());
        assertEquals(4, entriesByValue.size());
        assertEquals(entriesByKey, entriesByValue);
    }

    @Test
    public void testHashCollisions_IntegrityPreserved() throws Exception {
        // Créer des clés avec le même hash pour forcer des collisions
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
                if (!(o instanceof CollidingKey)) return false;
                return ((CollidingKey) o).value == this.value;
            }

            @Override
            public String toString() {
                return "Key" + value;
            }
        }

        UnsynchronizedSymmetricMap<CollidingKey, Integer> map = new UnsynchronizedSymmetricMap<>();
        // Toutes ces clés ont le même hash (0)
        CollidingKey k1 = new CollidingKey(1, 0);
        CollidingKey k2 = new CollidingKey(2, 0);
        CollidingKey k3 = new CollidingKey(3, 0);

        map.put(k1, 10);
        map.put(k2, 20);
        map.put(k3, 30);

        // Vérifier que toutes les entrées sont présentes et accessibles
        assertEquals(10, map.get(k1));
        assertEquals(20, map.get(k2));
        assertEquals(30, map.get(k3));
        assertEquals(3, map.size());

        // Vérifier l'intégrité des chaînes internes
        Field tableField = UnsynchronizedSymmetricMap.class.getDeclaredField("table");
        tableField.setAccessible(true);
        Object[] table = (Object[]) tableField.get(map);

        // Toutes les entrées devraient être dans le même bucket (index 0)
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
        UnsynchronizedSymmetricMap<Integer, Integer> map = new UnsynchronizedSymmetricMap<>(4, 0.75f); // Petite capacité pour forcer le resize tôt

        // Ajouter suffisamment d'éléments pour déclencher un resize
        for (int i = 0; i < 20; i++) {
            map.put(i, i + 100);
        }

        // Vérifier que la taille est correcte
        assertEquals(20, map.size());

        // Vérifier que toutes les entrées sont accessibles
        for (int i = 0; i < 20; i++) {
            assertEquals(i + 100, map.get(i));
            assertEquals(i, map.getKey(i + 100).get());
        }

        // Vérifier l'intégrité des chaînes internes après resize
        Field tableField = UnsynchronizedSymmetricMap.class.getDeclaredField("table");
        tableField.setAccessible(true);
        Object[] table = (Object[]) tableField.get(map);

        Set<Object> entriesByKey = new HashSet<>();
        Set<Object> entriesByValue = new HashSet<>();

        // Parcours des chaînes par clé
        for (Object bucket : table) {
            if (bucket == null) continue;
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
        // Parcours des chaînes par valeur
        for (Object bucket : table) {
            if (bucket == null) continue;
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
        // Les deux ensembles doivent être égaux et de la bonne taille
        assertEquals(20, entriesByKey.size());
        assertEquals(20, entriesByValue.size());
        assertEquals(entriesByKey, entriesByValue);
    }
}
