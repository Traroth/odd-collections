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

import java.util.AbstractList;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
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
/*@
  @ public invariant (\forall int i; 0 <= i && i < size() - 1;
  @     compare(get(i), get(i + 1)) < 0);
  @ public invariant size() >= 0;
  @ public invariant (\forall int i; 0 <= i && i < size(); get(i) != null);
  @ model public pure helper int compare(E a, E b);
  @*/
public class UnsynchronizedTreeList<E> extends AbstractList<E> implements TreeList<E> {

    // ─── Constants ────────────────────────────────────────────────────────────────

    private static final boolean RED   = true;
    private static final boolean BLACK = false;

    // ─── Inner class ──────────────────────────────────────────────────────────────

    private static final class Node<E> {
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
    //@ ensures comparator().isEmpty();
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
    //@ ensures comparator == null ==> comparator().isEmpty();
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
    //@ ensures comparator().isEmpty();
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
    @Override
    //@ ensures \result >= 0;
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
    @Override
    //@ requires 0 <= index && index < size();
    //@ ensures \result != null;
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
    @Override
    //@ requires e != null;
    //@ ensures \result <==> !\old(contains(e));
    //@ ensures contains(e);
    //@ ensures !\old(contains(e)) ==> size() == \old(size()) + 1;
    //@ ensures  \old(contains(e)) ==> size() == \old(size());
    //@ ensures (\forall int i; 0 <= i && i < size() - 1;
    //@     compare(get(i), get(i + 1)) < 0);
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
            cmp = compare(e, t.element);
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
    @Override
    //@ requires 0 <= index && index < size();
    //@ ensures size() == \old(size()) - 1;
    //@ ensures \result.equals(\old(get(index)));
    //@ ensures !contains(\result);
    //@ ensures (\forall int i; 0 <= i && i < size() - 1;
    //@     compare(get(i), get(i + 1)) < 0);
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
    @Override
    //@ ensures \result <==> \old(contains(o));
    //@ ensures !contains(o);
    //@ ensures  \old(contains(o)) ==> size() == \old(size()) - 1;
    //@ ensures !\old(contains(o)) ==> size() == \old(size());
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
    @Override
    //@ requires c != null;
    //@ ensures (\forall Object e; c.contains(e); !contains(e));
    //@ ensures \result <==> (\exists Object e; c.contains(e); \old(contains(e)));
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
    @Override
    //@ requires c != null;
    //@ ensures (\forall int i; 0 <= i && i < size(); c.contains(get(i)));
    //@ ensures \result <==> (\exists Object e; \old(contains(e)); !c.contains(e));
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
    @Override
    //@ ensures size() == 0;
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
     * Not supported. A live subList view over a tree-backed structure is not
     * yet implemented.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException();
    }

    // ─── List — queries ───────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if this list contains the specified element.
     * Runs in O(log n) by performing a binary search on the red-black tree.
     *
     * @param o the element whose presence is to be tested
     * @return {@code true} if this list contains the element
     */
    @Override
    //@ ensures \result <==> (\exists int i; 0 <= i && i < size(); get(i).equals(o));
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
    @Override
    //@ ensures \result >= -1 && \result < size();
    //@ ensures \result == -1 <==> !contains(o);
    //@ ensures \result >= 0  ==> get(\result).equals(o);
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
    @Override
    //@ ensures \result == indexOf(o);
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
    @Override
    //@ requires 0 <= index && index <= size();
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
    private int compare(Object a, Object b) {
        if (comparator != null) {
            return comparator.compare((E) a, (E) b);
        }
        return ((Comparable<Object>) a).compareTo(b);
    }

    private Node<E> findNode(Object o) {
        Objects.requireNonNull(o);
        Node<E> n = root;
        while (n != null) {
            int cmp = compare(o, n.element);
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
            int cmp = compare(element, n.element);
            if (cmp < 0) {
                result = n;
                n = n.left;
            } else {
                n = n.right;
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

    private static <X> boolean colorOf(Node<X> p)              { return p == null ? BLACK : p.color; }
    private static <X> Node<X>  parentOf(Node<X> p)             { return p == null ? null  : p.parent; }
    private static <X> Node<X>  leftOf(Node<X> p)               { return p == null ? null  : p.left; }
    private static <X> Node<X>  rightOf(Node<X> p)              { return p == null ? null  : p.right; }
    private static <X> void     setColor(Node<X> p, boolean c)  { if (p != null) p.color = c; }

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
}