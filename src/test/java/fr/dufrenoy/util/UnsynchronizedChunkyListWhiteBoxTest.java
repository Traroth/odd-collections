/*
 * UnsynchronizedChunkyListWhiteBoxTest.java
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
 * White-box tests for {@link UnsynchronizedChunkyList}.
 * These tests access internal details to verify the chunk structure.
 */
public class UnsynchronizedChunkyListWhiteBoxTest {

    // ===== Internal chunk structure tests =====

    @Test
    public void testInternalChunkStructure_WithSmallChunks() {
        UnsynchronizedChunkyList<String> list = new UnsynchronizedChunkyList<>(2);
        list.add("A");
        list.add("B");
        list.add("C");

        assertEquals(3, list.size());
        assertEquals(2, list.countChunks());
    }

    @Test
    public void testInternalChunkStructure_WithLargeChunks() {
        UnsynchronizedChunkyList<String> list = new UnsynchronizedChunkyList<>(100);
        for (int i = 0; i < 250; i++) {
            list.add("Item" + i);
        }

        assertEquals(250, list.size());
        assertEquals(3, list.countChunks());
    }

    @Test
    public void testInternalChunkStructure_AfterRemovals() {
        UnsynchronizedChunkyList<String> list = new UnsynchronizedChunkyList<>(3);
        for (int i = 0; i < 10; i++) {
            list.add("Item" + i);
        }
        list.remove(5);
        list.remove(3);
        list.remove(1);

        assertEquals(7, list.size());
        assertTrue(list.countChunks() >= 2);
    }

    // ===== Strategy tests =====

    @Test
    public void testOverflowStrategy() {
        UnsynchronizedChunkyList<String> list = new UnsynchronizedChunkyList<>(2);
        list.setCurrentGrowingStrategy(GrowingStrategy.OVERFLOW_STRATEGY);
        list.add("A");
        list.add("B");
        list.add("C");

        assertEquals(3, list.size());
        assertEquals(2, list.countChunks());
    }

    @Test
    public void testExtendStrategy() {
        UnsynchronizedChunkyList<String> list = new UnsynchronizedChunkyList<>(2);
        list.setCurrentGrowingStrategy(GrowingStrategy.EXTEND_STRATEGY);
        list.add("A");
        list.add("B");
        list.add("C");

        assertEquals(3, list.size());
        assertEquals(2, list.countChunks());
    }

    @Test
    public void testUnderflowStrategy() {
        UnsynchronizedChunkyList<String> list = new UnsynchronizedChunkyList<>(2);
        list.setCurrentShrinkingStrategy(ShrinkingStrategy.UNDERFLOW_STRATEGY);
        list.add("A");
        list.add("B");
        list.add("C");
        list.remove(0);

        assertEquals(2, list.size());
        assertEquals(1, list.countChunks());
    }

    @Test
    public void testDisappearStrategy() {
        UnsynchronizedChunkyList<String> list = new UnsynchronizedChunkyList<>(2);
        list.setCurrentShrinkingStrategy(ShrinkingStrategy.DISAPPEAR_STRATEGY);
        list.add("A");
        list.add("B");
        list.add("C");
        list.remove(0);
        list.remove(0);

        assertEquals(1, list.size());
        assertEquals(1, list.countChunks());
    }

    // ===== reorganize tests =====

    @Test
    public void testReorganize_WithSparseChunks() {
        UnsynchronizedChunkyList<String> list = new UnsynchronizedChunkyList<>(3);
        for (int i = 0; i < 10; i++) {
            list.add("Item" + i);
        }
        list.remove(5);
        list.remove(3);
        list.remove(1);

        int chunksBefore = list.countChunks();
        list.reorganize();
        int chunksAfter = list.countChunks();

        assertEquals(7, list.size());
        assertTrue(chunksAfter <= chunksBefore);
    }

    // ===== Copy constructor with new chunkSize tests =====

    @Test
    public void testCopyConstructor_ChunksFit_PreservesStructure() {
        UnsynchronizedChunkyList<String> original = new UnsynchronizedChunkyList<>(2);
        original.add("A");
        original.add("B");
        original.add("C");
        original.add("D");

        UnsynchronizedChunkyList<String> copy = new UnsynchronizedChunkyList<>(4, original);

        assertEquals(4, copy.size());
        assertEquals("A", copy.get(0));
        assertEquals("B", copy.get(1));
        assertEquals("C", copy.get(2));
        assertEquals("D", copy.get(3));
        assertEquals(2, copy.countChunks());
    }

    @Test
    public void testCopyConstructor_ChunksExceed_OverflowStrategy() {
        UnsynchronizedChunkyList<String> original = new UnsynchronizedChunkyList<>(4);
        original.add("A");
        original.add("B");
        original.add("C");
        original.add("D");

        UnsynchronizedChunkyList<String> copy = new UnsynchronizedChunkyList<>(2, original);
        copy.setCurrentGrowingStrategy(GrowingStrategy.OVERFLOW_STRATEGY);

        assertEquals(4, copy.size());
        assertEquals("A", copy.get(0));
        assertEquals("B", copy.get(1));
        assertEquals("C", copy.get(2));
        assertEquals("D", copy.get(3));
        assertEquals(2, copy.countChunks());
    }

    @Test
    public void testCopyConstructor_ChunksExceed_ExtendStrategy() {
        UnsynchronizedChunkyList<String> original = new UnsynchronizedChunkyList<>(4);
        original.add("A");
        original.add("B");
        original.add("C");
        original.add("D");

        UnsynchronizedChunkyList<String> copy = new UnsynchronizedChunkyList<>(2, original);
        copy.setCurrentGrowingStrategy(GrowingStrategy.EXTEND_STRATEGY);

        assertEquals(4, copy.size());
        assertEquals("A", copy.get(0));
        assertEquals("B", copy.get(1));
        assertEquals("C", copy.get(2));
        assertEquals("D", copy.get(3));
        assertEquals(2, copy.countChunks());
    }
}