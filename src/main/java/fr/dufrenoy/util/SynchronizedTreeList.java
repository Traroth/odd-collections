/*
 * SynchronizedTreeList.java
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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A thread-safe implementation of {@link TreeList} that delegates to an inner
 * {@link UnsynchronizedTreeList} protected by a
 * {@link ReentrantReadWriteLock}.
 *
 * <p>Multiple threads may read concurrently; write operations acquire an
 * exclusive write lock. All operations are therefore safe for concurrent use
 * without external synchronization.
 *
 * <p>{@link #iterator()}, {@link #listIterator()}, and {@link #listIterator(int)}
 * return snapshot-based iterators: a copy of the list is taken under a read
 * lock at the time the iterator is created. Subsequent modifications to the
 * list are not reflected in the iterator, and the iterator never throws
 * {@link java.util.ConcurrentModificationException}.
 *
 * <p>The unsupported operations of {@link TreeList} ({@link #add(int, Object)},
 * {@link #set(int, Object)}) throw {@link UnsupportedOperationException} as
 * documented.
 *
 * <p>{@link #subList(int, int)} returns a snapshot-based {@link TreeList}:
 * a copy of the elements in the specified range is taken under a read lock.
 * The returned list is an independent {@link UnsynchronizedTreeList} — it is
 * not a live view and modifications do not affect the original list.
 *
 * @param <E> the type of elements maintained by this list
 * @author Dufrenoy
 * @version 1.0
 * @see TreeList
 * @see UnsynchronizedTreeList
 */
public class SynchronizedTreeList<E> implements TreeList<E>, Serializable {

    private static final long serialVersionUID = 1L;

    /*@
      @ public invariant (\forall int i; 0 <= i && i < size() - 1;
      @     compare(get(i), get(i + 1)) < 0);
      @ public invariant size() >= 0;
      @ public invariant (\forall int i; 0 <= i && i < size(); get(i) != null);
      @ model public pure helper int compare(E a, E b);
      @*/

    // ─── Instance variables ───────────────────────────────────────────────────────

    private final UnsynchronizedTreeList<E>                delegate;
    private transient ReentrantReadWriteLock               lock;
    private transient ReentrantReadWriteLock.ReadLock      readLock;
    private transient ReentrantReadWriteLock.WriteLock     writeLock;

    // ─── Constructors ─────────────────────────────────────────────────────────────

    /**
     * Constructs an empty {@code SynchronizedTreeList} ordered by the natural
     * ordering of its elements.
     */
    //@ ensures size() == 0;
    //@ ensures comparator().isEmpty();
    public SynchronizedTreeList() {
        this.delegate  = new UnsynchronizedTreeList<>();
        this.lock      = new ReentrantReadWriteLock();
        this.readLock  = lock.readLock();
        this.writeLock = lock.writeLock();
    }

    /**
     * Constructs an empty {@code SynchronizedTreeList} ordered by the given
     * comparator.
     *
     * @param comparator the comparator used to order elements, or {@code null}
     *                   to use natural ordering
     */
    //@ ensures size() == 0;
    //@ ensures comparator == null ==> comparator().isEmpty();
    //@ ensures comparator != null ==> comparator().isPresent();
    public SynchronizedTreeList(Comparator<? super E> comparator) {
        this.delegate  = new UnsynchronizedTreeList<>(comparator);
        this.lock      = new ReentrantReadWriteLock();
        this.readLock  = lock.readLock();
        this.writeLock = lock.writeLock();
    }

    /**
     * Constructs a {@code SynchronizedTreeList} containing the elements of the
     * given collection, ordered by their natural ordering. Duplicate elements
     * are silently discarded.
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
    public SynchronizedTreeList(Collection<? extends E> c) {
        this.delegate  = new UnsynchronizedTreeList<>(c);
        this.lock      = new ReentrantReadWriteLock();
        this.readLock  = lock.readLock();
        this.writeLock = lock.writeLock();
    }

    /**
     * Constructs a {@code SynchronizedTreeList} containing the elements of the
     * given collection, ordered by the given comparator. Duplicate elements are
     * silently discarded.
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
    public SynchronizedTreeList(Comparator<? super E> comparator, Collection<? extends E> c) {
        this.delegate  = new UnsynchronizedTreeList<>(comparator, c);
        this.lock      = new ReentrantReadWriteLock();
        this.readLock  = lock.readLock();
        this.writeLock = lock.writeLock();
    }

    // ─── TreeList ─────────────────────────────────────────────────────────────────

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Comparator<? super E>> comparator() {
        return delegate.comparator();
    }

    // ─── List — size and indexed access ───────────────────────────────────────────

    /**
     * {@inheritDoc}
     */
    @Override
    //@ ensures \result >= 0;
    public int size() {
        readLock.lock();
        try {
            return delegate.size();
        } finally {
            readLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    //@ requires 0 <= index && index < size();
    //@ ensures \result != null;
    public E get(int index) {
        readLock.lock();
        try {
            return delegate.get(index);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty() {
        readLock.lock();
        try {
            return delegate.isEmpty();
        } finally {
            readLock.unlock();
        }
    }

    // ─── List — insertion ─────────────────────────────────────────────────────────

    /**
     * {@inheritDoc}
     */
    @Override
    //@ requires e != null;
    //@ ensures \result <==> !\old(contains(e));
    //@ ensures contains(e);
    //@ ensures !\old(contains(e)) ==> size() == \old(size()) + 1;
    //@ ensures  \old(contains(e)) ==> size() == \old(size());
    public boolean add(E e) {
        writeLock.lock();
        try {
            return delegate.add(e);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public void add(int index, E element) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addAll(Collection<? extends E> c) {
        Objects.requireNonNull(c);
        writeLock.lock();
        try {
            return delegate.addAll(c);
        } finally {
            writeLock.unlock();
        }
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
     * {@inheritDoc}
     */
    @Override
    //@ requires 0 <= index && index < size();
    //@ ensures size() == \old(size()) - 1;
    public E remove(int index) {
        writeLock.lock();
        try {
            return delegate.remove(index);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    //@ ensures \result <==> \old(contains(o));
    //@ ensures !contains(o);
    public boolean remove(Object o) {
        writeLock.lock();
        try {
            return delegate.remove(o);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeAll(Collection<?> c) {
        Objects.requireNonNull(c);
        writeLock.lock();
        try {
            return delegate.removeAll(c);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean retainAll(Collection<?> c) {
        Objects.requireNonNull(c);
        writeLock.lock();
        try {
            return delegate.retainAll(c);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    //@ ensures size() == 0;
    public void clear() {
        writeLock.lock();
        try {
            delegate.clear();
        } finally {
            writeLock.unlock();
        }
    }

    // ─── List — unsupported positional mutations ──────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public E set(int index, E element) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a snapshot of the specified range as an independent
     * {@link UnsynchronizedTreeList}. The snapshot is taken under a read lock
     * and is not a live view — modifications to the returned list do not
     * affect this list, and vice versa.
     *
     * <p>This is consistent with the snapshot-based iterator pattern used
     * throughout {@code SynchronizedTreeList}.
     *
     * @param fromIndex low endpoint (inclusive) of the subList
     * @param toIndex   high endpoint (exclusive) of the subList
     * @return a snapshot {@code TreeList} containing the elements in the range
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or
     *         {@code toIndex > size()}
     * @throws IllegalArgumentException  if {@code fromIndex > toIndex}
     */
    //@ requires fromIndex >= 0 && toIndex <= size();
    //@ requires fromIndex <= toIndex;
    @Override
    public TreeList<E> subList(int fromIndex, int toIndex) {
        readLock.lock();
        try {
            List<E> view = delegate.subList(fromIndex, toIndex);
            UnsynchronizedTreeList<E> snapshot =
                    new UnsynchronizedTreeList<>(delegate.comparator().orElse(null));
            for (int i = 0; i < view.size(); i++) {
                snapshot.add(view.get(i));
            }
            return snapshot;
        } finally {
            readLock.unlock();
        }
    }

    // ─── List — queries ───────────────────────────────────────────────────────────

    /**
     * {@inheritDoc}
     */
    @Override
    //@ ensures \result <==> (\exists int i; 0 <= i && i < size(); get(i).equals(o));
    public boolean contains(Object o) {
        readLock.lock();
        try {
            return delegate.contains(o);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsAll(Collection<?> c) {
        readLock.lock();
        try {
            return delegate.containsAll(c);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    //@ ensures \result >= -1 && \result < size();
    //@ ensures \result == -1 <==> !contains(o);
    //@ ensures \result >= 0  ==> get(\result).equals(o);
    public int indexOf(Object o) {
        readLock.lock();
        try {
            return delegate.indexOf(o);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    //@ ensures \result == indexOf(o);
    public int lastIndexOf(Object o) {
        readLock.lock();
        try {
            return delegate.lastIndexOf(o);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object[] toArray() {
        readLock.lock();
        try {
            return delegate.toArray();
        } finally {
            readLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T[] toArray(T[] a) {
        readLock.lock();
        try {
            return delegate.toArray(a);
        } finally {
            readLock.unlock();
        }
    }

    // ─── Object ───────────────────────────────────────────────────────────────────

    /**
     * Compares the specified object with this list for equality. Two lists are
     * equal if they contain the same elements in the same order. Delegates to
     * the inner {@link UnsynchronizedTreeList} under a read lock.
     *
     * @param o the object to be compared for equality with this list
     * @return {@code true} if the specified object is equal to this list
     */
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        readLock.lock();
        try {
            return delegate.equals(o);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Returns the hash code value for this list. Delegates to the inner
     * {@link UnsynchronizedTreeList} under a read lock.
     *
     * @return the hash code value for this list
     */
    @Override
    public int hashCode() {
        readLock.lock();
        try {
            return delegate.hashCode();
        } finally {
            readLock.unlock();
        }
    }

    // ─── Iterators ────────────────────────────────────────────────────────────────

    /**
     * Returns a snapshot-based iterator over the elements in this list in
     * sorted order. The snapshot is taken under a read lock at the time this
     * method is called. Subsequent modifications to the list are not reflected
     * in the iterator.
     *
     * <p>The iterator does not support {@link Iterator#remove()}.
     *
     * @return a snapshot iterator over the elements in sorted order
     */
    @Override
    public Iterator<E> iterator() {
        return listIterator(0);
    }

    /**
     * Returns a snapshot-based list iterator over the elements in this list in
     * sorted order, starting at the specified position. The snapshot is taken
     * under a read lock at the time this method is called.
     *
     * <p>{@link ListIterator#add(Object)} and {@link ListIterator#set(Object)}
     * are not supported and will throw {@link UnsupportedOperationException}.
     *
     * @param index index of the first element to be returned by
     *              {@link ListIterator#next()}
     * @return a snapshot list iterator over the elements in sorted order
     * @throws IndexOutOfBoundsException if {@code index < 0 || index > size()}
     */
    @Override
    //@ requires 0 <= index && index <= size();
    public ListIterator<E> listIterator(int index) {
        readLock.lock();
        try {
            List<E> snapshot = Collections.unmodifiableList(new ArrayList<>(delegate));
            return snapshot.listIterator(index);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ListIterator<E> listIterator() {
        return listIterator(0);
    }

    // ─── Serialization ───────────────────────────────────────────────────────────

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        lock = new ReentrantReadWriteLock();
        readLock = lock.readLock();
        writeLock = lock.writeLock();
    }
}
