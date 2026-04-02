/*
 * TreeListTest.java
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
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Interface contract tests for {@link TreeList}, using a {@link MockTreeList}
 * implementation. Tests the contract in isolation, without knowledge of any
 * real implementation.
 */
public class TreeListTest {

    // ─── comparator ───────────────────────────────────────────────────────────

    @Test
    public void testComparator_NaturalOrdering_IsEmpty() {
        TreeList<Integer> list = new MockTreeList<>();
        assertTrue(list.comparator().isEmpty());
    }

    @Test
    public void testComparator_WithComparator_IsPresent() {
        Comparator<Integer> cmp = Comparator.reverseOrder();
        TreeList<Integer> list = new MockTreeList<>(cmp);
        assertTrue(list.comparator().isPresent());
        assertEquals(cmp, list.comparator().get());
    }

    // ─── size / isEmpty ───────────────────────────────────────────────────────

    @Test
    public void testSize_EmptyList_IsZero() {
        TreeList<Integer> list = new MockTreeList<>();
        assertEquals(0, list.size());
    }

    @Test
    public void testSize_AfterAdds_Increases() {
        TreeList<Integer> list = new MockTreeList<>();
        list.add(3);
        list.add(1);
        list.add(2);
        assertEquals(3, list.size());
    }

    @Test
    public void testIsEmpty_EmptyList_ReturnsTrue() {
        TreeList<Integer> list = new MockTreeList<>();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testIsEmpty_NonEmptyList_ReturnsFalse() {
        TreeList<Integer> list = new MockTreeList<>();
        list.add(1);
        assertFalse(list.isEmpty());
    }

    // ─── add(E) ───────────────────────────────────────────────────────────────

    @Test
    public void testAdd_NewElement_ReturnsTrue() {
        TreeList<Integer> list = new MockTreeList<>();
        assertTrue(list.add(1));
    }

    @Test
    public void testAdd_Duplicate_ReturnsFalse() {
        TreeList<Integer> list = new MockTreeList<>();
        list.add(1);
        assertFalse(list.add(1));
    }

    @Test
    public void testAdd_Duplicate_SizeUnchanged() {
        TreeList<Integer> list = new MockTreeList<>();
        list.add(1);
        list.add(1);
        assertEquals(1, list.size());
    }

    @Test
    public void testAdd_Null_ThrowsNullPointerException() {
        TreeList<Integer> list = new MockTreeList<>();
        assertThrows(NullPointerException.class, () -> list.add(null));
    }

    @Test
    public void testAdd_MaintainsSortedOrder() {
        TreeList<Integer> list = new MockTreeList<>();
        list.add(3);
        list.add(1);
        list.add(2);
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
    }

    // ─── get(int) ─────────────────────────────────────────────────────────────

    @Test
    public void testGet_FirstElement() {
        TreeList<Integer> list = new MockTreeList<>();
        list.add(3);
        list.add(1);
        list.add(2);
        assertEquals(1, list.get(0));
    }

    @Test
    public void testGet_LastElement() {
        TreeList<Integer> list = new MockTreeList<>();
        list.add(3);
        list.add(1);
        list.add(2);
        assertEquals(3, list.get(2));
    }

    @Test
    public void testGet_NegativeIndex_ThrowsIndexOutOfBounds() {
        TreeList<Integer> list = new MockTreeList<>();
        list.add(1);
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
    }

    @Test
    public void testGet_IndexEqualToSize_ThrowsIndexOutOfBounds() {
        TreeList<Integer> list = new MockTreeList<>();
        list.add(1);
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(1));
    }

    @Test
    public void testGet_EmptyList_ThrowsIndexOutOfBounds() {
        TreeList<Integer> list = new MockTreeList<>();
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(0));
    }

    // ─── contains / indexOf / lastIndexOf ─────────────────────────────────────

    @Test
    public void testContains_PresentElement_ReturnsTrue() {
        TreeList<Integer> list = new MockTreeList<>();
        list.add(1);
        assertTrue(list.contains(1));
    }

    @Test
    public void testContains_AbsentElement_ReturnsFalse() {
        TreeList<Integer> list = new MockTreeList<>();
        assertFalse(list.contains(99));
    }

    @Test
    public void testIndexOf_PresentElement_ReturnsCorrectIndex() {
        TreeList<Integer> list = new MockTreeList<>();
        list.add(3);
        list.add(1);
        list.add(2);
        assertEquals(0, list.indexOf(1));
        assertEquals(1, list.indexOf(2));
        assertEquals(2, list.indexOf(3));
    }

    @Test
    public void testIndexOf_AbsentElement_ReturnsMinusOne() {
        TreeList<Integer> list = new MockTreeList<>();
        list.add(1);
        assertEquals(-1, list.indexOf(99));
    }

    @Test
    public void testLastIndexOf_EqualToIndexOf() {
        TreeList<Integer> list = new MockTreeList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        assertEquals(list.indexOf(2), list.lastIndexOf(2));
    }

    // ─── remove(int) / remove(Object) ─────────────────────────────────────────

    @Test
    public void testRemoveByIndex_ReturnsRemovedElement() {
        TreeList<Integer> list = new MockTreeList<>();
        list.add(1);
        list.add(2);
        assertEquals(1, list.remove(0));
    }

    @Test
    public void testRemoveByIndex_DecreasesSize() {
        TreeList<Integer> list = new MockTreeList<>();
        list.add(1);
        list.add(2);
        list.remove(0);
        assertEquals(1, list.size());
    }

    @Test
    public void testRemoveByIndex_MaintainsSortedOrder() {
        TreeList<Integer> list = new MockTreeList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        list.remove(1);
        assertEquals(1, list.get(0));
        assertEquals(3, list.get(1));
    }

    @Test
    public void testRemoveByIndex_NegativeIndex_ThrowsIndexOutOfBounds() {
        TreeList<Integer> list = new MockTreeList<>();
        list.add(1);
        assertThrows(IndexOutOfBoundsException.class, () -> list.remove(-1));
    }

    @Test
    public void testRemoveByIndex_IndexEqualToSize_ThrowsIndexOutOfBounds() {
        TreeList<Integer> list = new MockTreeList<>();
        list.add(1);
        assertThrows(IndexOutOfBoundsException.class, () -> list.remove(1));
    }

    @Test
    public void testRemoveByObject_PresentElement_ReturnsTrue() {
        TreeList<Integer> list = new MockTreeList<>();
        list.add(1);
        assertTrue(list.remove(Integer.valueOf(1)));
    }

    @Test
    public void testRemoveByObject_AbsentElement_ReturnsFalse() {
        TreeList<Integer> list = new MockTreeList<>();
        assertFalse(list.remove(Integer.valueOf(99)));
    }

    @Test
    public void testRemoveByObject_ElementIsGone() {
        TreeList<Integer> list = new MockTreeList<>();
        list.add(1);
        list.remove(Integer.valueOf(1));
        assertFalse(list.contains(1));
        assertEquals(0, list.size());
    }

    // ─── clear ────────────────────────────────────────────────────────────────

    @Test
    public void testClear_EmptiesList() {
        TreeList<Integer> list = new MockTreeList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        list.clear();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    public void testClear_ElementsNoLongerPresent() {
        TreeList<Integer> list = new MockTreeList<>();
        list.add(1);
        list.clear();
        assertFalse(list.contains(1));
    }

    @Test
    public void testClear_EmptyList_IsNoOp() {
        TreeList<Integer> list = new MockTreeList<>();
        list.clear();
        assertEquals(0, list.size());
    }

    // ─── Unsupported operations ───────────────────────────────────────────────

    @Test
    public void testAdd_ByIndex_ThrowsUnsupportedOperationException() {
        TreeList<Integer> list = new MockTreeList<>();
        list.add(1);
        assertThrows(UnsupportedOperationException.class, () -> list.add(0, 99));
    }

    @Test
    public void testSet_ThrowsUnsupportedOperationException() {
        TreeList<Integer> list = new MockTreeList<>();
        list.add(1);
        assertThrows(UnsupportedOperationException.class, () -> list.set(0, 99));
    }

    @Test
    public void testSubList_ThrowsUnsupportedOperationException() {
        TreeList<Integer> list = new MockTreeList<>();
        list.add(1);
        list.add(2);
        assertThrows(UnsupportedOperationException.class, () -> list.subList(0, 1));
    }

    @Test
    public void testListIterator_Add_ThrowsUnsupportedOperationException() {
        TreeList<Integer> list = new MockTreeList<>();
        list.add(1);
        ListIterator<Integer> it = list.listIterator();
        assertThrows(UnsupportedOperationException.class, () -> it.add(99));
    }

    @Test
    public void testListIterator_Set_ThrowsUnsupportedOperationException() {
        TreeList<Integer> list = new MockTreeList<>();
        list.add(1);
        ListIterator<Integer> it = list.listIterator();
        it.next();
        assertThrows(UnsupportedOperationException.class, () -> it.set(99));
    }

    // ─── iterator ─────────────────────────────────────────────────────────────

    @Test
    public void testIterator_TraversesInSortedOrder() {
        TreeList<Integer> list = new MockTreeList<>();
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
        TreeList<Integer> list = new MockTreeList<>();
        assertFalse(list.iterator().hasNext());
    }

    // ─── Sorted order invariant ───────────────────────────────────────────────

    @Test
    public void testSortedOrder_Maintained_AfterMultipleAdds() {
        TreeList<Integer> list = new MockTreeList<>();
        int[] values = {5, 3, 8, 1, 4, 7, 2, 6};
        for (int v : values) {
            list.add(v);
        }
        for (int i = 0; i < list.size() - 1; i++) {
            assertTrue(list.get(i) < list.get(i + 1));
        }
    }

    @Test
    public void testSortedOrder_Maintained_AfterRemove() {
        TreeList<Integer> list = new MockTreeList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(4);
        list.add(5);
        list.remove(Integer.valueOf(3));
        for (int i = 0; i < list.size() - 1; i++) {
            assertTrue(list.get(i) < list.get(i + 1));
        }
    }

    // ─── Custom comparator ────────────────────────────────────────────────────

    @Test
    public void testCustomComparator_ReverseOrder() {
        TreeList<Integer> list = new MockTreeList<>(Comparator.reverseOrder());
        list.add(1);
        list.add(3);
        list.add(2);
        assertEquals(3, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(1, list.get(2));
    }

    @Test
    public void testCustomComparator_DuplicatesByComparator_Rejected() {
        // Comparator ignores sign — treats 1 and -1 as equal
        TreeList<Integer> list = new MockTreeList<>(Comparator.comparingInt(Math::abs));
        list.add(1);
        assertFalse(list.add(-1));
        assertEquals(1, list.size());
    }

    // ─── equals / hashCode ────────────────────────────────────────────────────

    @Test
    public void testEquals_TwoListsWithSameElements_AreEqual() {
        TreeList<Integer> list1 = new MockTreeList<>();
        list1.add(1);
        list1.add(2);
        list1.add(3);

        TreeList<Integer> list2 = new MockTreeList<>();
        list2.add(3);
        list2.add(1);
        list2.add(2);

        assertEquals(list1, list2);
    }

    @Test
    public void testEquals_WithArrayList_SameOrder_AreEqual() {
        TreeList<Integer> treeList = new MockTreeList<>();
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
        TreeList<Integer> list1 = new MockTreeList<>();
        list1.add(1);
        list1.add(2);

        TreeList<Integer> list2 = new MockTreeList<>();
        list2.add(2);
        list2.add(1);

        assertEquals(list1.hashCode(), list2.hashCode());
    }
}