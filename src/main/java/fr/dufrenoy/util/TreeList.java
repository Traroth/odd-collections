/*
 * TreeList.java
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

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * A sorted list that contains no duplicate elements, backed by a red-black tree.
 *
 * <p>Elements are ordered using either a {@link Comparator} supplied at
 * construction time, or their {@linkplain Comparable natural ordering}. Two
 * elements {@code a} and {@code b} are considered duplicates if and only if
 * {@code compare(a, b) == 0}.
 *
 * <p>Unlike a general-purpose {@link List}, the position of each element is
 * determined by its sort order, not by the caller. The following optional
 * {@code List} operations are therefore not supported and will always throw
 * {@link UnsupportedOperationException}:
 * <ul>
 *   <li>{@link #add(int, Object)}</li>
 *   <li>{@link #addAll(int, java.util.Collection)}</li>
 *   <li>{@link #set(int, Object)}</li>
 *   <li>{@link #subList(int, int)}</li>
 *   <li>{@link java.util.ListIterator#add(Object) ListIterator.add(E)}</li>
 *   <li>{@link java.util.ListIterator#set(Object) ListIterator.set(E)}</li>
 * </ul>
 *
 * <p>All elements inserted into a {@code TreeList} must be mutually comparable
 * using the list's comparator (or natural ordering). Attempting to insert an
 * incomparable element will throw a {@link ClassCastException}.
 *
 * <p>{@code null} elements are not permitted.
 *
 * <p>Duplicate elements are silently rejected: {@link #add(Object)} returns
 * {@code false} if the element is already present, leaving the list unchanged.
 *
 * <p>The {@link List#equals(Object)} contract is position-based: a
 * {@code TreeList} is equal to any {@code List} containing the same elements
 * in the same sorted order.
 *
 * <p>Two implementations are provided:
 * <ul>
 *   <li>{@link UnsynchronizedTreeList} — not thread-safe; O(log n) for
 *       insertion, removal, and index-based access.</li>
 *   <li>{@link SynchronizedTreeList} — thread-safe, backed by a
 *       {@link java.util.concurrent.locks.ReentrantReadWriteLock}.</li>
 * </ul>
 *
 * @param <E> the type of elements maintained by this list
 * @author Dufrenoy
 * @version 1.0
 * @see UnsynchronizedTreeList
 * @see SynchronizedTreeList
 */
public interface TreeList<E> extends List<E> {

    /**
     * Returns the comparator used to order the elements in this list, or an
     * empty {@link Optional} if the elements are ordered by their
     * {@linkplain Comparable natural ordering}.
     *
     * @return the comparator used to order this list, or an empty
     *         {@code Optional} if natural ordering is used
     */
    /*@ pure @*/ Optional<Comparator<? super E>> comparator();
}