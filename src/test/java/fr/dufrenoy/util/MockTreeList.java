/*
 * MockTreeList.java
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Mock implementation of {@link TreeList} for testing the interface contract
 * in isolation. Delegates to a {@link TreeSet} to maintain sorted order and
 * the no-duplicates invariant. Not intended for production use.
 */
class MockTreeList<E> extends AbstractList<E> implements TreeList<E> {

    private final TreeSet<E>           set;
    private final Comparator<? super E> comparator;

    MockTreeList() {
        this.comparator = null;
        this.set        = new TreeSet<>();
    }

    MockTreeList(Comparator<? super E> comparator) {
        this.comparator = comparator;
        this.set        = new TreeSet<>(comparator);
    }

    // ─── TreeList ─────────────────────────────────────────────────────────────

    @Override
    public Optional<Comparator<? super E>> comparator() {
        return Optional.ofNullable(comparator);
    }

    // ─── List — required overrides ────────────────────────────────────────────

    @Override
    public int size() {
        return set.size();
    }

    @Override
    public E get(int index) {
        if (index < 0 || index >= size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());
        }
        return new ArrayList<>(set).get(index);
    }

    @Override
    public boolean add(E e) {
        Objects.requireNonNull(e);
        boolean changed = set.add(e);
        if (changed) {
            modCount++;
        }
        return changed;
    }

    @Override
    public void add(int index, E element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public E remove(int index) {
        E element = get(index);
        set.remove(element);
        modCount++;
        return element;
    }

    @Override
    public boolean remove(Object o) {
        boolean changed = set.remove(o);
        if (changed) {
            modCount++;
        }
        return changed;
    }

    @Override
    public E set(int index, E element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        set.clear();
        modCount++;
    }

    // ─── List — queries ───────────────────────────────────────────────────────

    @Override
    public boolean contains(Object o) {
        return set.contains(o);
    }

    @Override
    public int indexOf(Object o) {
        if (!set.contains(o)) {
            return -1;
        }
        return new ArrayList<>(set).indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return indexOf(o);
    }

    // ─── Iterators ────────────────────────────────────────────────────────────

    @Override
    public Iterator<E> iterator() {
        return set.iterator();
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        List<E> snapshot = new ArrayList<>(set);
        ListIterator<E> it = snapshot.listIterator(index);
        return new ListIterator<E>() {
            @Override public boolean hasNext()     { return it.hasNext(); }
            @Override public E next()              { return it.next(); }
            @Override public boolean hasPrevious() { return it.hasPrevious(); }
            @Override public E previous()          { return it.previous(); }
            @Override public int nextIndex()       { return it.nextIndex(); }
            @Override public int previousIndex()   { return it.previousIndex(); }
            @Override public void remove()         { throw new UnsupportedOperationException(); }
            @Override public void set(E e)         { throw new UnsupportedOperationException(); }
            @Override public void add(E e)         { throw new UnsupportedOperationException(); }
        };
    }
}