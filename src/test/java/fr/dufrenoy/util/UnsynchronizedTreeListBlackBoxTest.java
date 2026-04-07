/*
 * UnsynchronizedTreeListBlackBoxTest.java
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
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Black-box tests for {@link UnsynchronizedTreeList}. Tests the full
 * {@link TreeList} contract against the real implementation, without
 * knowledge of its internal structure.
 */
public class UnsynchronizedTreeListBlackBoxTest {

    // ─── Constructors ─────────────────────────────────────────────────────────

    @Test
    public void testDefaultConstructor_EmptyList() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    public void testDefaultConstructor_NaturalOrdering() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        assertTrue(list.comparator().isEmpty());
    }

    @Test
    public void testConstructor_WithComparator_IsPresent() {
        Comparator<Integer> cmp = Comparator.reverseOrder();
        TreeList<Integer> list = new UnsynchronizedTreeList<>(cmp);
        assertTrue(list.comparator().isPresent());
        assertEquals(cmp, list.comparator().get());
    }

    @Test
    public void testConstructor_WithCollection_ContainsAllElements() {
        List<Integer> source = List.of(3, 1, 4, 1, 5, 9, 2, 6);
        TreeList<Integer> list = new UnsynchronizedTreeList<>(source);
        assertTrue(list.contains(1));
        assertTrue(list.contains(3));
        assertTrue(list.contains(9));
    }

    @Test
    public void testConstructor_WithCollection_DuplicatesDiscarded() {
        List<Integer> source = List.of(1, 2, 2, 3, 3, 3);
        TreeList<Integer> list = new UnsynchronizedTreeList<>(source);
        assertEquals(3, list.size());
    }

    @Test
    public void testConstructor_WithCollection_IsSorted() {
        List<Integer> source = List.of(5, 3, 1, 4, 2);
        TreeList<Integer> list = new UnsynchronizedTreeList<>(source);
        for (int i = 0; i < list.size() - 1; i++) {
            assertTrue(list.get(i) < list.get(i + 1));
        }
    }

    @Test
    public void testConstructor_WithNullCollection_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new UnsynchronizedTreeList<>((List<Integer>) null));
    }

    @Test
    public void testConstructor_WithCollectionContainingNull_ThrowsNullPointerException() {
        List<Integer> source = Arrays.asList(1, null, 3);
        assertThrows(NullPointerException.class, () -> new UnsynchronizedTreeList<>(source));
    }

    @Test
    public void testConstructor_WithComparatorAndCollection_UseComparator() {
        List<Integer> source = List.of(1, 2, 3);
        TreeList<Integer> list = new UnsynchronizedTreeList<>(Comparator.reverseOrder(), source);
        assertEquals(3, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(1, list.get(2));
    }

    // ─── add(E) ───────────────────────────────────────────────────────────────

    @Test
    public void testAdd_NewElement_ReturnsTrue() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        assertTrue(list.add(1));
    }

    @Test
    public void testAdd_Duplicate_ReturnsFalse() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1);
        assertFalse(list.add(1));
    }

    @Test
    public void testAdd_Duplicate_SizeUnchanged() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1);
        list.add(1);
        assertEquals(1, list.size());
    }

    @Test
    public void testAdd_Null_ThrowsNullPointerException() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        assertThrows(NullPointerException.class, () -> list.add(null));
    }

    @Test
    public void testAdd_IncomparableElement_ThrowsClassCastException() {
        TreeList list = new UnsynchronizedTreeList();
        list.add("string");
        assertThrows(ClassCastException.class, () -> list.add(42));
    }

    @Test
    public void testAdd_MaintainsSortedOrder() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(3);
        list.add(1);
        list.add(2);
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
    }

    @Test
    public void testAdd_ManyElements_StaysSorted() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        int[] values = {50, 20, 80, 10, 30, 70, 90, 5, 15, 25, 35};
        for (int v : values) {
            list.add(v);
        }
        for (int i = 0; i < list.size() - 1; i++) {
            assertTrue(list.get(i) < list.get(i + 1));
        }
    }

    // ─── get(int) ─────────────────────────────────────────────────────────────

    @Test
    public void testGet_FirstElement() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(3);
        list.add(1);
        list.add(2);
        assertEquals(1, list.get(0));
    }

    @Test
    public void testGet_LastElement() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(3);
        list.add(1);
        list.add(2);
        assertEquals(3, list.get(list.size() - 1));
    }

    @Test
    public void testGet_NegativeIndex_ThrowsIndexOutOfBounds() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1);
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
    }

    @Test
    public void testGet_IndexEqualToSize_ThrowsIndexOutOfBounds() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1);
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(1));
    }

    @Test
    public void testGet_EmptyList_ThrowsIndexOutOfBounds() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(0));
    }

    // ─── contains / indexOf / lastIndexOf ─────────────────────────────────────

    @Test
    public void testContains_PresentElement_ReturnsTrue() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(42);
        assertTrue(list.contains(42));
    }

    @Test
    public void testContains_AbsentElement_ReturnsFalse() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        assertFalse(list.contains(99));
    }

    @Test
    public void testContains_AfterRemoval_ReturnsFalse() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1);
        list.remove(Integer.valueOf(1));
        assertFalse(list.contains(1));
    }

    @Test
    public void testIndexOf_PresentElement_ReturnsCorrectIndex() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(3);
        list.add(1);
        list.add(2);
        assertEquals(0, list.indexOf(1));
        assertEquals(1, list.indexOf(2));
        assertEquals(2, list.indexOf(3));
    }

    @Test
    public void testIndexOf_AbsentElement_ReturnsMinusOne() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1);
        assertEquals(-1, list.indexOf(99));
    }

    @Test
    public void testLastIndexOf_EqualToIndexOf() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        assertEquals(list.indexOf(2), list.lastIndexOf(2));
    }

    // ─── remove(int) / remove(Object) ─────────────────────────────────────────

    @Test
    public void testRemoveByIndex_ReturnsRemovedElement() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1);
        list.add(2);
        assertEquals(1, list.remove(0));
    }

    @Test
    public void testRemoveByIndex_DecreasesSize() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1);
        list.add(2);
        list.remove(0);
        assertEquals(1, list.size());
    }

    @Test
    public void testRemoveByIndex_MaintainsSortedOrder() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        list.remove(1);
        assertEquals(1, list.get(0));
        assertEquals(3, list.get(1));
    }

    @Test
    public void testRemoveByIndex_NegativeIndex_ThrowsIndexOutOfBounds() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1);
        assertThrows(IndexOutOfBoundsException.class, () -> list.remove(-1));
    }

    @Test
    public void testRemoveByIndex_IndexEqualToSize_ThrowsIndexOutOfBounds() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1);
        assertThrows(IndexOutOfBoundsException.class, () -> list.remove(1));
    }

    @Test
    public void testRemoveByObject_PresentElement_ReturnsTrue() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1);
        assertTrue(list.remove(Integer.valueOf(1)));
    }

    @Test
    public void testRemoveByObject_AbsentElement_ReturnsFalse() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        assertFalse(list.remove(Integer.valueOf(99)));
    }

    @Test
    public void testRemoveByObject_ElementIsGone() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1);
        list.remove(Integer.valueOf(1));
        assertFalse(list.contains(1));
        assertEquals(0, list.size());
    }

    // ─── removeAll / retainAll ────────────────────────────────────────────────

    @Test
    public void testRemoveAll_RemovesAllMatchingElements() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(4);
        list.removeAll(List.of(2, 4));
        assertFalse(list.contains(2));
        assertFalse(list.contains(4));
        assertEquals(2, list.size());
    }

    @Test
    public void testRemoveAll_NoMatch_ReturnsFalse() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1);
        list.add(2);
        assertFalse(list.removeAll(List.of(99, 100)));
    }

    @Test
    public void testRemoveAll_WithMatch_ReturnsTrue() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1);
        list.add(2);
        assertTrue(list.removeAll(List.of(1, 99)));
    }

    @Test
    public void testRemoveAll_MaintainsSortedOrder() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(4);
        list.add(5);
        list.removeAll(List.of(2, 4));
        for (int i = 0; i < list.size() - 1; i++) {
            assertTrue(list.get(i) < list.get(i + 1));
        }
    }

    @Test
    public void testRetainAll_KeepsOnlyMatchingElements() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(4);
        list.retainAll(List.of(2, 4));
        assertTrue(list.contains(2));
        assertTrue(list.contains(4));
        assertFalse(list.contains(1));
        assertFalse(list.contains(3));
        assertEquals(2, list.size());
    }

    @Test
    public void testRetainAll_NoChange_ReturnsFalse() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1);
        list.add(2);
        assertFalse(list.retainAll(List.of(1, 2, 3)));
    }

    @Test
    public void testRemoveAll_Null_ThrowsNullPointerException() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        assertThrows(NullPointerException.class, () -> list.removeAll(null));
    }

    @Test
    public void testRetainAll_Null_ThrowsNullPointerException() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        assertThrows(NullPointerException.class, () -> list.retainAll(null));
    }

    // ─── addAll ───────────────────────────────────────────────────────────────

    @Test
    public void testAddAll_AddsAllElements() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.addAll(List.of(3, 1, 2));
        assertEquals(3, list.size());
        assertTrue(list.contains(1));
        assertTrue(list.contains(2));
        assertTrue(list.contains(3));
    }

    @Test
    public void testAddAll_DuplicatesDiscarded() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1);
        list.addAll(List.of(1, 2, 3));
        assertEquals(3, list.size());
    }

    @Test
    public void testAddAll_MaintainsSortedOrder() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.addAll(List.of(5, 3, 1, 4, 2));
        for (int i = 0; i < list.size() - 1; i++) {
            assertTrue(list.get(i) < list.get(i + 1));
        }
    }

    @Test
    public void testAddAll_Null_ThrowsNullPointerException() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        assertThrows(NullPointerException.class, () -> list.addAll(null));
    }

    // ─── containsAll ─────────────────────────────────────────────────────────

    @Test
    public void testContainsAll_AllPresent_ReturnsTrue() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1); list.add(2); list.add(3);
        assertTrue(list.containsAll(List.of(1, 2)));
    }

    @Test
    public void testContainsAll_SomeAbsent_ReturnsFalse() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1); list.add(2);
        assertFalse(list.containsAll(List.of(1, 99)));
    }

    // ─── clear ────────────────────────────────────────────────────────────────

    @Test
    public void testClear_EmptiesList() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1);
        list.add(2);
        list.clear();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    public void testClear_CanAddAfterClear() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1);
        list.clear();
        list.add(2);
        assertEquals(1, list.size());
        assertTrue(list.contains(2));
    }

    // ─── Unsupported operations ───────────────────────────────────────────────

    @Test
    public void testAdd_ByIndex_ThrowsUnsupportedOperationException() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1);
        assertThrows(UnsupportedOperationException.class, () -> list.add(0, 99));
    }

    @Test
    public void testSet_ThrowsUnsupportedOperationException() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1);
        assertThrows(UnsupportedOperationException.class, () -> list.set(0, 99));
    }

    // ─── subList — basic ────────────────────────────────────────────────────

    @Test
    public void testSubList_ReturnsCorrectElements() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        for (int i = 1; i <= 10; i++) { list.add(i); }
        TreeList<Integer> sub = list.subList(2, 7);
        assertEquals(List.of(3, 4, 5, 6, 7), sub);
    }

    @Test
    public void testSubList_EmptyRange() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1); list.add(2); list.add(3);
        TreeList<Integer> sub = list.subList(1, 1);
        assertTrue(sub.isEmpty());
        assertEquals(0, sub.size());
    }

    @Test
    public void testSubList_FullRange() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(3); list.add(1); list.add(2);
        TreeList<Integer> sub = list.subList(0, 3);
        assertEquals(List.of(1, 2, 3), sub);
    }

    @Test
    public void testSubList_SingleElement() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(10); list.add(20); list.add(30);
        TreeList<Integer> sub = list.subList(1, 2);
        assertEquals(1, sub.size());
        assertEquals(20, sub.get(0));
    }

    @Test
    public void testSubList_InvalidRange_ThrowsExceptions() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1); list.add(2);
        assertThrows(IndexOutOfBoundsException.class, () -> list.subList(-1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.subList(0, 3));
        assertThrows(IllegalArgumentException.class, () -> list.subList(2, 1));
    }

    @Test
    public void testSubList_InheritsComparator() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>(Comparator.reverseOrder());
        list.add(1); list.add(2); list.add(3);
        TreeList<Integer> sub = list.subList(0, 2);
        assertTrue(sub.comparator().isPresent());
    }

    // ─── subList — live view (mutations through subList affect parent) ────

    @Test
    public void testSubList_RemoveByIndex_AffectsParent() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        for (int i = 1; i <= 5; i++) { list.add(i); }
        TreeList<Integer> sub = list.subList(1, 4); // [2, 3, 4]

        sub.remove(1); // removes 3
        assertEquals(List.of(2, 4), sub);
        assertEquals(List.of(1, 2, 4, 5), list);
    }

    @Test
    public void testSubList_RemoveByObject_AffectsParent() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        for (int i = 1; i <= 5; i++) { list.add(i); }
        TreeList<Integer> sub = list.subList(1, 4); // [2, 3, 4]

        assertTrue(sub.remove(Integer.valueOf(3)));
        assertFalse(sub.contains(3));
        assertFalse(list.contains(3));
    }

    @Test
    public void testSubList_RemoveByObject_OutOfRange_ReturnsFalse() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        for (int i = 1; i <= 5; i++) { list.add(i); }
        TreeList<Integer> sub = list.subList(1, 4); // [2, 3, 4]

        assertFalse(sub.remove(Integer.valueOf(1)));
        assertFalse(sub.remove(Integer.valueOf(5)));
        assertTrue(list.contains(1));
        assertTrue(list.contains(5));
    }

    @Test
    public void testSubList_Clear_RemovesOnlyViewElements() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        for (int i = 1; i <= 5; i++) { list.add(i); }
        TreeList<Integer> sub = list.subList(1, 4); // [2, 3, 4]

        sub.clear();
        assertTrue(sub.isEmpty());
        assertEquals(List.of(1, 5), list);
    }

    @Test
    public void testSubList_Add_InRange_AffectsParent() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(10); list.add(20); list.add(40); list.add(50);
        TreeList<Integer> sub = list.subList(1, 3); // [20, 40]

        assertTrue(sub.add(30)); // 30 falls in [20, 40)
        assertTrue(sub.contains(30));
        assertTrue(list.contains(30));
    }

    @Test
    public void testSubList_Add_OutOfRange_ThrowsIllegalArgument() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(10); list.add(20); list.add(30); list.add(40);
        TreeList<Integer> sub = list.subList(1, 3); // [20, 30]

        // Below range
        assertThrows(IllegalArgumentException.class, () -> sub.add(5));
        // At upper bound (exclusive) — out of range
        assertThrows(IllegalArgumentException.class, () -> sub.add(40));
        // Above range
        assertThrows(IllegalArgumentException.class, () -> sub.add(50));
    }

    @Test
    public void testSubList_Add_Duplicate_ReturnsFalse() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(10); list.add(20); list.add(30);
        TreeList<Integer> sub = list.subList(0, 2); // [10, 20]

        assertFalse(sub.add(10));
        assertEquals(3, list.size());
    }

    // ─── subList — live view (mutations to parent invalidate subList) ─────

    @Test
    public void testSubList_ParentAdd_InvalidatesSubList() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1); list.add(2); list.add(3);
        TreeList<Integer> sub = list.subList(0, 2);

        list.add(4); // structural modification outside subList
        assertThrows(ConcurrentModificationException.class, () -> sub.size());
    }

    @Test
    public void testSubList_ParentRemove_InvalidatesSubList() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1); list.add(2); list.add(3);
        TreeList<Integer> sub = list.subList(0, 2);

        list.remove(Integer.valueOf(3)); // structural modification outside subList
        assertThrows(ConcurrentModificationException.class, () -> sub.get(0));
    }

    @Test
    public void testSubList_ParentClear_InvalidatesSubList() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1); list.add(2); list.add(3);
        TreeList<Integer> sub = list.subList(0, 2);

        list.clear();
        assertThrows(ConcurrentModificationException.class, () -> sub.size());
    }

    // ─── subList — queries ───────────────────────────────────────────────

    @Test
    public void testSubList_Contains() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        for (int i = 1; i <= 5; i++) { list.add(i); }
        TreeList<Integer> sub = list.subList(1, 4); // [2, 3, 4]

        assertTrue(sub.contains(2));
        assertTrue(sub.contains(3));
        assertTrue(sub.contains(4));
        assertFalse(sub.contains(1));
        assertFalse(sub.contains(5));
    }

    @Test
    public void testSubList_IndexOf() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        for (int i = 1; i <= 5; i++) { list.add(i); }
        TreeList<Integer> sub = list.subList(1, 4); // [2, 3, 4]

        assertEquals(0, sub.indexOf(2));
        assertEquals(1, sub.indexOf(3));
        assertEquals(2, sub.indexOf(4));
        assertEquals(-1, sub.indexOf(1));
        assertEquals(-1, sub.indexOf(5));
    }

    @Test
    public void testSubList_LastIndexOf_SameAsIndexOf() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        for (int i = 1; i <= 5; i++) { list.add(i); }
        TreeList<Integer> sub = list.subList(1, 4);

        assertEquals(sub.indexOf(3), sub.lastIndexOf(3));
        assertEquals(sub.indexOf(1), sub.lastIndexOf(1));
    }

    // ─── subList — iterator ──────────────────────────────────────────────

    @Test
    public void testSubList_Iterator_TraversesInOrder() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        for (int i = 1; i <= 10; i++) { list.add(i); }
        TreeList<Integer> sub = list.subList(3, 7); // [4, 5, 6, 7]

        List<Integer> collected = new ArrayList<>();
        for (Integer e : sub) { collected.add(e); }
        assertEquals(List.of(4, 5, 6, 7), collected);
    }

    @Test
    public void testSubList_ListIterator_Bidirectional() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        for (int i = 1; i <= 5; i++) { list.add(i); }
        TreeList<Integer> sub = list.subList(1, 4); // [2, 3, 4]

        ListIterator<Integer> it = sub.listIterator(sub.size());
        List<Integer> reversed = new ArrayList<>();
        while (it.hasPrevious()) { reversed.add(it.previous()); }
        assertEquals(List.of(4, 3, 2), reversed);
    }

    @Test
    public void testSubList_ListIterator_IndexMethods() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        for (int i = 1; i <= 5; i++) { list.add(i); }
        TreeList<Integer> sub = list.subList(1, 4); // [2, 3, 4]

        ListIterator<Integer> it = sub.listIterator(1);
        assertEquals(1, it.nextIndex());
        assertEquals(0, it.previousIndex());
    }

    // ─── subList — unsupported operations ────────────────────────────────

    @Test
    public void testSubList_AddPositional_ThrowsUOE() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1); list.add(2); list.add(3);
        TreeList<Integer> sub = list.subList(0, 2);
        assertThrows(UnsupportedOperationException.class, () -> sub.add(0, 99));
    }

    @Test
    public void testSubList_Set_ThrowsUOE() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1); list.add(2); list.add(3);
        TreeList<Integer> sub = list.subList(0, 2);
        assertThrows(UnsupportedOperationException.class, () -> sub.set(0, 99));
    }

    @Test
    public void testSubList_AddAllPositional_ThrowsUOE() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1); list.add(2); list.add(3);
        TreeList<Integer> sub = list.subList(0, 2);
        assertThrows(UnsupportedOperationException.class,
                () -> sub.addAll(0, List.of(99)));
    }

    // ─── subList — nested ────────────────────────────────────────────────

    @Test
    public void testSubList_Nested() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        for (int i = 1; i <= 10; i++) { list.add(i); }
        TreeList<Integer> sub1 = list.subList(2, 8); // [3,4,5,6,7,8]
        TreeList<Integer> sub2 = sub1.subList(1, 4); // [4,5,6]
        assertEquals(List.of(4, 5, 6), sub2);
    }

    @Test
    public void testSubList_Nested_RemoveAffectsAll() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        for (int i = 1; i <= 10; i++) { list.add(i); }
        TreeList<Integer> sub1 = list.subList(2, 8); // [3,4,5,6,7,8]
        TreeList<Integer> sub2 = sub1.subList(1, 4); // [4,5,6]

        sub2.remove(Integer.valueOf(5));
        assertFalse(list.contains(5));
    }

    // ─── subList — removeAll / retainAll ─────────────────────────────────

    @Test
    public void testSubList_RemoveAll() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        for (int i = 1; i <= 5; i++) { list.add(i); }
        TreeList<Integer> sub = list.subList(1, 4); // [2, 3, 4]

        assertTrue(sub.removeAll(List.of(2, 4, 99)));
        assertEquals(List.of(3), sub);
        assertEquals(List.of(1, 3, 5), list);
    }

    @Test
    public void testSubList_RetainAll() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        for (int i = 1; i <= 5; i++) { list.add(i); }
        TreeList<Integer> sub = list.subList(1, 4); // [2, 3, 4]

        assertTrue(sub.retainAll(List.of(3)));
        assertEquals(List.of(3), sub);
        assertEquals(List.of(1, 3, 5), list);
    }

    // ─── subList — equals / hashCode ─────────────────────────────────────

    @Test
    public void testSubList_Equals_RegularList() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        for (int i = 1; i <= 5; i++) { list.add(i); }
        TreeList<Integer> sub = list.subList(1, 4); // [2, 3, 4]

        assertEquals(List.of(2, 3, 4), sub);
        assertEquals(sub, List.of(2, 3, 4));
    }

    // ─── subList — fence element removed from parent ─────────────────────

    @Test
    public void testSubList_FenceElementRemoved_ViewStillWorks() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        for (int i = 1; i <= 5; i++) { list.add(i); }
        TreeList<Integer> sub = list.subList(1, 4); // [2, 3, 4], bounded by values 2 and 4+1=5

        // Remove through the subList — the subList stays valid
        sub.remove(Integer.valueOf(2)); // remove the lower fence element
        assertEquals(List.of(3, 4), sub);

        sub.remove(Integer.valueOf(4)); // remove the upper fence-1 element
        assertEquals(List.of(3), sub);
    }

    @Test
    public void testListIterator_Add_ThrowsUnsupportedOperationException() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1);
        ListIterator<Integer> it = list.listIterator();
        assertThrows(UnsupportedOperationException.class, () -> it.add(99));
    }

    @Test
    public void testListIterator_Set_ThrowsUnsupportedOperationException() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1);
        ListIterator<Integer> it = list.listIterator();
        it.next();
        assertThrows(UnsupportedOperationException.class, () -> it.set(99));
    }

    // ─── iterator ─────────────────────────────────────────────────────────────

    @Test
    public void testIterator_TraversesInSortedOrder() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(3);
        list.add(1);
        list.add(2);
        Iterator<Integer> it = list.iterator();
        assertEquals(1, it.next());
        assertEquals(2, it.next());
        assertEquals(3, it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void testIterator_EmptyList_HasNoNext() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        assertFalse(list.iterator().hasNext());
    }

    @Test
    public void testIterator_Remove_ThrowsUnsupportedOperationException() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1);
        Iterator<Integer> it = list.iterator();
        it.next();
        assertThrows(UnsupportedOperationException.class, it::remove);
    }

    // ─── listIterator ─────────────────────────────────────────────────────────

    @Test
    public void testListIterator_TraversesInSortedOrder() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(3);
        list.add(1);
        list.add(2);
        ListIterator<Integer> it = list.listIterator();
        assertEquals(1, it.next());
        assertEquals(2, it.next());
        assertEquals(3, it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void testListIterator_FromIndex_StartsAtCorrectPosition() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        ListIterator<Integer> it = list.listIterator(1);
        assertEquals(2, it.next());
    }

    @Test
    public void testListIterator_Previous_TraversesBackwards() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        ListIterator<Integer> it = list.listIterator(list.size());
        assertEquals(3, it.previous());
        assertEquals(2, it.previous());
        assertEquals(1, it.previous());
        assertFalse(it.hasPrevious());
    }

    @Test
    public void testListIterator_NegativeIndex_ThrowsIndexOutOfBounds() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1);
        assertThrows(IndexOutOfBoundsException.class, () -> list.listIterator(-1));
    }

    @Test
    public void testListIterator_IndexGreaterThanSize_ThrowsIndexOutOfBounds() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1);
        assertThrows(IndexOutOfBoundsException.class, () -> list.listIterator(2));
    }

    // ─── Custom comparator ────────────────────────────────────────────────────

    @Test
    public void testCustomComparator_ReverseOrder() {
        TreeList<Integer> list = new UnsynchronizedTreeList<>(Comparator.reverseOrder());
        list.add(1);
        list.add(3);
        list.add(2);
        assertEquals(3, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(1, list.get(2));
    }

    // ─── equals / hashCode ────────────────────────────────────────────────────

    @Test
    public void testEquals_TwoListsWithSameElements_AreEqual() {
        TreeList<Integer> list1 = new UnsynchronizedTreeList<>();
        list1.add(1);
        list1.add(2);
        list1.add(3);

        TreeList<Integer> list2 = new UnsynchronizedTreeList<>();
        list2.add(3);
        list2.add(1);
        list2.add(2);

        assertEquals(list1, list2);
    }

    @Test
    public void testEquals_WithArrayList_SameOrder_AreEqual() {
        TreeList<Integer> treeList = new UnsynchronizedTreeList<>();
        treeList.add(1);
        treeList.add(2);
        treeList.add(3);

        List<Integer> arrayList = new ArrayList<>();
        arrayList.add(1);
        arrayList.add(2);
        arrayList.add(3);

        assertEquals(treeList, arrayList);
    }

    @Test
    public void testHashCode_EqualLists_SameHashCode() {
        TreeList<Integer> list1 = new UnsynchronizedTreeList<>();
        list1.add(1);
        list1.add(2);

        TreeList<Integer> list2 = new UnsynchronizedTreeList<>();
        list2.add(2);
        list2.add(1);

        assertEquals(list1.hashCode(), list2.hashCode());
    }
}