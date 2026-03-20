/*
 * SynchronizedChunkyListBlackBoxTest.java
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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Black-box tests for {@link SynchronizedChunkyList}, based solely on the
 * public contract. Includes concurrency tests for thread-safety guarantees.
 */
public class SynchronizedChunkyListBlackBoxTest {

    // ===== Basic delegation tests =====

    @Test
    public void testAddAndGet() {
        SynchronizedChunkyList<String> list = new SynchronizedChunkyList<>(2);
        list.add("A");
        list.add("B");
        list.add("C");

        assertEquals(3, list.size());
        assertEquals("A", list.get(0));
        assertEquals("B", list.get(1));
        assertEquals("C", list.get(2));
    }

    @Test
    public void testRemoveAndSet() {
        SynchronizedChunkyList<String> list = new SynchronizedChunkyList<>(2);
        list.add("A");
        list.add("B");
        list.add("C");

        assertEquals("B", list.remove(1));
        assertEquals("A", list.set(0, "X"));
        assertEquals("X", list.get(0));
        assertEquals("C", list.get(1));
    }

    @Test
    public void testClearAndIsEmpty() {
        SynchronizedChunkyList<String> list = new SynchronizedChunkyList<>(2);
        list.add("A");
        list.add("B");
        list.clear();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    // ===== Constructor tests =====

    @Test
    public void testCopyConstructor_PreservesContent() {
        SynchronizedChunkyList<String> original = new SynchronizedChunkyList<>(2);
        original.add("A");
        original.add("B");
        original.add("C");
        original.setStrategies(GrowingStrategy.EXTEND_STRATEGY, ShrinkingStrategy.DISAPPEAR_STRATEGY);

        SynchronizedChunkyList<String> copy = new SynchronizedChunkyList<>(original);

        assertEquals(3, copy.size());
        assertEquals("A", copy.get(0));
        assertEquals("B", copy.get(1));
        assertEquals("C", copy.get(2));
        assertEquals(GrowingStrategy.EXTEND_STRATEGY, copy.getCurrentGrowingStrategy());
        assertEquals(ShrinkingStrategy.DISAPPEAR_STRATEGY, copy.getCurrentShrinkingStrategy());
    }

    @Test
    public void testCopyConstructor_Independence() {
        SynchronizedChunkyList<String> original = new SynchronizedChunkyList<>(2);
        original.add("A");
        original.add("B");

        SynchronizedChunkyList<String> copy = new SynchronizedChunkyList<>(original);
        copy.add("C");

        assertEquals(2, original.size());
        assertEquals(3, copy.size());
    }

    @Test
    public void testCopyConstructor_WithNewChunkSize() {
        SynchronizedChunkyList<String> original = new SynchronizedChunkyList<>(4);
        original.add("A");
        original.add("B");
        original.add("C");
        original.add("D");
        original.add("E");

        SynchronizedChunkyList<String> copy = new SynchronizedChunkyList<>(2, original);

        assertEquals(5, copy.size());
        assertEquals(2, copy.getChunkSize());
        for (int i = 0; i < 5; i++) {
            assertEquals(original.get(i), copy.get(i));
        }
    }

    @Test
    public void testConstructor_FromCollection() {
        List<String> source = Arrays.asList("A", "B", "C", "D", "E");
        SynchronizedChunkyList<String> list = new SynchronizedChunkyList<>(source);

        assertEquals(5, list.size());
        for (int i = 0; i < 5; i++) {
            assertEquals(source.get(i), list.get(i));
        }
    }

    // ===== Strategy tests =====

    @Test
    public void testSetStrategies_Atomic() {
        SynchronizedChunkyList<String> list = new SynchronizedChunkyList<>(2);
        list.setStrategies(GrowingStrategy.EXTEND_STRATEGY, ShrinkingStrategy.DISAPPEAR_STRATEGY);

        assertEquals(GrowingStrategy.EXTEND_STRATEGY, list.getCurrentGrowingStrategy());
        assertEquals(ShrinkingStrategy.DISAPPEAR_STRATEGY, list.getCurrentShrinkingStrategy());
    }

    @Test
    public void testSetGrowingStrategy() {
        SynchronizedChunkyList<String> list = new SynchronizedChunkyList<>(2);
        list.setCurrentGrowingStrategy(GrowingStrategy.EXTEND_STRATEGY);
        assertEquals(GrowingStrategy.EXTEND_STRATEGY, list.getCurrentGrowingStrategy());
    }

    @Test
    public void testSetShrinkingStrategy() {
        SynchronizedChunkyList<String> list = new SynchronizedChunkyList<>(2);
        list.setCurrentShrinkingStrategy(ShrinkingStrategy.DISAPPEAR_STRATEGY);
        assertEquals(ShrinkingStrategy.DISAPPEAR_STRATEGY, list.getCurrentShrinkingStrategy());
    }

    // ===== reorganize tests =====

    @Test
    public void testReorganize_Blocking_PreservesOrder() {
        SynchronizedChunkyList<String> list = new SynchronizedChunkyList<>(3);
        for (int i = 0; i < 20; i++) list.add("Item" + i);

        List<String> before = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) before.add(list.get(i));

        list.reorganize();

        assertEquals(before.size(), list.size());
        for (int i = 0; i < list.size(); i++) assertEquals(before.get(i), list.get(i));
    }

    @Test
    public void testReorganize_NonBlocking_PreservesOrder() {
        SynchronizedChunkyList<String> list = new SynchronizedChunkyList<>(3);
        for (int i = 0; i < 20; i++) list.add("Item" + i);

        List<String> before = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) before.add(list.get(i));

        list.reorganize(false);

        assertEquals(before.size(), list.size());
        for (int i = 0; i < list.size(); i++) assertEquals(before.get(i), list.get(i));
    }

    // ===== Thread-safety tests =====

    @Test
    public void testConcurrentWrites_PreservesSize() throws InterruptedException {
        SynchronizedChunkyList<Integer> list = new SynchronizedChunkyList<>();
        int nThreads = 10;
        int nPerThread = 100;
        CountDownLatch ready = new CountDownLatch(nThreads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(nThreads);

        for (int t = 0; t < nThreads; t++) {
            final int base = t * nPerThread;
            new Thread(() -> {
                ready.countDown();
                try { start.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                for (int i = 0; i < nPerThread; i++) list.add(base + i);
                done.countDown();
            }).start();
        }

        ready.await();
        start.countDown();
        done.await();

        assertEquals(nThreads * nPerThread, list.size());
    }

    @Test
    public void testConcurrentReadsAndWrites_NoException() throws InterruptedException {
        SynchronizedChunkyList<Integer> list = new SynchronizedChunkyList<>();
        for (int i = 0; i < 100; i++) list.add(i);

        int nThreads = 10;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(nThreads);
        AtomicInteger errors = new AtomicInteger(0);

        for (int t = 0; t < nThreads; t++) {
            final boolean writer = t % 2 == 0;
            new Thread(() -> {
                try { start.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                try {
                    if (writer) {
                        for (int i = 0; i < 50; i++) list.add(i);
                    } else {
                        for (int i = 0; i < list.size(); i++) list.get(i);
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    done.countDown();
                }
            }).start();
        }

        start.countDown();
        done.await();

        assertEquals(0, errors.get());
    }

    // ===== Snapshot tests =====

    @Test
    public void testIterator_IsSnapshot() {
        SynchronizedChunkyList<String> list = new SynchronizedChunkyList<>();
        list.add("A");
        list.add("B");
        list.add("C");

        Iterator<String> it = list.iterator();
        list.add("D"); // modification after iterator creation

        List<String> result = new ArrayList<>();
        it.forEachRemaining(result::add);

        // The iterator must not see "D"
        assertEquals(Arrays.asList("A", "B", "C"), result);
    }

    @Test
    public void testSpliterator_IsSnapshot() {
        SynchronizedChunkyList<String> list = new SynchronizedChunkyList<>();
        list.add("A");
        list.add("B");
        list.add("C");

        Spliterator<String> sp = list.spliterator();
        list.add("D"); // modification after spliterator creation

        List<String> result = new ArrayList<>();
        sp.forEachRemaining(result::add);

        assertEquals(Arrays.asList("A", "B", "C"), result);
    }

    @Test
    public void testStream_IsSnapshot() {
        SynchronizedChunkyList<String> list = new SynchronizedChunkyList<>();
        list.add("A");
        list.add("B");
        list.add("C");

        List<String> result = list.stream()
                .peek(e -> list.add("X")) // attempt to modify during stream
                .collect(Collectors.toList());

        assertEquals(Arrays.asList("A", "B", "C"), result);
    }

    @Test
    public void testAdd_Null_Throws() {
        SynchronizedChunkyList<String> list = new SynchronizedChunkyList<>();
        assertThrows(IllegalArgumentException.class, () -> list.add(null));
    }

    @Test
    public void testAddAtIndex_Null_Throws() {
        SynchronizedChunkyList<String> list = new SynchronizedChunkyList<>();
        list.add("A");
        assertThrows(IllegalArgumentException.class, () -> list.add(0, null));
    }

    @Test
    public void testSet_Null_Throws() {
        SynchronizedChunkyList<String> list = new SynchronizedChunkyList<>();
        list.add("A");
        assertThrows(IllegalArgumentException.class, () -> list.set(0, null));
    }
}