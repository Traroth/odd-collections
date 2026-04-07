/*
 * UnsynchronizedTreeList.java
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

import java.io.Serializable;
import java.util.AbstractList;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

/**
 * A non-thread-safe implementation of {@link TreeList} backed by a red-black
 * tree augmented with subtree sizes (order-statistic tree).
 *
 * <p>The augmentation stores, in each node, the size of the subtree rooted at
 * that node. This allows index-based access ({@link #get(int)},
 * {@link #remove(int)}) to run in O(log n) rather than O(n).
 *
 * <p>All structural operations ({@link #add(Object)}, {@link #remove(int)},
 * {@link #remove(Object)}) run in O(log n). Lookup operations
 * ({@link #contains(Object)}, {@link #indexOf(Object)}) also run in O(log n)
 * by exploiting the sorted order.
 *
 * <p>This implementation is not thread-safe. For concurrent use, prefer
 * {@link SynchronizedTreeList}.
 *
 * @param <E> the type of elements maintained by this list
 * @author Dufrenoy
 * @version 1.0
 * @see TreeList
 * @see SynchronizedTreeList
 */
public class UnsynchronizedTreeList<E> extends AbstractList<E>
        implements TreeList<E>, Serializable {

    private static final long serialVersionUID = 1L;

    /*@
      @ public invariant (\forall int i; 0 <= i && i < size() - 1;
      @     compare(get(i), get(i + 1)) < 0);
      @ public invariant size() >= 0;
      @ public invariant (\forall int i; 0 <= i && i < size(); get(i) != null);
      @ model public pure helper int compare(E a, E b);
      @*/

    // ─── Constants ────────────────────────────────────────────────────────────────

    private static final boolean RED   = true;
    private static final boolean BLACK = false;

    // ─── Inner class ──────────────────────────────────────────────────────────────

    private static final class Node<E> implements Serializable {

        private static final long serialVersionUID = 1L;
        E       element;
        Node<E> left;
        Node<E> right;
        Node<E> parent;
        boolean color;
        int     subtreeSize;

        Node(E element, Node<E> parent) {
            this.element     = element;
            this.parent      = parent;
            this.color       = RED;
            this.subtreeSize = 1;
        }
    }

    // ─── Instance variables ───────────────────────────────────────────────────────

    private Node<E>                    root;
    private int                        size;
    private final Comparator<? super E> comparator;

    // ─── Constructors ─────────────────────────────────────────────────────────────

    /**
     * Constructs an empty {@code UnsynchronizedTreeList} ordered by the natural
     * ordering of its elements.
     *
     * <p>All elements inserted must implement {@link Comparable}. Attempting to
     * insert an element that does not will throw a {@link ClassCastException}.
     */
    //@ ensures size() == 0;
    //@ ensures !comparator().isPresent();
    public UnsynchronizedTreeList() {
        this.comparator = null;
    }

    /**
     * Constructs an empty {@code UnsynchronizedTreeList} ordered by the given
     * comparator.
     *
     * @param comparator the comparator used to order elements, or {@code null}
     *                   to use natural ordering
     */
    //@ ensures size() == 0;
    //@ ensures comparator == null ==> !comparator().isPresent();
    //@ ensures comparator != null ==> comparator().isPresent();
    public UnsynchronizedTreeList(Comparator<? super E> comparator) {
        this.comparator = comparator;
    }

    /**
     * Constructs a {@code UnsynchronizedTreeList} containing the elements of
     * the given collection, ordered by their natural ordering. Duplicate
     * elements (as determined by natural ordering) are silently discarded.
     *
     * @param c the collection whose elements are to be placed into this list
     * @throws ClassCastException   if any element is not mutually comparable
     * @throws NullPointerException if {@code c} is {@code null}, or if any
     *                              element of {@code c} is {@code null}
     */
    //@ requires c != null;
    //@ requires (\forall Object e; c.contains(e); e != null);
    //@ ensures !comparator().isPresent();
    //@ ensures (\forall Object e; c.contains(e); contains(e));
    public UnsynchronizedTreeList(Collection<? extends E> c) {
        this.comparator = null;
        addAll(Objects.requireNonNull(c));
    }

    /**
     * Constructs a {@code UnsynchronizedTreeList} containing the elements of
     * the given collection, ordered by the given comparator. Duplicate elements
     * (as determined by the comparator) are silently discarded.
     *
     * @param comparator the comparator used to order elements, or {@code null}
     *                   to use natural ordering
     * @param c          the collection whose elements are to be placed into this list
     * @throws ClassCastException   if any element is not mutually comparable
     * @throws NullPointerException if {@code c} is {@code null}, or if any
     *                              element of {@code c} is {@code null}
     */
    //@ requires c != null;
    //@ requires (\forall Object e; c.contains(e); e != null);
    //@ ensures (\forall Object e; c.contains(e); contains(e));
    public UnsynchronizedTreeList(Comparator<? super E> comparator, Collection<? extends E> c) {
        this.comparator = comparator;
        addAll(Objects.requireNonNull(c));
    }

    // ─── TreeList ─────────────────────────────────────────────────────────────────

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Comparator<? super E>> comparator() {
        return Optional.ofNullable(comparator);
    }

    // ─── List — size and indexed access ───────────────────────────────────────────

    /**
     * Returns the number of elements in this list.
     *
     * @return the number of elements in this list
     */
    //@ also
    //@ ensures \result >= 0;
    @Override
    public int size() {
        return size;
    }

    /**
     * Returns the element at the specified position in this list.
     * Runs in O(log n) using the augmented subtree sizes.
     *
     * @param index index of the element to return
     * @return the element at the specified position
     * @throws IndexOutOfBoundsException if {@code index < 0 || index >= size()}
     */
    //@ also
    //@ requires 0 <= index && index < size();
    //@ ensures \result != null;
    @Override
    public E get(int index) {
        checkIndex(index);
        return findByIndex(index).element;
    }

    // ─── List — insertion ─────────────────────────────────────────────────────────

    /**
     * Inserts the specified element in its sorted position in this list.
     * Returns {@code false} if an element equal to {@code e} (as determined by
     * the comparator or natural ordering) is already present, leaving the list
     * unchanged.
     *
     * @param e the element to add
     * @return {@code true} if this list changed as a result of the call,
     *         {@code false} if {@code e} was already present
     * @throws ClassCastException   if {@code e} is not mutually comparable with
     *                              the existing elements
     * @throws NullPointerException if {@code e} is {@code null}
     */
    //@ also
    //@ requires e != null;
    //@ ensures \result <==> !\old(contains(e));
    //@ ensures contains(e);
    //@ ensures !\old(contains(e)) ==> size() == \old(size()) + 1;
    //@ ensures  \old(contains(e)) ==> size() == \old(size());
    //@ ensures (\forall int i; 0 <= i && i < size() - 1;
    //@     compare(get(i), get(i + 1)) < 0);
    @Override
    public boolean add(E e) {
        Objects.requireNonNull(e);

        if (root == null) {
            root = new Node<>(e, null);
            root.color = BLACK;
            size = 1;
            modCount++;
            return true;
        }

        Node<E> parent = null;
        Node<E> t      = root;
        int     cmp    = 0;

        while (t != null) {
            parent = t;
            cmp = compareElements(e, t.element);
            if (cmp < 0) {
                t = t.left;
            } else if (cmp > 0) {
                t = t.right;
            } else {
                return false; // duplicate
            }
        }

        Node<E> newNode = new Node<>(e, parent);
        if (cmp < 0) {
            parent.left = newNode;
        } else {
            parent.right = newNode;
        }

        size++;
        modCount++;
        updateSizesUp(parent);
        fixAfterInsert(newNode);
        return true;
    }

    /**
     * Not supported. The position of elements in a {@code TreeList} is
     * determined by sort order, not by the caller.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public void add(int index, E element) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if {@code c} is {@code null}, or if any
     *                              element of {@code c} is {@code null}
     */
    @Override
    public boolean addAll(Collection<? extends E> c) {
        Objects.requireNonNull(c);
        return super.addAll(c);
    }

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException always (positional insertion is not supported)
     */
    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    // ─── List — removal ───────────────────────────────────────────────────────────

    /**
     * Removes the element at the specified position in this list.
     * Runs in O(log n).
     *
     * @param index the index of the element to be removed
     * @return the element previously at the specified position
     * @throws IndexOutOfBoundsException if {@code index < 0 || index >= size()}
     */
    //@ also
    //@ requires 0 <= index && index < size();
    //@ ensures size() == \old(size()) - 1;
    //@ ensures \result.equals(\old(get(index)));
    //@ ensures !contains(\result);
    //@ ensures (\forall int i; 0 <= i && i < size() - 1;
    //@     compare(get(i), get(i + 1)) < 0);
    @Override
    public E remove(int index) {
        checkIndex(index);
        Node<E> node = findByIndex(index);
        E result = node.element;
        deleteEntry(node);
        return result;
    }

    /**
     * Removes the specified element from this list if it is present.
     * Runs in O(log n) using the sorted structure.
     *
     * @param o the element to be removed
     * @return {@code true} if this list contained the specified element
     */
    //@ also
    //@ ensures \result <==> \old(contains(o));
    //@ ensures !contains(o);
    //@ ensures  \old(contains(o)) ==> size() == \old(size()) - 1;
    //@ ensures !\old(contains(o)) ==> size() == \old(size());
    @Override
    public boolean remove(Object o) {
        Node<E> node = findNode(o);
        if (node == null) {
            return false;
        }
        deleteEntry(node);
        return true;
    }

    /**
     * Removes from this list all elements that are contained in the specified
     * collection. Runs in O(m log n) where m is the size of {@code c}.
     *
     * @param c the collection containing elements to be removed
     * @return {@code true} if this list changed as a result of the call
     * @throws NullPointerException if {@code c} is {@code null}
     */
    //@ also
    //@ requires c != null;
    //@ ensures (\forall Object e; c.contains(e); !contains(e));
    //@ ensures \result <==> (\exists Object e; c.contains(e); \old(contains(e)));
    @Override
    public boolean removeAll(Collection<?> c) {
        Objects.requireNonNull(c);
        boolean modified = false;
        Node<E> n = firstNode();
        while (n != null) {
            E element = n.element;
            if (c.contains(element)) {
                deleteEntry(n);
                modified = true;
                // deleteEntry may have consumed successor(n) internally (two-children case),
                // so the pre-computed next pointer would be stale. Re-anchor on the tree.
                n = findFirstGreaterThan(element);
            } else {
                n = successor(n);
            }
        }
        return modified;
    }

    /**
     * Retains only the elements in this list that are contained in the
     * specified collection. Runs in O(n log m) where m is the size of {@code c}.
     *
     * @param c the collection containing elements to be retained
     * @return {@code true} if this list changed as a result of the call
     * @throws NullPointerException if {@code c} is {@code null}
     */
    //@ also
    //@ requires c != null;
    //@ ensures (\forall int i; 0 <= i && i < size(); c.contains(get(i)));
    //@ ensures \result <==> (\exists Object e; \old(contains(e)); !c.contains(e));
    @Override
    public boolean retainAll(Collection<?> c) {
        Objects.requireNonNull(c);
        boolean modified = false;
        Node<E> n = firstNode();
        while (n != null) {
            E element = n.element;
            if (!c.contains(element)) {
                deleteEntry(n);
                modified = true;
                // deleteEntry may have consumed successor(n) internally (two-children case),
                // so the pre-computed next pointer would be stale. Re-anchor on the tree.
                n = findFirstGreaterThan(element);
            } else {
                n = successor(n);
            }
        }
        return modified;
    }

    /**
     * Removes all elements from this list. The list will be empty after this
     * call returns.
     */
    //@ also
    //@ ensures size() == 0;
    @Override
    public void clear() {
        modCount++;
        root = null;
        size = 0;
    }

    // ─── List — unsupported positional mutations ──────────────────────────────────

    /**
     * Not supported. The position of elements in a {@code TreeList} is
     * determined by sort order, not by the caller.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public E set(int index, E element) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns a live view bounded by the element values at positions
     * {@code fromIndex} and {@code toIndex} at the time this method is called.
     *
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or
     *         {@code toIndex > size()}
     * @throws IllegalArgumentException  if {@code fromIndex > toIndex}
     */
    //@ also
    //@ requires fromIndex >= 0 && toIndex <= size();
    //@ requires fromIndex <= toIndex;
    @Override
    public TreeList<E> subList(int fromIndex, int toIndex) {
        subListRangeCheck(fromIndex, toIndex, size);

        E from = (fromIndex == 0) ? null : findByIndex(fromIndex).element;
        E to   = (toIndex == size) ? null : findByIndex(toIndex).element;
        return new SubList(from, to, fromIndex == 0, toIndex == size);
    }

    private static void subListRangeCheck(int fromIndex, int toIndex, int size) {
        if (fromIndex < 0) {
            throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
        }
        if (toIndex > size) {
            throw new IndexOutOfBoundsException("toIndex = " + toIndex);
        }
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException(
                    "fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
        }
    }

    // ─── List — queries ───────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if this list contains the specified element.
     * Runs in O(log n) by performing a binary search on the red-black tree.
     *
     * @param o the element whose presence is to be tested
     * @return {@code true} if this list contains the element
     */
    //@ also
    //@ ensures \result <==> (\exists int i; 0 <= i && i < size(); get(i).equals(o));
    @Override
    public boolean contains(Object o) {
        return findNode(o) != null;
    }

    /**
     * Returns the index of the first (and only) occurrence of the specified
     * element in this list, or {@code -1} if it is not present.
     * Runs in O(log n) by performing a binary search on the red-black tree.
     *
     * @param o the element to search for
     * @return the index of the element, or {@code -1} if not present
     */
    //@ also
    //@ ensures \result >= -1 && \result < size();
    //@ ensures \result == -1 <==> !contains(o);
    //@ ensures \result >= 0  ==> get(\result).equals(o);
    @Override
    public int indexOf(Object o) {
        Node<E> node = findNode(o);
        return node == null ? -1 : rankOf(node);
    }

    /**
     * Equivalent to {@link #indexOf(Object)} since this list contains no
     * duplicates.
     *
     * @param o the element to search for
     * @return the index of the element, or {@code -1} if not present
     */
    //@ also
    //@ ensures \result == indexOf(o);
    @Override
    public int lastIndexOf(Object o) {
        return indexOf(o);
    }

    // ─── Iterators ────────────────────────────────────────────────────────────────

    /**
     * Returns an iterator over the elements in this list in sorted (ascending)
     * order. The iterator performs an in-order traversal of the underlying
     * red-black tree in O(n) total.
     *
     * <p>The iterator does not support {@link Iterator#remove()}.
     *
     * @return an iterator over the elements in sorted order
     */
    @Override
    public Iterator<E> iterator() {
        return listIterator(0);
    }

    /**
     * Returns a list iterator over the elements in this list in sorted order,
     * starting at the specified position.
     *
     * <p>{@link ListIterator#add(Object)} and {@link ListIterator#set(Object)}
     * are not supported and will throw {@link UnsupportedOperationException}.
     *
     * @param index index of the first element to be returned by
     *              {@link ListIterator#next()}
     * @return a list iterator over the elements in sorted order
     * @throws IndexOutOfBoundsException if {@code index < 0 || index > size()}
     */
    //@ also
    //@ requires 0 <= index && index <= size();
    @Override
    public ListIterator<E> listIterator(int index) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        return new TreeListIterator(index);
    }

    // ─── Private helpers — queries ────────────────────────────────────────────────

    private void checkIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
    }

    @SuppressWarnings("unchecked")
    private int compareElements(Object a, Object b) {
        if (comparator != null) {
            return comparator.compare((E) a, (E) b);
        }
        return ((Comparable<Object>) a).compareTo(b);
    }

    private Node<E> findNode(Object o) {
        Objects.requireNonNull(o);
        Node<E> n = root;
        while (n != null) {
            int cmp = compareElements(o, n.element);
            if (cmp < 0) {
                n = n.left;
            } else if (cmp > 0) {
                n = n.right;
            } else {
                return n;
            }
        }
        return null;
    }

    private Node<E> findByIndex(int index) {
        Node<E> n = root;
        while (n != null) {
            int leftSize = size(n.left);
            if (index == leftSize) {
                return n;
            } else if (index < leftSize) {
                n = n.left;
            } else {
                index -= leftSize + 1;
                n = n.right;
            }
        }
        throw new IndexOutOfBoundsException();
    }

    private int rankOf(Node<E> n) {
        int rank = size(n.left);
        Node<E> p = n;
        while (p.parent != null) {
            if (p == p.parent.right) {
                rank += size(p.parent.left) + 1;
            }
            p = p.parent;
        }
        return rank;
    }

    private Node<E> firstNode() {
        Node<E> p = root;
        if (p != null) {
            while (p.left != null) {
                p = p.left;
            }
        }
        return p;
    }

    private Node<E> lastNode() {
        Node<E> p = root;
        if (p != null) {
            while (p.right != null) {
                p = p.right;
            }
        }
        return p;
    }

    private Node<E> findFirstGreaterThan(E element) {
        Node<E> n = root;
        Node<E> result = null;
        while (n != null) {
            int cmp = compareElements(element, n.element);
            if (cmp < 0) {
                result = n;
                n = n.left;
            } else {
                n = n.right;
            }
        }
        return result;
    }

    /**
     * Returns the first node whose element is greater than or equal to the
     * given element, or {@code null} if no such node exists. Differs from
     * {@link #findFirstGreaterThan(Object)} by including equality.
     */
    private Node<E> ceilingNode(E element) {
        Node<E> n = root;
        Node<E> result = null;
        while (n != null) {
            int cmp = compareElements(element, n.element);
            if (cmp < 0) {
                result = n;
                n = n.left;
            } else if (cmp > 0) {
                n = n.right;
            } else {
                return n;
            }
        }
        return result;
    }

    private static <X> Node<X> successor(Node<X> t) {
        if (t == null) {
            return null;
        }
        if (t.right != null) {
            Node<X> p = t.right;
            while (p.left != null) {
                p = p.left;
            }
            return p;
        }
        Node<X> p  = t.parent;
        Node<X> ch = t;
        while (p != null && ch == p.right) {
            ch = p;
            p  = p.parent;
        }
        return p;
    }

    private static <X> Node<X> predecessor(Node<X> t) {
        if (t == null) {
            return null;
        }
        if (t.left != null) {
            Node<X> p = t.left;
            while (p.right != null) {
                p = p.right;
            }
            return p;
        }
        Node<X> p  = t.parent;
        Node<X> ch = t;
        while (p != null && ch == p.left) {
            ch = p;
            p  = p.parent;
        }
        return p;
    }

    private static <X> int size(Node<X> n) {
        return n == null ? 0 : n.subtreeSize;
    }

    private void updateSizesUp(Node<E> n) {
        while (n != null) {
            n.subtreeSize = 1 + size(n.left) + size(n.right);
            n = n.parent;
        }
    }

    // ─── Private helpers — null-safe node accessors ───────────────────────────────

    private static <X> boolean colorOf(Node<X> p) {
        return p == null ? BLACK : p.color;
    }

    private static <X> Node<X> parentOf(Node<X> p) {
        return p == null ? null : p.parent;
    }

    private static <X> Node<X> leftOf(Node<X> p) {
        return p == null ? null : p.left;
    }

    private static <X> Node<X> rightOf(Node<X> p) {
        return p == null ? null : p.right;
    }

    private static <X> void setColor(Node<X> p, boolean c) {
        if (p != null) {
            p.color = c;
        }
    }

    // ─── Private helpers — tree rotations ────────────────────────────────────────

    private void rotateLeft(Node<E> x) {
        Node<E> y = x.right;
        x.right = y.left;
        if (y.left != null) {
            y.left.parent = x;
        }
        y.parent = x.parent;
        if (x.parent == null) {
            root = y;
        } else if (x == x.parent.left) {
            x.parent.left = y;
        } else {
            x.parent.right = y;
        }
        y.left    = x;
        x.parent  = y;
        y.subtreeSize = x.subtreeSize;
        x.subtreeSize = 1 + size(x.left) + size(x.right);
    }

    private void rotateRight(Node<E> x) {
        Node<E> y = x.left;
        x.left = y.right;
        if (y.right != null) {
            y.right.parent = x;
        }
        y.parent = x.parent;
        if (x.parent == null) {
            root = y;
        } else if (x == x.parent.right) {
            x.parent.right = y;
        } else {
            x.parent.left = y;
        }
        y.right   = x;
        x.parent  = y;
        y.subtreeSize = x.subtreeSize;
        x.subtreeSize = 1 + size(x.left) + size(x.right);
    }

    // ─── Private helpers — red-black fix after insert ─────────────────────────────

    private void fixAfterInsert(Node<E> x) {
        x.color = RED;
        while (x != null && x != root && x.parent.color == RED) {
            if (parentOf(x) == leftOf(parentOf(parentOf(x)))) {
                Node<E> y = rightOf(parentOf(parentOf(x)));
                if (colorOf(y) == RED) {
                    setColor(parentOf(x), BLACK);
                    setColor(y, BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    x = parentOf(parentOf(x));
                } else {
                    if (x == rightOf(parentOf(x))) {
                        x = parentOf(x);
                        rotateLeft(x);
                    }
                    setColor(parentOf(x), BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    rotateRight(parentOf(parentOf(x)));
                }
            } else {
                Node<E> y = leftOf(parentOf(parentOf(x)));
                if (colorOf(y) == RED) {
                    setColor(parentOf(x), BLACK);
                    setColor(y, BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    x = parentOf(parentOf(x));
                } else {
                    if (x == leftOf(parentOf(x))) {
                        x = parentOf(x);
                        rotateRight(x);
                    }
                    setColor(parentOf(x), BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    rotateLeft(parentOf(parentOf(x)));
                }
            }
        }
        root.color = BLACK;
    }

    // ─── Private helpers — deletion ───────────────────────────────────────────────

    private void deleteEntry(Node<E> p) {
        modCount++;
        size--;

        // If strictly internal, copy successor's element to p and delete the successor.
        if (p.left != null && p.right != null) {
            Node<E> s = successor(p);
            p.element = s.element;
            p = s;
        }

        // p has at most one non-null child.
        Node<E> replacement = (p.left != null ? p.left : p.right);

        if (replacement != null) {
            replacement.parent = p.parent;
            if (p.parent == null) {
                root = replacement;
            } else if (p == p.parent.left) {
                p.parent.left = replacement;
            } else {
                p.parent.right = replacement;
            }
            p.left = p.right = p.parent = null;
            updateSizesUp(replacement.parent);
            if (p.color == BLACK) {
                fixAfterDelete(replacement);
            }
        } else if (p.parent == null) {
            root = null;
        } else {
            // Leaf node — fix before unlinking (p stays in tree as phantom nil)
            if (p.color == BLACK) {
                fixAfterDelete(p);
            }
            if (p.parent != null) {
                if (p == p.parent.left) {
                    p.parent.left = null;
                } else {
                    p.parent.right = null;
                }
                updateSizesUp(p.parent);
                p.parent = null;
            }
        }
    }

    private void fixAfterDelete(Node<E> x) {
        while (x != root && colorOf(x) == BLACK) {
            if (x == leftOf(parentOf(x))) {
                Node<E> sib = rightOf(parentOf(x));
                if (colorOf(sib) == RED) {
                    setColor(sib, BLACK);
                    setColor(parentOf(x), RED);
                    rotateLeft(parentOf(x));
                    sib = rightOf(parentOf(x));
                }
                if (colorOf(leftOf(sib)) == BLACK && colorOf(rightOf(sib)) == BLACK) {
                    setColor(sib, RED);
                    x = parentOf(x);
                } else {
                    if (colorOf(rightOf(sib)) == BLACK) {
                        setColor(leftOf(sib), BLACK);
                        setColor(sib, RED);
                        rotateRight(sib);
                        sib = rightOf(parentOf(x));
                    }
                    setColor(sib, colorOf(parentOf(x)));
                    setColor(parentOf(x), BLACK);
                    setColor(rightOf(sib), BLACK);
                    rotateLeft(parentOf(x));
                    x = root;
                }
            } else {
                Node<E> sib = leftOf(parentOf(x));
                if (colorOf(sib) == RED) {
                    setColor(sib, BLACK);
                    setColor(parentOf(x), RED);
                    rotateRight(parentOf(x));
                    sib = leftOf(parentOf(x));
                }
                if (colorOf(rightOf(sib)) == BLACK && colorOf(leftOf(sib)) == BLACK) {
                    setColor(sib, RED);
                    x = parentOf(x);
                } else {
                    if (colorOf(leftOf(sib)) == BLACK) {
                        setColor(rightOf(sib), BLACK);
                        setColor(sib, RED);
                        rotateLeft(sib);
                        sib = leftOf(parentOf(x));
                    }
                    setColor(sib, colorOf(parentOf(x)));
                    setColor(parentOf(x), BLACK);
                    setColor(leftOf(sib), BLACK);
                    rotateRight(parentOf(x));
                    x = root;
                }
            }
        }
        setColor(x, BLACK);
    }

    // ─── Inner class — iterator ───────────────────────────────────────────────────

    private final class TreeListIterator implements ListIterator<E> {

        private Node<E> nextNode;
        private int     nextIndex;
        private int     expectedModCount;

        TreeListIterator(int index) {
            this.expectedModCount = modCount;
            this.nextIndex        = index;
            this.nextNode         = (index == size) ? null : findByIndex(index);
        }

        @Override
        public boolean hasNext() {
            return nextIndex < size;
        }

        @Override
        public E next() {
            checkForModification();
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            E result = nextNode.element;
            nextNode = successor(nextNode);
            nextIndex++;
            return result;
        }

        @Override
        public boolean hasPrevious() {
            return nextIndex > 0;
        }

        @Override
        public E previous() {
            checkForModification();
            if (!hasPrevious()) {
                throw new NoSuchElementException();
            }
            nextNode = (nextNode == null) ? lastNode() : predecessor(nextNode);
            nextIndex--;
            return nextNode.element;
        }

        @Override
        public int nextIndex() {
            return nextIndex;
        }

        @Override
        public int previousIndex() {
            return nextIndex - 1;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(E e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(E e) {
            throw new UnsupportedOperationException();
        }

        private void checkForModification() {
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
        }
    }

    // ─── Inner class — SubList (live view bounded by element values) ─────────────

    /**
     * A live view of a range of this {@code UnsynchronizedTreeList}, bounded by
     * element values rather than indices. The view is itself a {@link TreeList}:
     * sorted, duplicate-free, and supporting the same operations.
     *
     * <p>The bounds are captured at creation time and are immutable. All elements
     * {@code e} in the parent list such that
     * {@code compare(fromElement, e) <= 0 && compare(e, toElement) < 0} belong
     * to this view. If an element equal to a bound is removed from the parent,
     * the comparison still holds — the bound values act as markers, not node
     * references.
     *
     * <p>Structural modifications through this view update the parent list and
     * refresh the expected modification count. External modifications to the
     * parent invalidate this view (fail-fast via
     * {@link ConcurrentModificationException}).
     *
     * <p>Adding an element outside the value range throws
     * {@link IllegalArgumentException}, following the convention of
     * {@link java.util.TreeMap#subMap(Object, Object)}.
     */
    private final class SubList extends AbstractList<E> implements TreeList<E> {

        /*@
          @ public invariant size() >= 0;
          @ public invariant (\forall int i; 0 <= i && i < size(); get(i) != null);
          @ public invariant (\forall int i; 0 <= i && i < size() - 1;
          @     compare(get(i), get(i + 1)) < 0);
          @ public invariant !fromStart ==>
          @     (\forall int i; 0 <= i && i < size();
          @         compare(fromElement, get(i)) <= 0);
          @ public invariant !toEnd ==>
          @     (\forall int i; 0 <= i && i < size();
          @         compare(get(i), toElement) < 0);
          @ model public pure helper int compare(E a, E b);
          @*/

        private final E       fromElement;
        private final E       toElement;
        private final boolean fromStart;
        private final boolean toEnd;
        private int           expectedModCount;

        /**
         * Creates a new SubList bounded by element values.
         *
         * @param fromElement lower bound (inclusive), ignored if {@code fromStart}
         * @param toElement   upper bound (exclusive), ignored if {@code toEnd}
         * @param fromStart   {@code true} if there is no lower bound
         * @param toEnd       {@code true} if there is no upper bound
         */
        //@ ensures size() >= 0;
        SubList(E fromElement, E toElement, boolean fromStart, boolean toEnd) {
            this.fromElement      = fromElement;
            this.toElement        = toElement;
            this.fromStart        = fromStart;
            this.toEnd            = toEnd;
            this.expectedModCount = UnsynchronizedTreeList.this.modCount;
        }

        // ─── TreeList ────────────────────────────────────────────────────────────

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<Comparator<? super E>> comparator() {
            return UnsynchronizedTreeList.this.comparator();
        }

        // ─── List — size and indexed access ──────────────────────────────────────

        /**
         * Returns the number of elements in this view. Computed as the
         * difference between the ranks of the high and low boundaries in the
         * parent tree.
         *
         * @return the number of elements in this view
         */
        //@ also
        //@ ensures \result >= 0;
        @Override
        public int size() {
            checkForComodification();
            return Math.max(0, highIndex() - lowIndex());
        }

        /**
         * Returns the element at the specified position in this view.
         *
         * @param index index of the element to return (relative to this view)
         * @return the element at the specified position
         * @throws IndexOutOfBoundsException if {@code index < 0 || index >= size()}
         * @throws ConcurrentModificationException if the parent list was
         *         structurally modified outside this view
         */
        //@ also
        //@ requires 0 <= index && index < size();
        //@ ensures \result != null;
        @Override
        public E get(int index) {
            checkForComodification();
            int s = size();
            if (index < 0 || index >= s) {
                throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + s);
            }
            return findByIndex(lowIndex() + index).element;
        }

        // ─── List — insertion ────────────────────────────────────────────────────

        /**
         * Adds the specified element to the parent list, if it falls within
         * this view's value range and is not already present.
         *
         * @param e the element to add
         * @return {@code true} if the parent list changed
         * @throws IllegalArgumentException if {@code e} falls outside the
         *         view's value range
         * @throws NullPointerException     if {@code e} is {@code null}
         * @throws ConcurrentModificationException if the parent list was
         *         structurally modified outside this view
         */
        //@ also
        //@ requires e != null;
        //@ ensures contains(e);
        @Override
        public boolean add(E e) {
            checkForComodification();
            Objects.requireNonNull(e);
            if (!inRange(e)) {
                throw new IllegalArgumentException(
                        "Element " + e + " is outside the range of this subList");
            }
            boolean result = UnsynchronizedTreeList.this.add(e);
            expectedModCount = UnsynchronizedTreeList.this.modCount;
            return result;
        }

        /**
         * Not supported. The position of elements is determined by sort order.
         *
         * @throws UnsupportedOperationException always
         */
        @Override
        public void add(int index, E element) {
            throw new UnsupportedOperationException();
        }

        /**
         * Not supported. Positional insertion is not supported.
         *
         * @throws UnsupportedOperationException always
         */
        @Override
        public boolean addAll(int index, Collection<? extends E> c) {
            throw new UnsupportedOperationException();
        }

        // ─── List — removal ──────────────────────────────────────────────────────

        /**
         * Removes the element at the specified position in this view from the
         * parent list.
         *
         * @param index the index of the element to be removed (relative to
         *              this view)
         * @return the element previously at the specified position
         * @throws IndexOutOfBoundsException if {@code index < 0 || index >= size()}
         * @throws ConcurrentModificationException if the parent list was
         *         structurally modified outside this view
         */
        //@ also
        //@ requires 0 <= index && index < size();
        //@ ensures size() == \old(size()) - 1;
        //@ ensures \result != null;
        @Override
        public E remove(int index) {
            checkForComodification();
            int s = size();
            if (index < 0 || index >= s) {
                throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + s);
            }
            E result = UnsynchronizedTreeList.this.remove(lowIndex() + index);
            expectedModCount = UnsynchronizedTreeList.this.modCount;
            return result;
        }

        /**
         * Removes the specified element from the parent list, if it is present
         * in this view.
         *
         * @param o the element to be removed
         * @return {@code true} if the parent list changed
         * @throws ConcurrentModificationException if the parent list was
         *         structurally modified outside this view
         */
        //@ also
        //@ ensures \result <==> \old(contains(o));
        //@ ensures !contains(o);
        @Override
        public boolean remove(Object o) {
            checkForComodification();
            if (!inRange(o)) {
                return false;
            }
            boolean result = UnsynchronizedTreeList.this.remove(o);
            if (result) {
                expectedModCount = UnsynchronizedTreeList.this.modCount;
            }
            return result;
        }

        /**
         * Removes all elements in this view from the parent list.
         *
         * @throws ConcurrentModificationException if the parent list was
         *         structurally modified outside this view
         */
        //@ also
        //@ ensures size() == 0;
        @Override
        public void clear() {
            checkForComodification();
            // Iterate through the range and delete each element from the parent.
            Node<E> n = fromStart ? firstNode() : ceilingNode(fromElement);
            while (n != null) {
                if (!toEnd && compareElements(n.element, toElement) >= 0) {
                    break;
                }
                E element = n.element;
                deleteEntry(findNode(element));
                // Re-anchor: deleteEntry may have consumed successor(n)
                // internally (two-children case).
                n = findFirstGreaterThan(element);
            }
            expectedModCount = UnsynchronizedTreeList.this.modCount;
        }

        /**
         * Removes from the parent list all elements in this view that are
         * contained in the specified collection.
         *
         * <p>Overridden because {@link AbstractList#removeAll(Collection)}
         * delegates to {@link Iterator#remove()}, which is not supported.
         *
         * @param c the collection containing elements to be removed
         * @return {@code true} if this view changed as a result of the call
         * @throws NullPointerException if {@code c} is {@code null}
         * @throws ConcurrentModificationException if the parent list was
         *         structurally modified outside this view
         */
        //@ also
        //@ requires c != null;
        //@ ensures (\forall Object e; c.contains(e); !contains(e));
        @Override
        public boolean removeAll(Collection<?> c) {
            checkForComodification();
            Objects.requireNonNull(c);
            boolean modified = false;
            Node<E> n = fromStart ? firstNode() : ceilingNode(fromElement);
            while (n != null) {
                if (!toEnd && compareElements(n.element, toElement) >= 0) {
                    break;
                }
                E element = n.element;
                if (c.contains(element)) {
                    deleteEntry(findNode(element));
                    modified = true;
                    // Re-anchor: deleteEntry may have consumed successor(n)
                    // internally (two-children case).
                    n = findFirstGreaterThan(element);
                } else {
                    n = successor(n);
                }
            }
            if (modified) {
                expectedModCount = UnsynchronizedTreeList.this.modCount;
            }
            return modified;
        }

        /**
         * Retains only the elements in this view that are contained in the
         * specified collection. All other elements are removed from the parent
         * list.
         *
         * <p>Overridden because {@link AbstractList#retainAll(Collection)}
         * delegates to {@link Iterator#remove()}, which is not supported.
         *
         * @param c the collection containing elements to be retained
         * @return {@code true} if this view changed as a result of the call
         * @throws NullPointerException if {@code c} is {@code null}
         * @throws ConcurrentModificationException if the parent list was
         *         structurally modified outside this view
         */
        //@ also
        //@ requires c != null;
        //@ ensures (\forall int i; 0 <= i && i < size(); c.contains(get(i)));
        @Override
        public boolean retainAll(Collection<?> c) {
            checkForComodification();
            Objects.requireNonNull(c);
            boolean modified = false;
            Node<E> n = fromStart ? firstNode() : ceilingNode(fromElement);
            while (n != null) {
                if (!toEnd && compareElements(n.element, toElement) >= 0) {
                    break;
                }
                E element = n.element;
                if (!c.contains(element)) {
                    deleteEntry(findNode(element));
                    modified = true;
                    // Re-anchor: deleteEntry may have consumed successor(n)
                    // internally (two-children case).
                    n = findFirstGreaterThan(element);
                } else {
                    n = successor(n);
                }
            }
            if (modified) {
                expectedModCount = UnsynchronizedTreeList.this.modCount;
            }
            return modified;
        }

        // ─── List — unsupported positional mutations ─────────────────────────────

        /**
         * Not supported. The position of elements is determined by sort order.
         *
         * @throws UnsupportedOperationException always
         */
        @Override
        public E set(int index, E element) {
            throw new UnsupportedOperationException();
        }

        // ─── List — queries ──────────────────────────────────────────────────────

        /**
         * Returns {@code true} if this view contains the specified element.
         *
         * @param o the element whose presence is to be tested
         * @return {@code true} if the element is present in this view
         * @throws ConcurrentModificationException if the parent list was
         *         structurally modified outside this view
         */
        //@ also
        //@ ensures \result ==> inRange(o);
        @Override
        public boolean contains(Object o) {
            checkForComodification();
            if (!inRange(o)) {
                return false;
            }
            return findNode(o) != null;
        }

        /**
         * Returns the index of the specified element in this view, or
         * {@code -1} if it is not present.
         *
         * @param o the element to search for
         * @return the index of the element in this view, or {@code -1}
         * @throws ConcurrentModificationException if the parent list was
         *         structurally modified outside this view
         */
        //@ also
        //@ ensures \result >= -1 && \result < size();
        //@ ensures \result == -1 <==> !contains(o);
        @Override
        public int indexOf(Object o) {
            checkForComodification();
            if (!inRange(o)) {
                return -1;
            }
            int parentIndex = UnsynchronizedTreeList.this.indexOf(o);
            if (parentIndex < 0) {
                return -1;
            }
            return parentIndex - lowIndex();
        }

        /**
         * Equivalent to {@link #indexOf(Object)} since this view contains no
         * duplicates.
         */
        //@ also
        //@ ensures \result == indexOf(o);
        @Override
        public int lastIndexOf(Object o) {
            return indexOf(o);
        }

        // ─── Iterators ──────────────────────────────────────────────────────────

        /**
         * Returns an iterator over the elements in this view in sorted order.
         *
         * @return an iterator over the elements in this view
         */
        @Override
        public Iterator<E> iterator() {
            return listIterator(0);
        }

        /**
         * Returns a list iterator over the elements in this view, starting at
         * the specified position.
         *
         * @param index index of the first element to be returned
         * @return a list iterator starting at the specified position
         * @throws IndexOutOfBoundsException if {@code index < 0 || index > size()}
         * @throws ConcurrentModificationException if the parent list was
         *         structurally modified outside this view
         */
        //@ also
        //@ requires 0 <= index && index <= size();
        @Override
        public ListIterator<E> listIterator(int index) {
            checkForComodification();
            int s = size();
            if (index < 0 || index > s) {
                throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + s);
            }
            return new SubListIterator(index, s);
        }

        /**
         * List iterator for the SubList, bounded by the view's value range.
         * Traverses the parent tree in-order, restricted to elements within
         * {@code [fromElement, toElement)}.
         */
        private final class SubListIterator implements ListIterator<E> {

            private Node<E> nextNode;
            private int     nextIndex;
            private final int subSize;
            private final int iterExpectedModCount;

            SubListIterator(int index, int subSize) {
                this.iterExpectedModCount = UnsynchronizedTreeList.this.modCount;
                this.nextIndex            = index;
                this.subSize              = subSize;
                if (index == subSize) {
                    this.nextNode = null;
                } else {
                    this.nextNode = findByIndex(lowIndex() + index);
                }
            }

            @Override
            public boolean hasNext() {
                return nextIndex < subSize;
            }

            @Override
            public E next() {
                checkIteratorModification();
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                E result = nextNode.element;
                nextNode = successor(nextNode);
                nextIndex++;
                return result;
            }

            @Override
            public boolean hasPrevious() {
                return nextIndex > 0;
            }

            @Override
            public E previous() {
                checkIteratorModification();
                if (!hasPrevious()) {
                    throw new NoSuchElementException();
                }
                if (nextNode == null) {
                    // At the end — find the last node in range
                    int absIndex = lowIndex() + nextIndex - 1;
                    nextNode = findByIndex(absIndex);
                } else {
                    nextNode = predecessor(nextNode);
                }
                nextIndex--;
                return nextNode.element;
            }

            @Override
            public int nextIndex() {
                return nextIndex;
            }

            @Override
            public int previousIndex() {
                return nextIndex - 1;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void set(E e) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void add(E e) {
                throw new UnsupportedOperationException();
            }

            private void checkIteratorModification() {
                if (UnsynchronizedTreeList.this.modCount != iterExpectedModCount) {
                    throw new ConcurrentModificationException();
                }
            }
        }

        // ─── SubList — nested ────────────────────────────────────────────────────

        /**
         * Returns a nested live view with narrowed bounds, directly backed by
         * the root {@code UnsynchronizedTreeList} (no SubList-on-SubList
         * chaining).
         *
         * @param fromIndex low endpoint (inclusive) relative to this view
         * @param toIndex   high endpoint (exclusive) relative to this view
         * @return a view of the specified range within this view
         * @throws IndexOutOfBoundsException if endpoints are out of range
         * @throws IllegalArgumentException  if {@code fromIndex > toIndex}
         * @throws ConcurrentModificationException if the parent list was
         *         structurally modified outside this view
         */
        //@ also
        //@ requires fromIndex >= 0 && toIndex <= size();
        //@ requires fromIndex <= toIndex;
        //@ ensures \result.size() == toIndex - fromIndex;
        @Override
        public TreeList<E> subList(int fromIndex, int toIndex) {
            checkForComodification();
            int s = size();
            subListRangeCheck(fromIndex, toIndex, s);

            // Compute narrowed bounds relative to the parent tree.
            E newFrom;
            boolean newFromStart;
            if (fromIndex == 0) {
                newFrom      = this.fromElement;
                newFromStart = this.fromStart;
            } else {
                newFrom      = get(fromIndex);
                newFromStart = false;
            }

            E newTo;
            boolean newToEnd;
            if (toIndex == s) {
                newTo    = this.toElement;
                newToEnd = this.toEnd;
            } else {
                newTo    = get(toIndex);
                newToEnd = false;
            }

            return new SubList(newFrom, newTo, newFromStart, newToEnd);
        }

        // ─── Private helpers ─────────────────────────────────────────────────────

        /**
         * Checks that the parent list has not been structurally modified
         * outside this view.
         */
        private void checkForComodification() {
            if (UnsynchronizedTreeList.this.modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
        }

        /**
         * Returns {@code true} if the given element falls within this view's
         * value range: {@code [fromElement, toElement)}.
         */
        @SuppressWarnings("unchecked")
        private boolean inRange(Object o) {
            E e = (E) o;
            if (!fromStart && compareElements(e, fromElement) < 0) {
                return false;
            }
            if (!toEnd && compareElements(e, toElement) >= 0) {
                return false;
            }
            return true;
        }

        /**
         * Returns the rank (absolute index in the parent list) of the first
         * element in this view, or the parent size if this view is empty.
         */
        private int lowIndex() {
            if (fromStart) {
                return 0;
            }
            Node<E> lo = ceilingNode(fromElement);
            return lo == null ? size : rankOf(lo);
        }

        /**
         * Returns the rank (absolute index in the parent list) of the first
         * element past this view's upper bound, or the parent size if
         * {@code toEnd} is true.
         */
        private int highIndex() {
            if (toEnd) {
                return size;
            }
            Node<E> hi = ceilingNode(toElement);
            return hi == null ? size : rankOf(hi);
        }
    }
}
