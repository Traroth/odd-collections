/*
 * SynchronizedChunkyList.java
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

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Spliterator;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * A thread-safe implementation of {@link ChunkyList}, backed by an
 * {@link UnsynchronizedChunkyList} and protected by a
 * {@link ReentrantReadWriteLock}.
 *
 * <p>Multiple threads may read concurrently. Write operations are exclusive:
 * a write blocks until all ongoing reads have completed, and blocks any new
 * reads until the write is done.
 *
 * <p>The iterators returned by this class are <em>fail-safe</em>: {@link #iterator()},
 * {@link #listIterator()}, {@link #spliterator()}, and {@link #stream()} operate on
 * a snapshot of the list taken at the time of the call, under a read lock.
 * Subsequent modifications to the list are not reflected in the snapshot.
 *
 * <p><strong>Memory note:</strong> snapshot-based operations ({@link #spliterator()},
 * {@link #stream()}, {@link #parallelStream()}, {@link #iterator()},
 * {@link #listIterator()}) copy the entire list at the time of the call.
 * Avoid calling them on very large lists in memory-constrained environments.
 *
 * @param <E> the type of elements in this list
 */
public class SynchronizedChunkyList<E> implements ChunkyList<E> {

    /*@
      @ public invariant size() >= 0;
      @ public invariant (\forall int i; 0 <= i && i < size(); get(i) != null);
      @ public invariant getChunkSize() >= 1;
      @ public invariant getCurrentGrowingStrategy() != null;
      @ public invariant getCurrentShrinkingStrategy() != null;
      @*/

    // ─── Instance fields ──────────────────────────────────────────────────────

    private final UnsynchronizedChunkyList<E> inner;
    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = rwl.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = rwl.writeLock();

    // ─── Constructors ─────────────────────────────────────────────────────────

    /**
     * Creates a new {@code SynchronizedChunkyList} with the default chunk size.
     */
    //@ ensures size() == 0;
    //@ ensures getChunkSize() == 100;
    //@ ensures getCurrentGrowingStrategy() == GrowingStrategy.OVERFLOW_STRATEGY;
    //@ ensures getCurrentShrinkingStrategy() == ShrinkingStrategy.UNDERFLOW_STRATEGY;
    public SynchronizedChunkyList() {
        this.inner = new UnsynchronizedChunkyList<>();
    }

    /**
     * Creates a new {@code SynchronizedChunkyList} with the given chunk size.
     *
     * @param chunkSize the number of elements per chunk; must be at least 1
     */
    //@ requires chunkSize >= 1;
    //@ ensures size() == 0;
    //@ ensures getChunkSize() == chunkSize;
    //@ ensures getCurrentGrowingStrategy() == GrowingStrategy.OVERFLOW_STRATEGY;
    //@ ensures getCurrentShrinkingStrategy() == ShrinkingStrategy.UNDERFLOW_STRATEGY;
    public SynchronizedChunkyList(int chunkSize) {
        this.inner = new UnsynchronizedChunkyList<>(chunkSize);
    }

    /**
     * Creates a faithful copy of the given {@code SynchronizedChunkyList}, preserving
     * its chunk size, both strategies, and the internal chunk structure.
     *
     * <p>A read lock is acquired on {@code other} for the duration of the copy,
     * guaranteeing a consistent snapshot.
     *
     * @param other the list to copy
     */
    //@ requires other != null;
    //@ ensures size() == other.size();
    //@ ensures getChunkSize() == other.getChunkSize();
    //@ ensures getCurrentGrowingStrategy() == other.getCurrentGrowingStrategy();
    //@ ensures getCurrentShrinkingStrategy() == other.getCurrentShrinkingStrategy();
    //@ ensures (\forall int i; 0 <= i && i < size(); get(i).equals(other.get(i)));
    public SynchronizedChunkyList(SynchronizedChunkyList<? extends E> other) {
        other.readLock.lock();
        try {
            @SuppressWarnings("unchecked")
            UnsynchronizedChunkyList<E> copy =
                    new UnsynchronizedChunkyList<>((UnsynchronizedChunkyList<E>) other.inner);
            this.inner = copy;
        } finally {
            other.readLock.unlock();
        }
    }

    /**
     * Creates a copy of the given {@code SynchronizedChunkyList} with a different
     * chunk size, preserving both strategies.
     *
     * <p>A read lock is acquired on {@code other} for the duration of the copy,
     * guaranteeing a consistent snapshot.
     *
     * @param chunkSize the new chunk size; must be at least 1
     * @param other     the list to copy
     */
    //@ requires chunkSize >= 1;
    //@ requires other != null;
    //@ ensures size() == other.size();
    //@ ensures getChunkSize() == chunkSize;
    //@ ensures getCurrentGrowingStrategy() == other.getCurrentGrowingStrategy();
    //@ ensures getCurrentShrinkingStrategy() == other.getCurrentShrinkingStrategy();
    //@ ensures (\forall int i; 0 <= i && i < size(); get(i).equals(other.get(i)));
    public SynchronizedChunkyList(int chunkSize, SynchronizedChunkyList<? extends E> other) {
        other.readLock.lock();
        try {
            @SuppressWarnings("unchecked")
            UnsynchronizedChunkyList<E> copy =
                    new UnsynchronizedChunkyList<>(chunkSize, (UnsynchronizedChunkyList<E>) other.inner);
            this.inner = copy;
        } finally {
            other.readLock.unlock();
        }
    }

    /**
     * Creates a new {@code SynchronizedChunkyList} with the default chunk size
     * containing all elements of the given collection, in the order returned by
     * its iterator.
     *
     * @param c the collection whose elements are to be placed into this list
     */
    //@ requires c != null;
    //@ requires (\forall Object e; c.contains(e); e != null);
    //@ ensures size() == c.size();
    //@ ensures getChunkSize() == 100;
    public SynchronizedChunkyList(Collection<? extends E> c) {
        this.inner = new UnsynchronizedChunkyList<>(c);
    }

    /**
     * Creates a new {@code SynchronizedChunkyList} with the given chunk size
     * containing all elements of the given collection, in the order returned by
     * its iterator.
     *
     * @param chunkSize the number of elements per chunk; must be at least 1
     * @param c         the collection whose elements are to be placed into this list
     */
    //@ requires chunkSize >= 1;
    //@ requires c != null;
    //@ requires (\forall Object e; c.contains(e); e != null);
    //@ ensures size() == c.size();
    //@ ensures getChunkSize() == chunkSize;
    public SynchronizedChunkyList(int chunkSize, Collection<? extends E> c) {
        this.inner = new UnsynchronizedChunkyList<>(chunkSize, c);
    }

    // ─── Accessors ────────────────────────────────────────────────────────────

    /**
     * Returns the Chunksize, which is final, so no need for locking.
     * @return the chunk size of this list
     */
    //@ ensures \result >= 1;
    @Override
    public int getChunkSize() {
        return inner.getChunkSize();
    }

    //@ ensures \result != null;
    @Override
    public GrowingStrategy getCurrentGrowingStrategy() {
        readLock.lock();
        try {
            return inner.getCurrentGrowingStrategy();
        } finally {
            readLock.unlock();
        }
    }

    //@ requires growingStrategy != null;
    //@ ensures getCurrentGrowingStrategy() == growingStrategy;
    @Override
    public void setCurrentGrowingStrategy(GrowingStrategy growingStrategy) {
        writeLock.lock();
        try {
            inner.setCurrentGrowingStrategy(growingStrategy);
        } finally {
            writeLock.unlock();
        }
    }

    //@ ensures \result != null;
    @Override
    public ShrinkingStrategy getCurrentShrinkingStrategy() {
        readLock.lock();
        try {
            return inner.getCurrentShrinkingStrategy();
        } finally {
            readLock.unlock();
        }
    }

    //@ requires shrinkingStrategy != null;
    //@ ensures getCurrentShrinkingStrategy() == shrinkingStrategy;
    @Override
    public void setCurrentShrinkingStrategy(ShrinkingStrategy shrinkingStrategy) {
        writeLock.lock();
        try {
            inner.setCurrentShrinkingStrategy(shrinkingStrategy);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Sets both strategies atomically under a single write lock, guaranteeing
     * that no operation can observe an inconsistent intermediate state.
     */
    //@ requires growingStrategy != null;
    //@ requires shrinkingStrategy != null;
    //@ ensures getCurrentGrowingStrategy() == growingStrategy;
    //@ ensures getCurrentShrinkingStrategy() == shrinkingStrategy;
    @Override
    public void setStrategies(GrowingStrategy growingStrategy, ShrinkingStrategy shrinkingStrategy) {
        writeLock.lock();
        try {
            inner.setStrategies(growingStrategy, shrinkingStrategy);
        } finally {
            writeLock.unlock();
        }
    }

    // ─── List contract ────────────────────────────────────────────────────────

    //@ ensures \result >= 0;
    @Override
    public int size() {
        readLock.lock();
        try {
            return inner.size();
        } finally {
            readLock.unlock();
        }
    }

    //@ ensures \result <==> size() == 0;
    @Override
    public boolean isEmpty() {
        readLock.lock();
        try {
            return inner.isEmpty();
        } finally {
            readLock.unlock();
        }
    }

    //@ ensures \result <==> (\exists int i; 0 <= i && i < size(); get(i).equals(o));
    @Override
    public boolean contains(Object o) {
        readLock.lock();
        try {
            return inner.contains(o);
        } finally {
            readLock.unlock();
        }
    }

    //@ requires c != null;
    //@ ensures \result <==> (\forall Object e; c.contains(e); contains(e));
    @Override
    public boolean containsAll(Collection<?> c) {
        readLock.lock();
        try {
            return inner.containsAll(c);
        } finally {
            readLock.unlock();
        }
    }

    //@ ensures \result >= -1 && \result < size();
    //@ ensures \result == -1 <==> !contains(o);
    //@ ensures \result >= 0 ==> get(\result).equals(o);
    @Override
    public int indexOf(Object o) {
        readLock.lock();
        try {
            return inner.indexOf(o);
        } finally {
            readLock.unlock();
        }
    }

    //@ ensures \result >= -1 && \result < size();
    //@ ensures \result == -1 <==> !contains(o);
    //@ ensures \result >= 0 ==> get(\result).equals(o);
    @Override
    public int lastIndexOf(Object o) {
        readLock.lock();
        try {
            return inner.lastIndexOf(o);
        } finally {
            readLock.unlock();
        }
    }

    //@ requires 0 <= index && index < size();
    //@ ensures \result != null;
    @Override
    public E get(int index) {
        readLock.lock();
        try {
            return inner.get(index);
        } finally {
            readLock.unlock();
        }
    }

    //@ requires element != null;
    //@ requires 0 <= index && index < size();
    //@ ensures \result != null;
    //@ ensures get(index) == element;
    //@ ensures size() == \old(size());
    @Override
    public E set(int index, E element) {
        if (element == null) {
            throw new IllegalArgumentException("null elements not allowed");
        }
        writeLock.lock();
        try {
            return inner.set(index, element);
        } finally {
            writeLock.unlock();
        }
    }

    //@ requires e != null;
    //@ ensures \result == true;
    //@ ensures size() == \old(size()) + 1;
    //@ ensures get(size() - 1).equals(e);
    @Override
    public boolean add(E e) {
        writeLock.lock();
        try {
            return inner.add(e);
        } finally {
            writeLock.unlock();
        }
    }

    //@ requires element != null;
    //@ requires 0 <= index && index <= size();
    //@ ensures size() == \old(size()) + 1;
    //@ ensures get(index) == element;
    @Override
    public void add(int index, E element) {
        writeLock.lock();
        try {
            inner.add(index, element);
        } finally {
            writeLock.unlock();
        }
    }

    //@ requires c != null;
    //@ requires (\forall Object e; c.contains(e); e != null);
    //@ ensures \result <==> c.size() > 0;
    //@ ensures size() == \old(size()) + c.size();
    @Override
    public boolean addAll(Collection<? extends E> c) {
        writeLock.lock();
        try {
            return inner.addAll(c);
        } finally {
            writeLock.unlock();
        }
    }

    //@ requires c != null;
    //@ requires 0 <= index && index <= size();
    //@ requires (\forall Object e; c.contains(e); e != null);
    //@ ensures size() == \old(size()) + c.size();
    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        writeLock.lock();
        try {
            return inner.addAll(index, c);
        } finally {
            writeLock.unlock();
        }
    }

    //@ ensures \result <==> \old(contains(o));
    //@ ensures !contains(o);
    //@ ensures  \old(contains(o)) ==> size() == \old(size()) - 1;
    //@ ensures !\old(contains(o)) ==> size() == \old(size());
    @Override
    public boolean remove(Object o) {
        writeLock.lock();
        try {
            return inner.remove(o);
        } finally {
            writeLock.unlock();
        }
    }

    //@ requires 0 <= index && index < size();
    //@ ensures size() == \old(size()) - 1;
    //@ ensures \result != null;
    //@ ensures \result.equals(\old(get(index)));
    @Override
    public E remove(int index) {
        writeLock.lock();
        try {
            return inner.remove(index);
        } finally {
            writeLock.unlock();
        }
    }

    //@ requires c != null;
    //@ ensures (\forall Object e; c.contains(e); !contains(e));
    @Override
    public boolean removeAll(Collection<?> c) {
        writeLock.lock();
        try {
            return inner.removeAll(c);
        } finally {
            writeLock.unlock();
        }
    }

    //@ requires filter != null;
    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        writeLock.lock();
        try {
            return inner.removeIf(filter);
        } finally {
            writeLock.unlock();
        }
    }

    //@ requires c != null;
    //@ ensures (\forall int i; 0 <= i && i < size(); c.contains(get(i)));
    @Override
    public boolean retainAll(Collection<?> c) {
        writeLock.lock();
        try {
            return inner.retainAll(c);
        } finally {
            writeLock.unlock();
        }
    }

    //@ requires operator != null;
    //@ ensures size() == \old(size());
    @Override
    public void replaceAll(UnaryOperator<E> operator) {
        writeLock.lock();
        try {
            inner.replaceAll(operator);
        } finally {
            writeLock.unlock();
        }
    }

    //@ ensures size() == \old(size());
    @Override
    public void sort(Comparator<? super E> c) {
        writeLock.lock();
        try {
            inner.sort(c);
        } finally {
            writeLock.unlock();
        }
    }

    //@ ensures size() == 0;
    @Override
    public void clear() {
        writeLock.lock();
        try {
            inner.clear();
        } finally {
            writeLock.unlock();
        }
    }

    //@ requires 0 <= fromIndex && fromIndex <= toIndex && toIndex <= size();
    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        readLock.lock();
        try {
            return inner.subList(fromIndex, toIndex);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Object[] toArray() {
        readLock.lock();
        try {
            return inner.toArray();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public <T> T[] toArray(T[] a) {
        readLock.lock();
        try {
            return inner.toArray(a);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        readLock.lock();
        try {
            inner.forEach(action);
        } finally {
            readLock.unlock();
        }
    }

    // ─── Snapshot-based operations ────────────────────────────────────────────

    /**
     * Returns an iterator over a snapshot of this list taken under a read lock.
     *
     * <p><strong>Memory note:</strong> this operation copies the entire list.
     */
    @Override
    public Iterator<E> iterator() {
        return snapshot().iterator();
    }

    /**
     * Returns a list iterator over a snapshot of this list taken under a read lock.
     *
     * <p><strong>Memory note:</strong> this operation copies the entire list.
     */
    @Override
    public ListIterator<E> listIterator() {
        return snapshot().listIterator();
    }

    /**
     * Returns a list iterator over a snapshot of this list taken under a read lock,
     * starting at the given index.
     *
     * <p><strong>Memory note:</strong> this operation copies the entire list.
     */
    @Override
    public ListIterator<E> listIterator(int index) {
        return snapshot().listIterator(index);
    }

    /**
     * Returns a {@link Spliterator} over a snapshot of this list taken under a read lock.
     * The spliterator is {@link Spliterator#ORDERED}, {@link Spliterator#SIZED}, and
     * {@link Spliterator#SUBSIZED}.
     *
     * <p><strong>Memory note:</strong> this operation copies the entire list.
     */
    @Override
    public Spliterator<E> spliterator() {
        return snapshot().spliterator();
    }

    // ─── reorganize ───────────────────────────────────────────────────────────

    /**
     * Reorganizes the list by redistributing all elements into full chunks,
     * holding the write lock for the entire duration of the operation.
     *
     * <p>No reads or writes can proceed while this method is running.
     * This is the safe default; prefer it unless blocking is a concern.
     *
     * @see #reorganize(boolean)
     */
    //@ ensures size() == \old(size());
    //@ ensures (\forall int i; 0 <= i && i < size(); get(i).equals(\old(get(i))));
    @Override
    public void reorganize() {
        writeLock.lock();
        try {
            inner.reorganize();
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Reorganizes the list by redistributing all elements into full chunks.
     *
     * <ul>
     *   <li>If {@code blocking} is {@code true}, behaves identically to
     *       {@link #reorganize()}: the write lock is held for the entire operation.</li>
     *   <li>If {@code blocking} is {@code false}, uses a snapshot strategy:
     *       a copy of the list is taken under a read lock, reorganized without
     *       any lock, then swapped in under a write lock.
     *       <p><strong>Warning:</strong> any modifications made to the list between
     *       the snapshot and the final swap are silently lost. Use this mode only
     *       when the list is known to be quiescent or when occasional data loss
     *       is acceptable.</li>
     * </ul>
     *
     * @param blocking if {@code true}, holds the write lock for the full duration;
     *                 if {@code false}, uses a non-blocking snapshot strategy
     */
    //@ ensures size() == \old(size());
    public void reorganize(boolean blocking) {
        if (blocking) {
            reorganize();
            return;
        }
        // Take a snapshot under read lock
        UnsynchronizedChunkyList<E> copy;
        readLock.lock();
        try {
            copy = new UnsynchronizedChunkyList<>(inner);
        } finally {
            readLock.unlock();
        }
        // Reorganize the copy without any lock
        copy.reorganize();
        // Swap internal state under write lock
        writeLock.lock();
        try {
            inner.clear();
            inner.addAll(copy);
        } finally {
            writeLock.unlock();
        }
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    /**
     * Returns a copy of the inner list taken under a read lock.
     */
    private UnsynchronizedChunkyList<E> snapshot() {
        readLock.lock();
        try {
            return new UnsynchronizedChunkyList<>(inner);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Returns the number of chunks in this list.
     *
     * <p>This method is package-private and intended for testing purposes only.
     */
    final int countChunks() {
        readLock.lock();
        try {
            return inner.countChunks();
        } finally {
            readLock.unlock();
        }
    }

    // ─── Object ───────────────────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        readLock.lock();
        try {
            return inner.equals(o);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public int hashCode() {
        readLock.lock();
        try {
            return inner.hashCode();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public String toString() {
        readLock.lock();
        try {
            return inner.toString();
        } finally {
            readLock.unlock();
        }
    }
}
