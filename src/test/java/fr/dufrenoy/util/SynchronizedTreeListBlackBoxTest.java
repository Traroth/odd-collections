/*
 * SynchronizedTreeListBlackBoxTest.java
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
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Black-box tests for {@link SynchronizedTreeList}. Tests the full
 * {@link TreeList} contract and the concurrency guarantees documented
 * in the Javadoc, without knowledge of the internal implementation.
 */
public class SynchronizedTreeListBlackBoxTest {

    // ─── Constructors ─────────────────────────────────────────────────────────

    @Test
    public void testDefaultConstructor_EmptyList() {
        TreeList<Integer> list = new SynchronizedTreeList<>();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    public void testDefaultConstructor_NaturalOrdering() {
        TreeList<Integer> list = new SynchronizedTreeList<>();
        assertFalse(list.comparator().isPresent());
    }

    @Test
    public void testConstructor_WithComparator_IsPresent() {
        Comparator<Integer> cmp = Comparator.reverseOrder();
        TreeList<Integer> list = new SynchronizedTreeList<>(cmp);
        assertTrue(list.comparator().isPresent());
        assertEquals(cmp, list.comparator().get());
    }

    @Test
    public void testConstructor_WithCollection_ContainsAllElements() {
        List<Integer> source = Arrays.asList(3, 1, 4, 1, 5, 9, 2, 6);
        TreeList<Integer> list = new SynchronizedTreeList<>(source);
        assertTrue(list.contains(1));
        assertTrue(list.contains(3));
        assertTrue(list.contains(9));
    }

    @Test
    public void testConstructor_WithCollection_DuplicatesDiscarded() {
        List<Integer> source = Arrays.asList(1, 2, 2, 3, 3, 3);
        TreeList<Integer> list = new SynchronizedTreeList<>(source);
        assertEquals(3, list.size());
    }

    @Test
    public void testConstructor_WithNullCollection_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new SynchronizedTreeList<>((List<Integer>) null));
    }

    @Test
    public void testConstructor_WithCollectionContainingNull_ThrowsNullPointerException() {
        List<Integer> source = Arrays.asList(1, null, 3);
        assertThrows(NullPointerException.class, () -> new SynchronizedTreeList<>(source));
    }

    @Test
    public void testConstructor_WithComparatorAndCollection_UseComparator() {
        List<Integer> source = Arrays.asList(1, 2, 3);
        TreeList<Integer> list = new SynchronizedTreeList<>(Comparator.reverseOrder(), source);
        assertEquals(3, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(1, list.get(2));
    }

    // ─── add(E) ───────────────────────────────────────────────────────────────

    @Test
    public void testAdd_NewElement_ReturnsTrue() {
        TreeList<Integer> list = new SynchronizedTreeList<>();
        assertTrue(list.add(1));
    }

    @Test
    public void testAdd_Duplicate_ReturnsFalse() {
        TreeList<Integer> list = new SynchronizedTreeList<>();
        list.add(1);
        assertFalse(list.add(1));
    }

    @Test
    public void testAdd_Null_ThrowsNullPointerException() {
        TreeList<Integer> list = new SynchronizedTreeList<>();
        assertThrows(NullPointerException.class, () -> list.add(null));
    }

    @Test
    public void testAdd_MaintainsSortedOrder() {
        TreeList<Integer> list = new SynchronizedTreeList<>();
        list.add(3);
        list.add(1);
        list.add(2);
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
    }

    // ─── get / size / isEmpty ─────────────────────────────────────────────────

    @Test
    public void testGet_NegativeIndex_ThrowsIndexOutOfBounds() {
        TreeList<Integer> list = new SynchronizedTreeList<>();
        list.add(1);
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
    }

    @Test
    public void testGet_IndexEqualToSize_ThrowsIndexOutOfBounds() {
        TreeList<Integer> list = new SynchronizedTreeList<>();
        list.add(1);
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(1));
    }

    @Test
    public void testSize_AfterAdds_IsCorrect() {
        TreeList<Integer> list = new SynchronizedTreeList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        assertEquals(3, list.size());
    }

    // ─── contains / indexOf / lastIndexOf ─────────────────────────────────────

    @Test
    public void testContains_PresentElement_ReturnsTrue() {
        TreeList<Integer> list = new SynchronizedTreeList<>();
        list.add(42);
        assertTrue(list.contains(42));
    }

    @Test
    public void testContains_AbsentElement_ReturnsFalse() {
        TreeList<Integer> list = new SynchronizedTreeList<>();
        assertFalse(list.contains(99));
    }

    @Test
    public void testIndexOf_PresentElement_ReturnsCorrectIndex() {
        TreeList<Integer> list = new SynchronizedTreeList<>();
        list.add(3);
        list.add(1);
        list.add(2);
        assertEquals(0, list.indexOf(1));
        assertEquals(1, list.indexOf(2));
        assertEquals(2, list.indexOf(3));
    }

    @Test
    public void testLastIndexOf_EqualToIndexOf() {
        TreeList<Integer> list = new SynchronizedTreeList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        assertEquals(list.indexOf(2), list.lastIndexOf(2));
    }

    // ─── remove ───────────────────────────────────────────────────────────────

    @Test
    public void testRemoveByIndex_ReturnsRemovedElement() {
        TreeList<Integer> list = new SynchronizedTreeList<>();
        list.add(1);
        list.add(2);
        assertEquals(1, list.remove(0));
    }

    @Test
    public void testRemoveByObject_PresentElement_ReturnsTrue() {
        TreeList<Integer> list = new SynchronizedTreeList<>();
        list.add(1);
        assertTrue(list.remove(Integer.valueOf(1)));
    }

    @Test
    public void testRemoveByObject_AbsentElement_ReturnsFalse() {
        TreeList<Integer> list = new SynchronizedTreeList<>();
        assertFalse(list.remove(Integer.valueOf(99)));
    }

    // ─── removeAll / retainAll ────────────────────────────────────────────────

    @Test
    public void testRemoveAll_RemovesAllMatchingElements() {
        TreeList<Integer> list = new SynchronizedTreeList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(4);
        list.removeAll(Arrays.asList(2, 4));
        assertFalse(list.contains(2));
        assertFalse(list.contains(4));
        assertEquals(2, list.size());
    }

    @Test
    public void testRetainAll_KeepsOnlyMatchingElements() {
        TreeList<Integer> list = new SynchronizedTreeList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(4);
        list.retainAll(Arrays.asList(2, 4));
        assertEquals(2, list.size());
        assertTrue(list.contains(2));
        assertTrue(list.contains(4));
    }

    @Test
    public void testAddAll_Null_ThrowsNullPointerException() {
        TreeList<Integer> list = new SynchronizedTreeList<>();
        assertThrows(NullPointerException.class, () -> list.addAll(null));
    }

    @Test
    public void testRemoveAll_Null_ThrowsNullPointerException() {
        TreeList<Integer> list = new SynchronizedTreeList<>();
        assertThrows(NullPointerException.class, () -> list.removeAll(null));
    }

    @Test
    public void testRetainAll_Null_ThrowsNullPointerException() {
        TreeList<Integer> list = new SynchronizedTreeList<>();
        assertThrows(NullPointerException.class, () -> list.retainAll(null));
    }

    // ─── containsAll ─────────────────────────────────────────────────────────

    @Test
    public void testContainsAll_AllPresent_ReturnsTrue() {
        TreeList<Integer> list = new SynchronizedTreeList<>();
        list.add(1); list.add(2); list.add(3);
        assertTrue(list.containsAll(Arrays.asList(1, 2)));
    }

    @Test
    public void testContainsAll_SomeAbsent_ReturnsFalse() {
        TreeList<Integer> list = new SynchronizedTreeList<>();
        list.add(1); list.add(2);
        assertFalse(list.containsAll(Arrays.asList(1, 99)));
    }

    // ─── clear ────────────────────────────────────────────────────────────────

    @Test
    public void testClear_EmptiesList() {
        TreeList<Integer> list = new SynchronizedTreeList<>();
        list.add(1);
        list.add(2);
        list.clear();
        assertTrue(list.isEmpty());
    }

    // ─── Unsupported operations ───────────────────────────────────────────────

    @Test
    public void testAdd_ByIndex_ThrowsUnsupportedOperationException() {
        TreeList<Integer> list = new SynchronizedTreeList<>();
        list.add(1);
        assertThrows(UnsupportedOperationException.class, () -> list.add(0, 99));
    }

    @Test
    public void testSet_ThrowsUnsupportedOperationException() {
        TreeList<Integer> list = new SynchronizedTreeList<>();
        list.add(1);
        assertThrows(UnsupportedOperationException.class, () -> list.set(0, 99));
    }

    @Test
    public void testSubList_ThrowsUnsupportedOperationException() {
        TreeList<Integer> list = new SynchronizedTreeList<>();
        list.add(1);
        list.add(2);
        assertThrows(UnsupportedOperationException.class, () -> list.subList(0, 1));
    }

    @Test
    public void testListIterator_Add_ThrowsUnsupportedOperationException() {
        TreeList<Integer> list = new SynchronizedTreeList<>();
        list.add(1);
        ListIterator<Integer> it = list.listIterator();
        assertThrows(UnsupportedOperationException.class, () -> it.add(99));
    }

    @Test
    public void testListIterator_Set_ThrowsUnsupportedOperationException() {
        TreeList<Integer> list = new SynchronizedTreeList<>();
        list.add(1);
        ListIterator<Integer> it = list.listIterator();
        it.next();
        assertThrows(UnsupportedOperationException.class, () -> it.set(99));
    }

    @Test
    public void testIterator_Remove_ThrowsUnsupportedOperationException() {
        TreeList<Integer> list = new SynchronizedTreeList<>();
        list.add(1);
        Iterator<Integer> it = list.iterator();
        it.next();
        assertThrows(UnsupportedOperationException.class, it::remove);
    }

    // ─── equals / hashCode ────────────────────────────────────────────────────

    @Test
    public void testEquals_TwoListsWithSameElements_AreEqual() {
        TreeList<Integer> list1 = new SynchronizedTreeList<>();
        list1.add(1);
        list1.add(2);

        TreeList<Integer> list2 = new SynchronizedTreeList<>();
        list2.add(2);
        list2.add(1);

        assertEquals(list1, list2);
    }

    @Test
    public void testEquals_WithUnsynchronizedList_SameElements_AreEqual() {
        TreeList<Integer> sync = new SynchronizedTreeList<>();
        sync.add(1);
        sync.add(2);
        sync.add(3);

        TreeList<Integer> unsync = new UnsynchronizedTreeList<>();
        unsync.add(3);
        unsync.add(1);
        unsync.add(2);

        assertEquals(sync, unsync);
    }

    @Test
    public void testEquals_WithArrayList_SameOrder_AreEqual() {
        TreeList<Integer> list = new SynchronizedTreeList<>();
        list.add(1);
        list.add(2);
        list.add(3);

        List<Integer> arrayList = new ArrayList<>();
        arrayList.add(1);
        arrayList.add(2);
        arrayList.add(3);

        assertEquals(list, arrayList);
    }

    @Test
    public void testHashCode_EqualLists_SameHashCode() {
        TreeList<Integer> list1 = new SynchronizedTreeList<>();
        list1.add(1);
        list1.add(2);

        TreeList<Integer> list2 = new SynchronizedTreeList<>();
        list2.add(2);
        list2.add(1);

        assertEquals(list1.hashCode(), list2.hashCode());
    }

    // ─── Concurrency — snapshot iterator ─────────────────────────────────────

    @Test
    public void testIterator_IsSnapshot_ModificationAfterCreationNotVisible()
            throws InterruptedException {
        TreeList<Integer> list = new SynchronizedTreeList<>();
        list.add(1);
        list.add(2);
        list.add(3);

        Iterator<Integer> it = list.iterator();
        list.add(4);

        List<Integer> seen = new ArrayList<>();
        while (it.hasNext()) {
            seen.add(it.next());
        }

        assertFalse(seen.contains(4),
                "Snapshot iterator must not reflect modifications made after its creation");
        assertEquals(3, seen.size());
    }

    @Test
    public void testIterator_IsSnapshot_NeverThrowsConcurrentModificationException()
            throws InterruptedException {
        TreeList<Integer> list = new SynchronizedTreeList<>();
        for (int i = 0; i < 100; i++) {
            list.add(i);
        }

        AtomicBoolean exceptionThrown = new AtomicBoolean(false);

        Iterator<Integer> it = list.iterator();

        Thread writer = new Thread(() -> {
            for (int i = 100; i < 200; i++) {
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

    // ─── Concurrency — concurrent writes ──────────────────────────────────────

    @Test
    public void testConcurrentAdd_AllElementsPresent() throws InterruptedException {
        TreeList<Integer> list = new SynchronizedTreeList<>();
        int threadCount   = 4;
        int perThread     = 250;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);

        for (int t = 0; t < threadCount; t++) {
            final int base = t * perThread;
            executor.submit(() -> {
                try {
                    latch.await();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                for (int i = base; i < base + perThread; i++) {
                    list.add(i);
                }
            });
        }

        latch.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        assertEquals(threadCount * perThread, list.size());
        for (int i = 0; i < list.size() - 1; i++) {
            assertTrue(list.get(i) < list.get(i + 1),
                    "List must remain sorted after concurrent insertions");
        }
    }

    // ─── Concurrency — concurrent reads ───────────────────────────────────────

    @Test
    public void testConcurrentReads_NoException() throws InterruptedException {
        TreeList<Integer> list = new SynchronizedTreeList<>();
        for (int i = 0; i < 100; i++) {
            list.add(i);
        }

        int threadCount = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean exceptionThrown = new AtomicBoolean(false);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    for (int i = 0; i < list.size(); i++) {
                        list.get(i);
                        list.contains(i);
                    }
                } catch (Exception e) {
                    exceptionThrown.set(true);
                }
            });
        }

        latch.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        assertFalse(exceptionThrown.get(), "Concurrent reads must not throw exceptions");
    }
}