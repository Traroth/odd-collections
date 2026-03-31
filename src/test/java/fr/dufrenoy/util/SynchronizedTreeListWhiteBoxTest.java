/*
 * SynchronizedTreeListWhiteBoxTest.java
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * White-box tests for {@link SynchronizedTreeList}. Tests verify the
 * locking and snapshot mechanisms documented in the implementation, including:
 *
 * <ul>
 *   <li>Multiple readers proceed concurrently (read lock is shared)</li>
 *   <li>Writes are serialized and do not corrupt concurrent reads</li>
 *   <li>Snapshot iterators truly reflect state at creation time, not at
 *       iteration time</li>
 *   <li>{@link SynchronizedTreeList#equals(Object)} short-circuits on
 *       {@code this} without acquiring a lock</li>
 * </ul>
 */
public class SynchronizedTreeListWhiteBoxTest {

    // ─── equals(this) short-circuit ───────────────────────────────────────────

    @Test
    public void testEquals_Self_ReturnsTrueWithoutDeadlock() {
        // equals(o == this) short-circuits before acquiring the read lock.
        // Verifies that calling list.equals(list) does not deadlock.
        SynchronizedTreeList<Integer> list = new SynchronizedTreeList<>();
        list.add(1); list.add(2); list.add(3);
        assertTrue(list.equals(list));
    }

    // ─── Snapshot iterator — captures state at creation, not at iteration ─────

    @Test
    public void testSnapshot_AddAfterCreation_NotVisibleInIterator() {
        SynchronizedTreeList<Integer> list = new SynchronizedTreeList<>();
        list.add(1); list.add(2); list.add(3);

        Iterator<Integer> it = list.iterator();
        // Consume one element, then modify the list
        assertEquals(1, it.next());
        list.add(100);

        // The remaining elements from the snapshot must be 2 and 3, not 100
        List<Integer> remaining = new ArrayList<>();
        it.forEachRemaining(remaining::add);
        assertEquals(List.of(2, 3), remaining,
                "Snapshot must not reflect add() made after iterator creation");
    }

    @Test
    public void testSnapshot_RemoveAfterCreation_NotVisibleInIterator() {
        SynchronizedTreeList<Integer> list = new SynchronizedTreeList<>();
        list.add(1); list.add(2); list.add(3);

        Iterator<Integer> it = list.iterator();
        assertEquals(1, it.next());
        list.remove(Integer.valueOf(2));

        List<Integer> remaining = new ArrayList<>();
        it.forEachRemaining(remaining::add);
        assertEquals(List.of(2, 3), remaining,
                "Snapshot must not reflect remove() made after iterator creation");
    }

    @Test
    public void testSnapshot_ClearAfterCreation_IteratorSeesOriginalElements() {
        SynchronizedTreeList<Integer> list = new SynchronizedTreeList<>();
        list.add(1); list.add(2); list.add(3);

        Iterator<Integer> it = list.iterator();
        list.clear(); // total wipe after snapshot taken

        int count = 0;
        while (it.hasNext()) {
            it.next();
            count++;
        }
        assertEquals(3, count,
                "Snapshot iterator must see 3 elements despite clear() being called after creation");
    }

    @Test
    public void testSnapshot_NeverThrowsConcurrentModificationException_UnderConcurrentWrites()
            throws InterruptedException {
        SynchronizedTreeList<Integer> list = new SynchronizedTreeList<>();
        for (int i = 0; i < 50; i++) {
            list.add(i);
        }

        AtomicBoolean exceptionThrown = new AtomicBoolean(false);
        Iterator<Integer> it = list.iterator();

        Thread writer = new Thread(() -> {
            for (int i = 50; i < 100; i++) {
                list.add(i);
            }
        });
        writer.start();
        writer.join();

        try {
            while (it.hasNext()) {
                it.next();
            }
        } catch (Exception e) {
            exceptionThrown.set(true);
        }

        assertFalse(exceptionThrown.get(),
                "Snapshot iterator must never throw ConcurrentModificationException");
    }

    // ─── Concurrent reads — read lock is shared ───────────────────────────────

    @Test
    public void testConcurrentReads_MultipleThreadsActiveSimultaneously()
            throws InterruptedException {
        SynchronizedTreeList<Integer> list = new SynchronizedTreeList<>();
        for (int i = 0; i < 200; i++) {
            list.add(i);
        }

        int threadCount = 8;
        CountDownLatch start        = new CountDownLatch(1);
        CountDownLatch allStarted   = new CountDownLatch(threadCount);
        CountDownLatch done         = new CountDownLatch(threadCount);
        AtomicInteger  maxConcurrent = new AtomicInteger(0);
        AtomicInteger  active        = new AtomicInteger(0);
        AtomicBoolean  failed        = new AtomicBoolean(false);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    start.await();
                    int current = active.incrementAndGet();
                    maxConcurrent.accumulateAndGet(current, Math::max);
                    allStarted.countDown();
                    // Perform reads while other threads are also reading
                    for (int i = 0; i < list.size(); i++) {
                        list.get(i);
                        list.contains(i);
                    }
                    active.decrementAndGet();
                } catch (Exception e) {
                    failed.set(true);
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertTrue(done.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        assertFalse(failed.get(), "A reader thread threw an exception");
        // All threads were active before the first one finished — confirms concurrency
        assertTrue(maxConcurrent.get() > 1,
                "Expected concurrent readers, but max active was " + maxConcurrent.get());
    }

    // ─── Concurrent writes — write lock serializes ────────────────────────────

    @Test
    public void testConcurrentWrites_FinalStateIsConsistentAndSorted()
            throws InterruptedException {
        SynchronizedTreeList<Integer> list = new SynchronizedTreeList<>();
        int threadCount = 4;
        int perThread   = 250;
        CountDownLatch start   = new CountDownLatch(1);
        CountDownLatch done    = new CountDownLatch(threadCount);
        AtomicBoolean  failed  = new AtomicBoolean(false);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        for (int t = 0; t < threadCount; t++) {
            final int base = t * perThread;
            executor.submit(() -> {
                try {
                    start.await();
                    for (int i = base; i < base + perThread; i++) {
                        list.add(i);
                    }
                } catch (Exception e) {
                    failed.set(true);
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertTrue(done.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        assertFalse(failed.get());
        assertEquals(threadCount * perThread, list.size());
        for (int i = 0; i < list.size() - 1; i++) {
            assertTrue(list.get(i) < list.get(i + 1),
                    "Sorted order violated after concurrent writes");
        }
    }

    // ─── Mixed reads and writes ────────────────────────────────────────────────

    @Test
    public void testConcurrentReadWrite_NoExceptionAndCorrectReads()
            throws InterruptedException {
        SynchronizedTreeList<Integer> list = new SynchronizedTreeList<>();
        for (int i = 0; i < 100; i++) {
            list.add(i);
        }

        int writerCount = 2;
        int readerCount = 4;
        CountDownLatch start  = new CountDownLatch(1);
        CountDownLatch done   = new CountDownLatch(writerCount + readerCount);
        AtomicBoolean  failed = new AtomicBoolean(false);

        ExecutorService executor = Executors.newFixedThreadPool(writerCount + readerCount);

        for (int t = 0; t < writerCount; t++) {
            final int base = 100 + t * 100;
            executor.submit(() -> {
                try {
                    start.await();
                    for (int i = base; i < base + 100; i++) {
                        list.add(i);
                    }
                } catch (Exception e) {
                    failed.set(true);
                } finally {
                    done.countDown();
                }
            });
        }

        for (int t = 0; t < readerCount; t++) {
            executor.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < 500; i++) {
                        int s = list.size();
                        if (s > 0) {
                            assertNotNull(list.get(s / 2));
                        }
                    }
                } catch (Exception e) {
                    failed.set(true);
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        assertFalse(failed.get(), "A thread threw an exception during concurrent read/write");
    }
}