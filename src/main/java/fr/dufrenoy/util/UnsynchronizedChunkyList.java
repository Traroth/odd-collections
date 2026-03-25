package fr.dufrenoy.util;
/*
 * ChunkyList - An unrolled linked list implementation of java.util.List
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
import java.util.*;

/**
 * An unrolled linked list implementation of {@link ChunkyList},
 * backed by a chain of fixed-size arrays (Chunks).
 *
 * <p>This implementation is <strong>not thread-safe</strong>. If multiple threads
 * access an {@code UnsynchronizedChunkyList} instance concurrently, and at least one
 * of them modifies it structurally, it must be synchronized externally.
 * A structural modification is any operation that adds or removes one or more
 * elements, or explicitly resizes the list; merely setting the value of an element
 * is not a structural modification.
 *
 * <p>For concurrent access, it is recommended to use {@code SynchronizedChunkyList},
 * or to wrap the list at creation time:
 * <pre>
 *     List list = Collections.synchronizedList(new UnsynchronizedChunkyList(...));
 * </pre>
 *
 * <p>The iterators returned by this class are <em>fail-fast</em>: if the list is
 * structurally modified after the iterator is created, the iterator will throw a
 * {@link ConcurrentModificationException}. This behaviour cannot be
 * guaranteed in a concurrent context without external synchronization.
 * @param <E> the type of elements in this list
 */
public class UnsynchronizedChunkyList<E> extends AbstractList<E> implements ChunkyList<E> {

    // ─── Constants ────────────────────────────────────────────────────────────

    private static final int DEFAULT_CHUNK_SIZE = 100;

    // ─── Instance fields ──────────────────────────────────────────────────────

    private int size;
    private final int chunkSize;
    private Chunk firstChunk;
    private Chunk lastChunk;
    private GrowingStrategy currentGrowingStrategy;
    private ShrinkingStrategy currentShrinkingStrategy;

    // ─── Constructors ─────────────────────────────────────────────────────────

    /**
     * Creates a new {@code UnsynchronizedChunkyList} with the default chunk size.
     */
    public UnsynchronizedChunkyList() {
        this(DEFAULT_CHUNK_SIZE);
    }

    /**
     * Creates a new {@code UnsynchronizedChunkyList} with the given chunk size.
     *
     * @param chunkSize the number of elements per chunk; must be at least 1
     * @throws IllegalArgumentException if {@code chunkSize} is less than 1
     */
    public UnsynchronizedChunkyList(int chunkSize) {
        if (chunkSize < 1) throw new IllegalArgumentException("chunkSize must be at least 1");
        this.chunkSize = chunkSize;
        this.size = 0;
        this.currentGrowingStrategy = GrowingStrategy.OVERFLOW_STRATEGY;
        this.currentShrinkingStrategy = ShrinkingStrategy.UNDERFLOW_STRATEGY;
    }

    /**
     * Creates a faithful copy of the given {@code UnsynchronizedChunkyList}, preserving
     * its chunk size (not the default chunk size), both strategies, and the
     * internal chunk structure.
     *
     * @param other the list to copy
     */
    public UnsynchronizedChunkyList(UnsynchronizedChunkyList<? extends E> other) {
        this(other.chunkSize);
        this.currentGrowingStrategy = other.currentGrowingStrategy;
        this.currentShrinkingStrategy = other.currentShrinkingStrategy;
        @SuppressWarnings("unchecked")
        UnsynchronizedChunkyList<E> src = (UnsynchronizedChunkyList<E>) other;
        Chunk current = src.firstChunk;
        while (current != null) {
            Chunk newChunk = current.copy();
            if (firstChunk == null) {
                firstChunk = newChunk;
            } else {
                lastChunk.nextChunk = newChunk;
                newChunk.previousChunk = lastChunk;
            }
            lastChunk = newChunk;
            size += current.nbElements;
            current = current != src.lastChunk ? current.nextChunk : null;
        }
    }

    /**
     * Creates a copy of the given {@code UnsynchronizedChunkyList} with a different chunk size,
     * preserving both strategies. Chunks whose number of elements does not exceed
     * the new chunk size are copied as-is. Chunks that exceed the new chunk size
     * are split according to the current {@link GrowingStrategy}.
     *
     * @param chunkSize the new chunk size; must be at least 1
     * @param other     the list to copy
     */
    public UnsynchronizedChunkyList(int chunkSize, UnsynchronizedChunkyList<? extends E> other) {
        this(chunkSize);
        this.currentGrowingStrategy = other.currentGrowingStrategy;
        this.currentShrinkingStrategy = other.currentShrinkingStrategy;
        @SuppressWarnings("unchecked")
        UnsynchronizedChunkyList<E> src = (UnsynchronizedChunkyList<E>) other;
        Chunk current = src.firstChunk;
        while (current != null) {
            if (current.nbElements <= chunkSize) {
                Chunk newChunk = current.copy();
                if (firstChunk == null) {
                    firstChunk = newChunk;
                } else {
                    lastChunk.nextChunk = newChunk;
                    newChunk.previousChunk = lastChunk;
                }
                lastChunk = newChunk;
                size += current.nbElements;
            } else {
                for (int i = 0; i < current.nbElements; i++) {
                    add(current.elements[i]);
                }
            }
            current = current != src.lastChunk ? current.nextChunk : null;
        }
    }

    /**
     * Creates a new {@code UnsynchronizedChunkyList} with the default chunk size containing
     * all elements of the given collection, in the order returned by its iterator.
     *
     * @param c the collection whose elements are to be placed into this list
     */
    public UnsynchronizedChunkyList(Collection<? extends E> c) {
        this(DEFAULT_CHUNK_SIZE, c);
    }

    /**
     * Creates a new {@code UnsynchronizedChunkyList} with the given chunk size containing
     * all elements of the given collection, in the order returned by its iterator.
     *
     * @param chunkSize the number of elements per chunk; must be at least 1
     * @param c         the collection whose elements are to be placed into this list
     */
    public UnsynchronizedChunkyList(int chunkSize, Collection<? extends E> c) {
        this(chunkSize);
        addAll(c);
    }

    // ─── Accessors ────────────────────────────────────────────────────────────

    @Override
    public int getChunkSize() {
        return chunkSize;
    }

    @Override
    public GrowingStrategy getCurrentGrowingStrategy() {
        return currentGrowingStrategy;
    }

    @Override
    public void setCurrentGrowingStrategy(GrowingStrategy currentGrowingStrategy) {
        this.currentGrowingStrategy = currentGrowingStrategy;
    }

    @Override
    public ShrinkingStrategy getCurrentShrinkingStrategy() {
        return currentShrinkingStrategy;
    }

    @Override
    public void setCurrentShrinkingStrategy(ShrinkingStrategy currentShrinkingStrategy) {
        this.currentShrinkingStrategy = currentShrinkingStrategy;
    }

    @Override
    public void setStrategies(GrowingStrategy growingStrategy, ShrinkingStrategy shrinkingStrategy) {
        this.currentGrowingStrategy = growingStrategy;
        this.currentShrinkingStrategy = shrinkingStrategy;
    }

    // ─── Public methods (List contract) ───────────────────────────────────────

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean contains(Object o) {
        return indexOf(o) != -1;
    }

    @Override
    public int indexOf(Object o) {
        if (isEmpty()) return -1;
        int index = 0;
        Chunk current = firstChunk;
        while (current != null) {
            for (int i = 0; i < current.nbElements; i++) {
                if (o == null ? current.elements[i] == null : o.equals(current.elements[i])) {
                    return index;
                }
                index++;
            }
            current = current != lastChunk ? current.nextChunk : null;
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        if (isEmpty()) return -1;
        int index = size - 1;
        Chunk current = lastChunk;
        while (current != null) {
            for (int i = current.nbElements - 1; i >= 0; i--) {
                if (o == null ? current.elements[i] == null : o.equals(current.elements[i])) {
                    return index;
                }
                index--;
            }
            current = current != firstChunk ? current.previousChunk : null;
        }
        return -1;
    }

    @Override
    public E get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        Chunk current = firstChunk;
        int remaining = index;
        do {
            if (remaining < current.nbElements) {
                return current.elements[remaining];
            }
            remaining -= current.nbElements;
            current = current.nextChunk;
        } while (current != lastChunk);
        return current.elements[remaining];
    }

    @Override
    public E set(int index, E element) {
        if (element == null) throw new IllegalArgumentException("null elements not allowed");
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        Chunk current = firstChunk;
        int remaining = index;
        do {
            if (remaining < current.nbElements) {
                E old = current.elements[remaining];
                current.elements[remaining] = element;
                return old;
            }
            remaining -= current.nbElements;
            current = current.nextChunk;
        } while (current != lastChunk);
        E old = current.elements[remaining];
        current.elements[remaining] = element;
        return old;
    }

    @Override
    public boolean add(E e) {
        if (e == null) throw new IllegalArgumentException("null elements not allowed");
        if (lastChunk == null) {
            add(e, addEmptyChunkAfter(null), 0);
        } else if (lastChunk.nbElements < chunkSize) {
            add(e, lastChunk, lastChunk.nbElements);
        } else {
            handleFullChunk(lastChunk, e);
        }
        return true;
    }

    /**
     * Appends all elements of the given collection to the end of this list,
     * in the order returned by the collection's iterator.
     *
     * <p>This implementation converts the collection to an array via
     * {@link Collection#toArray()}, then fills the last existing chunk and
     * creates new full chunks using {@link System#arraycopy}, avoiding the
     * per-element overhead of repeated {@link #add(Object)} calls.
     *
     * @param c the collection whose elements are to be appended
     * @return {@code true} if the list was modified
     * @throws IllegalArgumentException if any element in {@code c} is {@code null}
     */
    @Override
    public boolean addAll(Collection<? extends E> c) {
        Object[] incoming = c.toArray();
        int n = incoming.length;
        if (n == 0) return false;

        // Validate all elements before modifying the list
        for (int i = 0; i < n; i++) {
            if (incoming[i] == null) {
                throw new IllegalArgumentException("null elements not allowed");
            }
        }

        int srcPos = 0;

        // Fill the last existing chunk if it has room
        if (lastChunk != null && lastChunk.nbElements < chunkSize) {
            int room = chunkSize - lastChunk.nbElements;
            int toCopy = Math.min(room, n);
            System.arraycopy(incoming, srcPos, lastChunk.elements, lastChunk.nbElements, toCopy);
            lastChunk.nbElements += toCopy;
            size += toCopy;
            srcPos += toCopy;
        }

        // Create new chunks for the remaining elements
        while (srcPos < n) {
            int toCopy = Math.min(chunkSize, n - srcPos);
            Chunk newChunk = addEmptyChunkAfter(lastChunk);
            System.arraycopy(incoming, srcPos, newChunk.elements, 0, toCopy);
            newChunk.nbElements = toCopy;
            size += toCopy;
            srcPos += toCopy;
        }

        modCount++;
        return true;
    }

    @Override
    public void add(int index, E element) {
        if (element == null) throw new IllegalArgumentException("null elements not allowed");
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        if (index == size) {
            if (lastChunk == null) {
                add(element, addEmptyChunkAfter(null), 0);
            } else if (lastChunk.nbElements < chunkSize) {
                add(element, lastChunk, lastChunk.nbElements);
            } else {
                handleFullChunk(lastChunk, element);
            }
            return;
        }
        Chunk current = firstChunk;
        int remaining = index;
        do {
            if (remaining < current.nbElements) {
                if (current.nbElements < chunkSize) {
                    System.arraycopy(current.elements, remaining,
                            current.elements, remaining + 1,
                            current.nbElements - remaining);
                    current.elements[remaining] = element;
                    current.nbElements++;
                    size++;
                    modCount++;
                } else {
                    E overflow = current.elements[current.nbElements - 1];
                    System.arraycopy(current.elements, remaining,
                            current.elements, remaining + 1,
                            current.nbElements - remaining - 1);
                    current.elements[remaining] = element;
                    handleFullChunk(current, overflow);
                }
                return;
            }
            remaining -= current.nbElements;
            current = current.nextChunk;
        } while (current != lastChunk);
        System.arraycopy(current.elements, remaining,
                current.elements, remaining + 1,
                current.nbElements - remaining);
        current.elements[remaining] = element;
        current.nbElements++;
        size++;
        modCount++;
    }

    @Override
    public boolean remove(Object o) {
        if (isEmpty()) return false;
        Chunk current = firstChunk;
        while (current != null) {
            for (int i = 0; i < current.nbElements; i++) {
                if (o == null ? current.elements[i] == null : o.equals(current.elements[i])) {
                    removeFromChunk(current, i);
                    return true;
                }
            }
            current = current != lastChunk ? current.nextChunk : null;
        }
        return false;
    }

    @Override
    public E remove(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        Chunk current = firstChunk;
        int remaining = index;
        do {
            if (remaining < current.nbElements) {
                E removed = current.elements[remaining];
                removeFromChunk(current, remaining);
                return removed;
            }
            remaining -= current.nbElements;
            current = current.nextChunk;
        } while (current != lastChunk);
        E removed = current.elements[remaining];
        removeFromChunk(current, remaining);
        return removed;
    }

    /**
     * Reorganizes the list by redistributing all elements into chunks of
     * exactly {@code chunkSize} elements (except possibly the last one).
     * The order of elements is preserved.
     *
     * <p>This is useful after many removals have left chunks sparsely filled.
     */
    @Override
    public void reorganize() {
        if (isEmpty()) return;
        Chunk newFirst = null;
        Chunk newLast = null;
        Chunk current = firstChunk;
        int srcIndex = 0;
        while (current != null) {
            Chunk newChunk = new Chunk(chunkSize);
            if (newFirst == null) {
                newFirst = newChunk;
            } else {
                newLast.nextChunk = newChunk;
                newChunk.previousChunk = newLast;
            }
            newLast = newChunk;
            while (newChunk.nbElements < chunkSize && current != null) {
                newChunk.elements[newChunk.nbElements++] = current.elements[srcIndex++];
                if (srcIndex >= current.nbElements) {
                    current = current.nextChunk;
                    srcIndex = 0;
                }
            }
        }
        firstChunk = newFirst;
        lastChunk = newLast;
        modCount++;
    }

    @Override
    public void clear() {
        firstChunk = null;
        lastChunk = null;
        size = 0;
        modCount++;
    }

    // ─── Private methods ──────────────────────────────────────────────────────

    private void add(E e, Chunk chunk, int nbElements) {
        chunk.elements[nbElements] = e;
        chunk.nbElements++;
        size++;
        modCount++;
    }

    private void removeFromChunk(Chunk chunk, int i) {
        if (chunk.nbElements == 1) {
            // Skip arraycopy — remove the chunk directly
            removeChunk(chunk);
            size--;
            modCount++;
            return;
        }
        System.arraycopy(chunk.elements, i + 1,
                chunk.elements, i,
                chunk.nbElements - i - 1);
        chunk.elements[--chunk.nbElements] = null;
        size--;
        modCount++;
        handleShrunkChunk(chunk);
    }

    private Chunk addEmptyChunkAfter(Chunk chunk) {
        Chunk newChunk = new Chunk(chunkSize);
        if (chunk == null) {
            firstChunk = newChunk;
            lastChunk = newChunk;
            return newChunk;
        }
        newChunk.previousChunk = chunk;
        newChunk.nextChunk = chunk.nextChunk;
        if (chunk.nextChunk != null) {
            chunk.nextChunk.previousChunk = newChunk;
        } else {
            lastChunk = newChunk;
        }
        chunk.nextChunk = newChunk;
        return newChunk;
    }

    private void handleFullChunk(Chunk chunk, E overflow) {
        switch (currentGrowingStrategy) {
            case EXTEND_STRATEGY:
                Chunk newChunk = addEmptyChunkAfter(chunk);
                add(overflow, newChunk, 0);
                break;
            case OVERFLOW_STRATEGY:
                Chunk next = chunk.nextChunk != null
                        ? chunk.nextChunk : addEmptyChunkAfter(chunk);
                if (next.nbElements < chunkSize) {
                    System.arraycopy(next.elements, 0, next.elements, 1, next.nbElements);
                    next.elements[0] = overflow;
                    next.nbElements++;
                    size++;
                    modCount++;
                } else {
                    E nextOverflow = next.elements[next.nbElements - 1];
                    System.arraycopy(next.elements, 0, next.elements, 1, next.nbElements - 1);
                    next.elements[0] = overflow;
                    handleFullChunk(next, nextOverflow);
                }
                break;
        }
    }

    private void removeChunk(Chunk chunk) {
        if (chunk.previousChunk != null) chunk.previousChunk.nextChunk = chunk.nextChunk;
        else firstChunk = chunk.nextChunk;
        if (chunk.nextChunk != null) chunk.nextChunk.previousChunk = chunk.previousChunk;
        else lastChunk = chunk.previousChunk;
    }

    private void handleShrunkChunk(Chunk chunk) {
        switch (currentShrinkingStrategy) {
            case DISAPPEAR_STRATEGY:
                if (chunk.nbElements == 0) removeChunk(chunk);
                break;
            case UNDERFLOW_STRATEGY:
                if (chunk.nextChunk != null && chunk.nextChunk.nbElements > 0) {
                    chunk.elements[chunk.nbElements] = chunk.nextChunk.elements[0];
                    chunk.nbElements++;
                    System.arraycopy(chunk.nextChunk.elements, 1,
                            chunk.nextChunk.elements, 0,
                            chunk.nextChunk.nbElements - 1);
                    chunk.nextChunk.elements[--chunk.nextChunk.nbElements] = null;
                    handleShrunkChunk(chunk.nextChunk);
                }
                if (chunk.nbElements == 0) removeChunk(chunk);
                break;
        }
    }

    /**
     * Returns the number of chunks in this list.
     *
     * <p>This method is package-private and intended for testing purposes only.
     */
    final int countChunks() {
        int count = 0;
        Chunk current = firstChunk;
        while (current != null) {
            count++;
            current = current != lastChunk ? current.nextChunk : null;
        }
        return count;
    }

    @Override
    public Spliterator<E> spliterator() {
        return new ChunkSpliterator(firstChunk, null, size);
    }

    @Override
    public Iterator<E> iterator() {
        return new ChunkIterator();
    }

    private class ChunkIterator implements Iterator<E> {

        private Chunk currentChunk;
        private int currentIndex;
        private int expectedModCount;

        private ChunkIterator() {
            this.currentChunk = firstChunk;
            this.currentIndex = 0;
            this.expectedModCount = modCount;
        }

        @Override
        public boolean hasNext() {
            return currentChunk != null
                    && (currentIndex < currentChunk.nbElements
                    || currentChunk.nextChunk != null);
        }

        @Override
        public E next() {
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            E element = currentChunk.elements[currentIndex++];
            if (currentIndex >= currentChunk.nbElements) {
                currentChunk = currentChunk.nextChunk;
                currentIndex = 0;
            }
            return element;
        }
    }

    // ─── Inner classes ────────────────────────────────────────────────────────

    /**
     * A {@link Spliterator} that traverses the chunk chain natively,
     * maintaining a pointer to the current chunk and an index within it,
     * rather than delegating to {@link #get(int)}.
     *
     * <p>{@link #trySplit()} splits the covered chunk range in half,
     * returning a new {@code ChunkSpliterator} over the first half
     * and retaining the second half. Returns {@code null} when only
     * one chunk remains, as further splitting would not be beneficial.
     */
    private class ChunkSpliterator implements Spliterator<E> {

        private Chunk currentChunk;
        private int currentIndex;
        private Chunk endChunk;
        private long remaining;

        private ChunkSpliterator(Chunk startChunk, Chunk endChunk, long remaining) {
            this.currentChunk = startChunk;
            this.currentIndex = 0;
            this.endChunk = endChunk;
            this.remaining = remaining;
        }

        @Override
        public boolean tryAdvance(java.util.function.Consumer<? super E> action) {
            if (remaining <= 0) return false;
            action.accept(currentChunk.elements[currentIndex++]);
            remaining--;
            if (currentIndex >= currentChunk.nbElements) {
                currentChunk = currentChunk.nextChunk;
                currentIndex = 0;
            }
            return true;
        }

        @Override
        public Spliterator<E> trySplit() {
            if (currentChunk == null || currentChunk.nextChunk == endChunk) return null;

            int chunkCount = 0;
            Chunk c = currentChunk;
            while (c != endChunk) {
                chunkCount++;
                c = c.nextChunk;
            }
            if (chunkCount < 2) return null;

            int half = chunkCount / 2;
            Chunk mid = currentChunk;
            long firstHalfSize = 0;
            for (int i = 0; i < half; i++) {
                firstHalfSize += mid.nbElements;
                mid = mid.nextChunk;
            }
            firstHalfSize -= currentIndex;

            ChunkSpliterator prefix = new ChunkSpliterator(currentChunk, mid, firstHalfSize);
            prefix.currentIndex = this.currentIndex;

            this.currentChunk = mid;
            this.currentIndex = 0;
            this.remaining -= firstHalfSize;

            return prefix;
        }

        @Override
        public long estimateSize() {
            return remaining;
        }

        @Override
        public int characteristics() {
            return ORDERED | SIZED | SUBSIZED;
        }
    }

    // ─── Inner class: Chunk ───────────────────────────────────────────────────

    private class Chunk {

        private E[] elements;
        private int nbElements;
        private Chunk previousChunk;
        private Chunk nextChunk;

        @SuppressWarnings("unchecked")
        private Chunk(int chunkSize) {
            this.elements = (E[]) new Object[chunkSize];
            this.nbElements = 0;
        }

        private Chunk copy() {
            Chunk newChunk = new Chunk(chunkSize);
            System.arraycopy(elements, 0, newChunk.elements, 0, nbElements);
            newChunk.nbElements = nbElements;
            return newChunk;
        }

        private Chunk getPreviousChunk() {
            return previousChunk;
        }

        private void setPreviousChunk(Chunk previousChunk) {
            this.previousChunk = previousChunk;
        }

        private Chunk getNextChunk() {
            return nextChunk;
        }

        private void setNextChunk(Chunk nextChunk) {
            this.nextChunk = nextChunk;
        }
    }
}