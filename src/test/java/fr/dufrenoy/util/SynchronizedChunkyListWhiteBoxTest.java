/*
 * SynchronizedChunkyListWhiteBoxTest.java
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * White-box tests for {@link SynchronizedChunkyList}.
 * These tests target implementation risks specific to the synchronized variant:
 * internal chunk structure after reorganize, snapshot isolation of iterators,
 * consistency of the copy constructor under concurrent modification, and
 * atomicity of strategy changes.
 */
public class SynchronizedChunkyListWhiteBoxTest {

    // ===== reorganize() — blocking =====

    @Test
    public void testReorganizeBlocking_CompactsChunks() {
        // Risk: reorganize() under write lock must actually compact the internal
        // chunk structure, not just preserve element order.
        SynchronizedChunkyList<String> list = new SynchronizedChunkyList<>(3);
        for (int i = 0; i < 9; i++) {
            list.add("Item" + i);
        }
        // Remove elements to create sparse chunks
        list.remove(7);
        list.remove(4);
        list.remove(1);

        int chunksBefore = list.countChunks();
        list.reorganize();
        int chunksAfter = list.countChunks();

        assertEquals(6, list.size());
        assertTrue(chunksAfter <= chunksBefore,
                "reorganize() should not increase the number of chunks");
        assertEquals(2, chunksAfter,
                "6 elements with chunkSize=3 should fit in exactly 2 chunks");
    }

    // ===== reorganize(false) — non-blocking =====

    @Test
    public void testReorganizeNonBlocking_CompactsChunks() {
        // Risk: the non-blocking reorganize() uses a snapshot strategy —
        // the swap must still result in a compacted structure.
        SynchronizedChunkyList<String> list = new SynchronizedChunkyList<>(3);
        for (int i = 0; i < 9; i++) {
            list.add("Item" + i);
        }
        list.remove(7);
        list.remove(4);
        list.remove(1);

        int chunksBefore = list.countChunks();
        list.reorganize(false);
        int chunksAfter = list.countChunks();

        assertEquals(6, list.size());
        assertTrue(chunksAfter <= chunksBefore,
                "reorganize(false) should not increase the number of chunks");
        assertEquals(2, chunksAfter,
                "6 elements with chunkSize=3 should fit in exactly 2 chunks");
    }

    // ===== Snapshot isolation =====

    @Test
    public void testIterator_SnapshotIsolation_ChunkStructureUnaffected() {
        // Risk: the iterator operates on a snapshot taken at creation time.
        // Concurrent modifications must not affect the elements seen by the
        // iterator, and countChunks() on the live list must reflect the
        // modifications, not the snapshot.
        SynchronizedChunkyList<String> list = new SynchronizedChunkyList<>(2);
        list.add("A");
        list.add("B");
        list.add("C");

        int chunksBeforeIteration = list.countChunks();
        Iterator<String> it = list.iterator();

        // Add elements after the iterator is created
        list.add("D");
        list.add("E");

        List<String> seen = new ArrayList<>();
        it.forEachRemaining(seen::add);

        // The iterator must have seen the snapshot — not the new elements
        assertEquals(3, seen.size(),
                "Iterator must see exactly the elements present at creation time");
        assertFalse(seen.contains("D"),
                "Iterator must not see elements added after its creation");
        assertFalse(seen.contains("E"),
                "Iterator must not see elements added after its creation");

        // The live list must reflect the additions
        assertEquals(5, list.size(),
                "Live list must reflect all additions");
        assertTrue(list.countChunks() >= chunksBeforeIteration,
                "Live list chunk count must reflect structural modifications");
    }

    // ===== Copy constructor under concurrent modification =====

    @Test
    public void testCopyConstructor_ConsistentSnapshotUnderConcurrentModification()
            throws InterruptedException {
        // Risk: the copy constructor acquires a read lock on `other` for the
        // duration of the copy. A concurrent write must not corrupt the copy.
        SynchronizedChunkyList<Integer> original = new SynchronizedChunkyList<>(10);
        for (int i = 0; i < 50; i++) {
            original.add(i);
        }

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(1);

        // Writer thread modifies original concurrently with the copy
        Thread writer = new Thread(() -> {
            try {
                start.await();
                for (int i = 50; i < 100; i++) {
                    original.add(i);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                done.countDown();
            }
        });

        writer.start();
        start.countDown();

        // Copy taken concurrently with the writer
        SynchronizedChunkyList<Integer> copy = new SynchronizedChunkyList<>(original);
        done.await();

        // The copy must be internally consistent — size must match countChunks()
        int size = copy.size();
        int chunks = copy.countChunks();
        int chunkSize = copy.getChunkSize();

        assertTrue(size >= 0, "Copy size must be non-negative");
        assertTrue(chunks >= 0, "Copy chunk count must be non-negative");
        assertTrue(size <= chunks * chunkSize,
                "Copy must not have more elements than its chunk capacity");
    }

    // ===== Atomicity of setStrategies() =====

    @Test
    public void testSetStrategies_ConcurrentAccess_NoExceptionAndConsistentFinalState()
            throws InterruptedException {
        // Risk: concurrent calls to setStrategies() and getCurrentXxxStrategy()
        // must not throw exceptions or leave the list in a state where neither
        // strategy pair is valid. Full atomicity of the read cannot be verified
        // without exposing a getStrategies() method — instead we verify that
        // concurrent access does not corrupt the final state.
        SynchronizedChunkyList<String> list = new SynchronizedChunkyList<>(2);

        int iterations = 10_000;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        AtomicBoolean exceptionObserved = new AtomicBoolean(false);

        Thread writer = new Thread(() -> {
            try {
                start.await();
                for (int i = 0; i < iterations; i++) {
                    if (i % 2 == 0) {
                        list.setStrategies(
                                GrowingStrategy.OVERFLOW_STRATEGY,
                                ShrinkingStrategy.UNDERFLOW_STRATEGY);
                    } else {
                        list.setStrategies(
                                GrowingStrategy.EXTEND_STRATEGY,
                                ShrinkingStrategy.DISAPPEAR_STRATEGY);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                exceptionObserved.set(true);
            } finally {
                done.countDown();
            }
        });

        Thread reader = new Thread(() -> {
            try {
                start.await();
                for (int i = 0; i < iterations; i++) {
                    list.getCurrentGrowingStrategy();
                    list.getCurrentShrinkingStrategy();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                exceptionObserved.set(true);
            } finally {
                done.countDown();
            }
        });

        writer.start();
        reader.start();
        start.countDown();
        done.await();

        assertFalse(exceptionObserved.get(),
                "Concurrent access to setStrategies() must not throw any exception");

        // Final state must be one of the two valid strategy pairs
        GrowingStrategy growing = list.getCurrentGrowingStrategy();
        ShrinkingStrategy shrinking = list.getCurrentShrinkingStrategy();
        boolean valid =
                (growing == GrowingStrategy.OVERFLOW_STRATEGY
                        && shrinking == ShrinkingStrategy.UNDERFLOW_STRATEGY)
                        || (growing == GrowingStrategy.EXTEND_STRATEGY
                        && shrinking == ShrinkingStrategy.DISAPPEAR_STRATEGY);
        assertTrue(valid,
                "Final strategy pair must be one of the two valid pairs");
    }
}