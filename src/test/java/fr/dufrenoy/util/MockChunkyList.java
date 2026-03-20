/*
 * MockChunkyList.java
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

import java.util.*;
import static fr.dufrenoy.util.ChunkyList.GrowingStrategy;
import static fr.dufrenoy.util.ChunkyList.ShrinkingStrategy;

/**
 * Minimal implementation of {@link ChunkyList} for interface contract tests.
 * Simulates the expected behaviour without implementing the real chunk logic.
 * Not intended for production use.
 */
final class MockChunkyList<E> implements ChunkyList<E> {

    // Same default value as in UnsynchronizedChunkyList
    private static final int DEFAULT_CHUNK_SIZE = 100;

    private final List<E> delegate = new ArrayList<>();
    private final int chunkSize;
    private GrowingStrategy growingStrategy = GrowingStrategy.OVERFLOW_STRATEGY;
    private ShrinkingStrategy shrinkingStrategy = ShrinkingStrategy.UNDERFLOW_STRATEGY;

    // Default constructor
    public MockChunkyList() {
        this(DEFAULT_CHUNK_SIZE);
    }

    // Constructor with chunkSize
    public MockChunkyList(int chunkSize) {
        if (chunkSize < 1) {
            throw new IllegalArgumentException("chunkSize must be at least 1");
        }
        this.chunkSize = chunkSize;
    }

    // ─── ChunkyList methods ───────────────────────────────────────────────────

    @Override public int getChunkSize() { return chunkSize; }

    @Override public GrowingStrategy getCurrentGrowingStrategy() { return growingStrategy; }
    @Override public void setCurrentGrowingStrategy(GrowingStrategy strategy) { this.growingStrategy = strategy; }

    @Override public ShrinkingStrategy getCurrentShrinkingStrategy() { return shrinkingStrategy; }
    @Override public void setCurrentShrinkingStrategy(ShrinkingStrategy strategy) { this.shrinkingStrategy = strategy; }

    @Override public void setStrategies(GrowingStrategy growing, ShrinkingStrategy shrinking) {
        this.growingStrategy = growing;
        this.shrinkingStrategy = shrinking;
    }

    @Override public void reorganize() {
        // No-op — simulates a reorganization without doing anything (for tests)
    }

    // ─── List methods (delegated to ArrayList) ────────────────────────────────

    @Override public int size() { return delegate.size(); }
    @Override public boolean isEmpty() { return delegate.isEmpty(); }
    @Override public boolean contains(Object o) { return delegate.contains(o); }
    @Override public Iterator<E> iterator() { return delegate.iterator(); }
    @Override public Object[] toArray() { return delegate.toArray(); }
    @Override public <T> T[] toArray(T[] a) { return delegate.toArray(a); }
    @Override public boolean add(E e) { return delegate.add(e); }
    @Override public boolean remove(Object o) { return delegate.remove(o); }
    @Override public boolean containsAll(Collection<?> c) { return delegate.containsAll(c); }
    @Override public boolean addAll(Collection<? extends E> c) { return delegate.addAll(c); }
    @Override public boolean addAll(int index, Collection<? extends E> c) { return delegate.addAll(index, c); }
    @Override public boolean removeAll(Collection<?> c) { return delegate.removeAll(c); }
    @Override public boolean retainAll(Collection<?> c) { return delegate.retainAll(c); }
    @Override public void clear() { delegate.clear(); }
    @Override public E get(int index) { return delegate.get(index); }
    @Override public E set(int index, E element) { return delegate.set(index, element); }
    @Override public void add(int index, E element) { delegate.add(index, element); }
    @Override public E remove(int index) { return delegate.remove(index); }
    @Override public int indexOf(Object o) { return delegate.indexOf(o); }
    @Override public int lastIndexOf(Object o) { return delegate.lastIndexOf(o); }
    @Override public ListIterator<E> listIterator() { return delegate.listIterator(); }
    @Override public ListIterator<E> listIterator(int index) { return delegate.listIterator(index); }
    @Override public List<E> subList(int fromIndex, int toIndex) { return delegate.subList(fromIndex, toIndex); }

    @Override
    public Spliterator<E> spliterator() {
        return delegate.spliterator();
    }
}