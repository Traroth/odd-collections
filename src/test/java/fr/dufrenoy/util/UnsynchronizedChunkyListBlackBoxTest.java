/*
 * UnsynchronizedChunkyListBlackBoxTest.java
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

import static fr.dufrenoy.util.ChunkyList.GrowingStrategy;
import static fr.dufrenoy.util.ChunkyList.ShrinkingStrategy;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

/**
 * Black-box tests for {@link UnsynchronizedChunkyList}, based solely on the
 * public contract. No knowledge of the internal implementation is assumed.
 */
public class UnsynchronizedChunkyListBlackBoxTest {

    // ===== reorganize tests =====

    @Test
    public void testReorganize_PreservesOrder() {
        UnsynchronizedChunkyList<String> list = new UnsynchronizedChunkyList<>(3);
        for (int i = 0; i < 20; i++) {
            list.add("Item" + i);
        }

        List<String> before = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            before.add(list.get(i));
        }

        list.reorganize();

        for (int i = 0; i < list.size(); i++) {
            assertEquals(before.get(i), list.get(i));
        }
    }

    // ===== Spliterator and Stream tests =====

    @Test
    public void testSpliterator_Characteristics() {
        UnsynchronizedChunkyList<String> list = new UnsynchronizedChunkyList<>(2);
        list.add("A");
        list.add("B");
        list.add("C");

        Spliterator<String> spliterator = list.spliterator();
        assertTrue(spliterator.hasCharacteristics(Spliterator.SIZED));
        assertTrue(spliterator.hasCharacteristics(Spliterator.SUBSIZED));
        assertTrue(spliterator.hasCharacteristics(Spliterator.ORDERED));
    }

    @Test
    public void testSpliterator_TryAdvance() {
        UnsynchronizedChunkyList<String> list = new UnsynchronizedChunkyList<>(2);
        list.add("A");
        list.add("B");
        list.add("C");

        Spliterator<String> spliterator = list.spliterator();
        List<String> result = new ArrayList<>();
        while (spliterator.tryAdvance(result::add));
        assertEquals(Arrays.asList("A", "B", "C"), result);
    }

    @Test
    public void testStream_Sequential() {
        UnsynchronizedChunkyList<String> list = new UnsynchronizedChunkyList<>(2);
        list.add("A");
        list.add("B");
        list.add("C");

        List<String> result = list.stream()
                .map(String::toLowerCase)
                .collect(java.util.stream.Collectors.toList());
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testStream_Parallel() {
        UnsynchronizedChunkyList<Integer> list = new UnsynchronizedChunkyList<>(10);
        for (int i = 0; i < 100; i++) list.add(i);

        List<Integer> result = list.parallelStream()
                .filter(i -> i % 2 == 0)
                .collect(java.util.stream.Collectors.toList());
        assertEquals(50, result.size());
        assertTrue(result.stream().allMatch(i -> i % 2 == 0));
    }

    // ===== Copy constructor with new chunkSize tests =====

    @Test
    public void testCopyConstructor_MixedChunks_PreservesOrder() {
        UnsynchronizedChunkyList<String> original = new UnsynchronizedChunkyList<>(4);
        original.add("A");
        original.add("B");
        original.add("C");
        original.add("D");
        original.add("E");

        UnsynchronizedChunkyList<String> copy = new UnsynchronizedChunkyList<>(2, original);

        assertEquals(5, copy.size());
        for (int i = 0; i < 5; i++) {
            assertEquals(original.get(i), copy.get(i));
        }
    }

    @Test
    public void testCopyConstructor_PreservesStrategies() {
        UnsynchronizedChunkyList<String> original = new UnsynchronizedChunkyList<>(4);
        original.setStrategies(GrowingStrategy.EXTEND_STRATEGY, ShrinkingStrategy.DISAPPEAR_STRATEGY);
        original.add("A");
        original.add("B");

        UnsynchronizedChunkyList<String> copy = new UnsynchronizedChunkyList<>(2, original);

        assertEquals(GrowingStrategy.EXTEND_STRATEGY, copy.getCurrentGrowingStrategy());
        assertEquals(ShrinkingStrategy.DISAPPEAR_STRATEGY, copy.getCurrentShrinkingStrategy());
    }

    @Test
    public void testAdd_Null_Throws() {
        UnsynchronizedChunkyList<String> list = new UnsynchronizedChunkyList<>();
        assertThrows(IllegalArgumentException.class, () -> list.add(null));
    }

    @Test
    public void testAddAtIndex_Null_Throws() {
        UnsynchronizedChunkyList<String> list = new UnsynchronizedChunkyList<>();
        list.add("A");
        assertThrows(IllegalArgumentException.class, () -> list.add(0, null));
    }

    @Test
    public void testSet_Null_Throws() {
        UnsynchronizedChunkyList<String> list = new UnsynchronizedChunkyList<>();
        list.add("A");
        assertThrows(IllegalArgumentException.class, () -> list.set(0, null));
    }
}