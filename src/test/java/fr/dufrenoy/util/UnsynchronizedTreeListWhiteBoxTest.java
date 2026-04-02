/*
 * UnsynchronizedTreeListWhiteBoxTest.java
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
import java.util.Arrays;
import java.util.List;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * White-box tests for {@link UnsynchronizedTreeList}. These tests have
 * knowledge of the internal red-black tree structure and use reflection to
 * verify invariants that are not observable from the public API alone:
 * red-black coloring, equal black-height, and {@code subtreeSize} augmentation.
 *
 * <p>Tests also cover internal behavioural details such as the
 * {@code modCount}-based {@link ConcurrentModificationException}, the
 * two-children case of {@code deleteEntry}, and bidirectional iterator
 * state tracking.
 */
public class UnsynchronizedTreeListWhiteBoxTest {

    // ─── Reflection helpers ───────────────────────────────────────────────────

    private static final Class<?> NODE_CLASS;

    static {
        NODE_CLASS = Arrays.stream(UnsynchronizedTreeList.class.getDeclaredClasses())
                .filter(c -> c.getSimpleName().equals("Node"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Node inner class not found"));
    }

    private static Object nodeField(Object node, String name) throws ReflectiveOperationException {
        Field f = NODE_CLASS.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(node);
    }

    private static Object root(UnsynchronizedTreeList<?> list) throws ReflectiveOperationException {
        Field f = UnsynchronizedTreeList.class.getDeclaredField("root");
        f.setAccessible(true);
        return f.get(list);
    }

    private static boolean isRed(Object node) throws ReflectiveOperationException {
        if (node == null) {
            return false; // null sentinel nodes are BLACK
        }
        return (boolean) nodeField(node, "color");
    }

    private static int subtreeSizeOf(Object node) throws ReflectiveOperationException {
        return (int) nodeField(node, "subtreeSize");
    }

    private static Object elementOf(Object node) throws ReflectiveOperationException {
        return nodeField(node, "element");
    }

    /**
     * Recursively verifies red-black coloring and returns the black-height of
     * the subtree. Fails the test if any red-black property is violated.
     */
    private static int checkBlackHeight(Object node) throws ReflectiveOperationException {
        if (node == null) {
            return 1; // null nodes count as BLACK
        }
        Object left  = nodeField(node, "left");
        Object right = nodeField(node, "right");
        if (isRed(node)) {
            assertFalse(isRed(left),  "RED node has RED left child (element=" + elementOf(node) + ")");
            assertFalse(isRed(right), "RED node has RED right child (element=" + elementOf(node) + ")");
        }
        int leftBH  = checkBlackHeight(left);
        int rightBH = checkBlackHeight(right);
        assertEquals(leftBH, rightBH,
                "Black-height mismatch at node with element=" + elementOf(node));
        return leftBH + (isRed(node) ? 0 : 1);
    }

    /** Recursively verifies that every node's {@code subtreeSize} is correct. */
    private static void checkSubtreeSizes(Object node) throws ReflectiveOperationException {
        if (node == null) {
            return;
        }
        Object left  = nodeField(node, "left");
        Object right = nodeField(node, "right");
        int leftSize  = left  == null ? 0 : subtreeSizeOf(left);
        int rightSize = right == null ? 0 : subtreeSizeOf(right);
        assertEquals(1 + leftSize + rightSize, subtreeSizeOf(node),
                "subtreeSize incorrect at element=" + elementOf(node));
        checkSubtreeSizes(left);
        checkSubtreeSizes(right);
    }

    private void assertTreeInvariants(UnsynchronizedTreeList<?> list)
            throws ReflectiveOperationException {
        Object r = root(list);
        if (r == null) {
            return;
        }
        assertFalse(isRed(r), "Root must be BLACK");
        checkBlackHeight(r);
        checkSubtreeSizes(r);
    }

    // ─── Red-black invariants — after insert ──────────────────────────────────

    @Test
    public void testRedBlack_AfterEachInsert_SequentialValues() throws Exception {
        UnsynchronizedTreeList<Integer> list = new UnsynchronizedTreeList<>();
        for (int v : new int[]{5, 3, 8, 1, 4, 7, 2, 6, 9, 10}) {
            list.add(v);
            assertTreeInvariants(list);
        }
    }

    @Test
    public void testRedBlack_AfterAscendingInserts_TriggersLeftRotations() throws Exception {
        // Ascending input is the worst case for a naive BST — the RB fix-up must
        // rebalance aggressively through left-rotations and colour flips.
        UnsynchronizedTreeList<Integer> list = new UnsynchronizedTreeList<>();
        for (int i = 1; i <= 31; i++) {
            list.add(i);
            assertTreeInvariants(list);
        }
    }

    @Test
    public void testRedBlack_AfterDescendingInserts_TriggersRightRotations() throws Exception {
        UnsynchronizedTreeList<Integer> list = new UnsynchronizedTreeList<>();
        for (int i = 31; i >= 1; i--) {
            list.add(i);
            assertTreeInvariants(list);
        }
    }

    // ─── Red-black invariants — after delete ──────────────────────────────────

    @Test
    public void testRedBlack_AfterRemoveLeaf() throws Exception {
        UnsynchronizedTreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(5); list.add(3); list.add(7);
        list.remove(Integer.valueOf(3));
        assertTreeInvariants(list);
    }

    @Test
    public void testRedBlack_AfterRemoveRoot_TwoChildren() throws Exception {
        UnsynchronizedTreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(5); list.add(3); list.add(7);
        list.remove(Integer.valueOf(5)); // root with two children → copies successor
        assertTreeInvariants(list);
    }

    @Test
    public void testRedBlack_AfterRemoveInternalNode_TwoChildren() throws Exception {
        UnsynchronizedTreeList<Integer> list = new UnsynchronizedTreeList<>();
        for (int v : new int[]{10, 5, 15, 3, 7, 12, 20}) {
            list.add(v);
        }
        list.remove(Integer.valueOf(5)); // internal node with children 3 and 7
        assertTreeInvariants(list);
    }

    @Test
    public void testRedBlack_AfterEachRemoval_AlternatingElements() throws Exception {
        UnsynchronizedTreeList<Integer> list = new UnsynchronizedTreeList<>();
        for (int i = 1; i <= 15; i++) {
            list.add(i);
        }
        for (int i = 1; i <= 15; i += 2) {
            list.remove(Integer.valueOf(i));
            assertTreeInvariants(list);
        }
    }

    @Test
    public void testRedBlack_AfterRemoveAll_EmptyTree() throws Exception {
        UnsynchronizedTreeList<Integer> list = new UnsynchronizedTreeList<>();
        for (int i = 1; i <= 7; i++) {
            list.add(i);
        }
        for (int i = 1; i <= 7; i++) {
            list.remove(Integer.valueOf(i));
            assertTreeInvariants(list);
        }
        assertEquals(0, list.size());
    }

    // ─── subtreeSize augmentation ─────────────────────────────────────────────

    @Test
    public void testSubtreeSize_AfterEachInsert() throws Exception {
        UnsynchronizedTreeList<Integer> list = new UnsynchronizedTreeList<>();
        for (int v : new int[]{5, 3, 8, 1, 4}) {
            list.add(v);
            checkSubtreeSizes(root(list));
        }
    }

    @Test
    public void testSubtreeSize_AfterEachRemoval() throws Exception {
        UnsynchronizedTreeList<Integer> list = new UnsynchronizedTreeList<>();
        for (int i = 1; i <= 10; i++) {
            list.add(i);
        }
        for (int i = 1; i <= 10; i += 3) {
            list.remove(Integer.valueOf(i));
            checkSubtreeSizes(root(list));
        }
    }

    @Test
    public void testSubtreeSize_RootEqualsListSize() throws Exception {
        UnsynchronizedTreeList<Integer> list = new UnsynchronizedTreeList<>();
        for (int i = 1; i <= 20; i++) {
            list.add(i);
        }
        Object r = root(list);
        assertEquals(list.size(), subtreeSizeOf(r),
                "Root subtreeSize must equal list size");
    }

    // ─── deleteEntry — two-children case (the stale-next bug) ─────────────────

    @Test
    public void testRemoveAll_TwoChildrenDeletion_PreservesRemainingElements() throws Exception {
        // {1..7}: removing 2, 4, 6 will exercise the two-children path of deleteEntry.
        // The stale-next bug caused traversal to stop after the first such deletion.
        UnsynchronizedTreeList<Integer> list = new UnsynchronizedTreeList<>();
        for (int i = 1; i <= 7; i++) {
            list.add(i);
        }
        list.removeAll(List.of(2, 4, 6));
        assertEquals(4, list.size());
        assertTrue(list.contains(1));
        assertTrue(list.contains(3));
        assertTrue(list.contains(5));
        assertTrue(list.contains(7));
        assertTreeInvariants(list);
    }

    @Test
    public void testRetainAll_TwoChildrenDeletion_PreservesStructure() throws Exception {
        UnsynchronizedTreeList<Integer> list = new UnsynchronizedTreeList<>();
        for (int i = 1; i <= 10; i++) {
            list.add(i);
        }
        list.retainAll(List.of(1, 3, 5, 7, 9));
        assertEquals(5, list.size());
        for (int i = 0; i < list.size() - 1; i++) {
            assertTrue(list.get(i) < list.get(i + 1), "Sorted order violated after retainAll");
        }
        assertTreeInvariants(list);
    }

    // ─── modCount / ConcurrentModificationException ───────────────────────────

    @Test
    public void testIterator_ThrowsCME_AfterAdd() {
        UnsynchronizedTreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1); list.add(2); list.add(3);
        Iterator<Integer> it = list.iterator();
        it.next();
        list.add(4);
        assertThrows(ConcurrentModificationException.class, it::next);
    }

    @Test
    public void testIterator_ThrowsCME_AfterRemoveByObject() {
        UnsynchronizedTreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1); list.add(2); list.add(3);
        Iterator<Integer> it = list.iterator();
        it.next();
        list.remove(Integer.valueOf(3));
        assertThrows(ConcurrentModificationException.class, it::next);
    }

    @Test
    public void testIterator_ThrowsCME_AfterRemoveByIndex() {
        UnsynchronizedTreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1); list.add(2); list.add(3);
        Iterator<Integer> it = list.iterator();
        it.next();
        list.remove(0);
        assertThrows(ConcurrentModificationException.class, it::next);
    }

    @Test
    public void testIterator_ThrowsCME_AfterClear() {
        UnsynchronizedTreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1); list.add(2);
        Iterator<Integer> it = list.iterator();
        list.clear();
        assertThrows(ConcurrentModificationException.class, it::next);
    }

    @Test
    public void testListIterator_ThrowsCME_OnPrevious_AfterAdd() {
        UnsynchronizedTreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1); list.add(2); list.add(3);
        ListIterator<Integer> it = list.listIterator(3);
        it.previous();
        list.add(4);
        assertThrows(ConcurrentModificationException.class, it::previous);
    }

    // ─── listIterator — bidirectional state tracking ──────────────────────────

    @Test
    public void testListIterator_ForwardThenBackward_ReturnsCorrectElements() {
        UnsynchronizedTreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(10); list.add(20); list.add(30);
        ListIterator<Integer> it = list.listIterator(0);
        assertEquals(10, it.next());
        assertEquals(20, it.next());
        assertEquals(30, it.next());
        assertFalse(it.hasNext());
        assertEquals(30, it.previous());
        assertEquals(20, it.previous());
        assertEquals(10, it.previous());
        assertFalse(it.hasPrevious());
    }

    @Test
    public void testListIterator_NextIndex_And_PreviousIndex_TrackCorrectly() {
        UnsynchronizedTreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(10); list.add(20); list.add(30);
        ListIterator<Integer> it = list.listIterator(1);
        assertEquals(1, it.nextIndex());
        assertEquals(0, it.previousIndex());
        it.next();
        assertEquals(2, it.nextIndex());
        assertEquals(1, it.previousIndex());
        it.previous();
        assertEquals(1, it.nextIndex());
        assertEquals(0, it.previousIndex());
    }

    @Test
    public void testListIterator_StartAtEnd_PreviousTraversesFullList() {
        UnsynchronizedTreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1); list.add(2); list.add(3);
        ListIterator<Integer> it = list.listIterator(3);
        assertFalse(it.hasNext());
        assertEquals(3, it.previous());
        assertEquals(2, it.previous());
        assertEquals(1, it.previous());
        assertFalse(it.hasPrevious());
    }

    @Test
    public void testListIterator_ThrowsNoSuchElement_AtEnd() {
        UnsynchronizedTreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1);
        ListIterator<Integer> it = list.listIterator();
        it.next();
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    public void testListIterator_ThrowsNoSuchElement_AtStart() {
        UnsynchronizedTreeList<Integer> list = new UnsynchronizedTreeList<>();
        list.add(1);
        ListIterator<Integer> it = list.listIterator(0);
        assertThrows(NoSuchElementException.class, it::previous);
    }

    @Test
    public void testListIterator_EmptyList_ThrowsNoSuchElement_OnNext() {
        UnsynchronizedTreeList<Integer> list = new UnsynchronizedTreeList<>();
        ListIterator<Integer> it = list.listIterator(0);
        assertThrows(NoSuchElementException.class, it::next);
    }
}