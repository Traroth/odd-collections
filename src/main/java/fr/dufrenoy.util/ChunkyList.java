package fr.dufrenoy.util;

import java.util.*;

/**
 * An unrolled linked list implementation of {@link List},
 * backed by a chain of fixed-size arrays (Chunks).
 *
 * <p>This implementation is <strong>not thread-safe</strong>. If multiple threads
 * access a {@code ChunkyList} instance concurrently, and at least one of them
 * modifies it structurally, it must be synchronized externally.
 * A structural modification is any operation that adds or removes one or more
 * elements, or explicitly resizes the list; merely setting the value of an element
 * is not a structural modification.
 *
 * <p>For concurrent access, it is recommended to use {@code SynchronizedChunkyList},
 * or to wrap the list at creation time:
 * <pre>
 *     List list = Collections.synchronizedList(new ChunkyList(...));
 * </pre>
 *
 * <p>The iterators returned by this class are <em>fail-fast</em>: if the list is
 * structurally modified after the iterator is created, the iterator will throw a
 * {@link ConcurrentModificationException}. This behaviour cannot be
 * guaranteed in a concurrent context without external synchronization.
 */
public class ChunkyList<E> extends AbstractList<E> {

    // ─── Constants ────────────────────────────────────────────────────────────

    private static final int DEFAULT_CHUNK_SIZE = 100;

    // ─── Enums ────────────────────────────────────────────────────────────────

    /**
     * Strategy used when inserting an element into a full chunk.
     * <ul>
     *   <li>{@link #OVERFLOW_STRATEGY} — the overflowing element is pushed into
     *       the next chunk (created if necessary).</li>
     *   <li>{@link #EXTEND_STRATEGY} — a new chunk is created after the current one
     *       to hold the overflowing element.</li>
     * </ul>
     */
    enum GrowingStrategy {
        OVERFLOW_STRATEGY,
        EXTEND_STRATEGY
    }

    /**
     * Strategy used after removing an element from a chunk.
     * <ul>
     *   <li>{@link #UNDERFLOW_STRATEGY} — the first element of the next chunk is
     *       pulled into the current one; if the chunk is still empty, it is removed.</li>
     *   <li>{@link #DISAPPEAR_STRATEGY} — the chunk is simply removed if it becomes empty.</li>
     * </ul>
     * <p>{@link #UNDERFLOW_STRATEGY} is the symmetric counterpart of
     * {@link GrowingStrategy#OVERFLOW_STRATEGY}, and {@link #DISAPPEAR_STRATEGY} is
     * the symmetric counterpart of {@link GrowingStrategy#EXTEND_STRATEGY}.
     */
    enum ShrinkingStrategy {
        UNDERFLOW_STRATEGY,
        DISAPPEAR_STRATEGY
    }

    // ─── Instance fields ──────────────────────────────────────────────────────

    private int size;
    private int chunkSize;
    private Chunk firstChunk;
    private Chunk lastChunk;
    private GrowingStrategy currentGrowingStrategy;
    private ShrinkingStrategy currentShrinkingStrategy;

    // ─── Constructors ─────────────────────────────────────────────────────────

    public ChunkyList() {
        this(DEFAULT_CHUNK_SIZE);
    }

    public ChunkyList(int chunkSize) {
        this.chunkSize = chunkSize;
        this.size = 0;
        this.currentGrowingStrategy = GrowingStrategy.OVERFLOW_STRATEGY;
        this.currentShrinkingStrategy = ShrinkingStrategy.UNDERFLOW_STRATEGY;
    }

    // ─── Accessors ────────────────────────────────────────────────────────────

    public GrowingStrategy getCurrentGrowingStrategy() {
        return currentGrowingStrategy;
    }

    public void setCurrentGrowingStrategy(GrowingStrategy currentGrowingStrategy) {
        this.currentGrowingStrategy = currentGrowingStrategy;
    }

    public ShrinkingStrategy getCurrentShrinkingStrategy() {
        return currentShrinkingStrategy;
    }

    public void setCurrentShrinkingStrategy(ShrinkingStrategy currentShrinkingStrategy) {
        this.currentShrinkingStrategy = currentShrinkingStrategy;
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
        if (lastChunk == null) {
            add(e, addEmptyChunkAfter(null), 0);
        } else if (lastChunk.nbElements < chunkSize) {
            add(e, lastChunk, lastChunk.nbElements);
        } else {
            handleFullChunk(lastChunk, e);
        }
        return true;
    }

    @Override
    public void add(int index, E element) {
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
                    // Room available in the chunk: shift right and insert
                    System.arraycopy(current.elements, remaining,
                                     current.elements, remaining + 1,
                                     current.nbElements - remaining);
                    current.elements[remaining] = element;
                    current.nbElements++;
                    size++;
                    modCount++;
                } else {
                    // Chunk is full: save last element, shift right, insert, then overflow
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
        // Insert into lastChunk
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
        // Remove from lastChunk
        E removed = current.elements[remaining];
        removeFromChunk(current, remaining);
        return removed;
    }

    @Override
    public void clear() {
        firstChunk = null;
        lastChunk = null;
        size = 0;
        modCount++;
    }

    // ─── Private methods ──────────────────────────────────────────────────────

    /**
     * Helper method split out from {@link #add(Object)} to keep method bytecode
     * size under 35 (the -XX:MaxInlineSize default value), which helps when
     * add(E) is called in a C1-compiled loop.
     */
    private void add(E e, Chunk chunk, int nbElements) {
        chunk.elements[nbElements] = e;
        chunk.nbElements++;
        size++;
        modCount++;
    }

    private void removeFromChunk(Chunk chunk, int i) {
        System.arraycopy(chunk.elements, i + 1,
                         chunk.elements, i,
                         chunk.nbElements - i - 1);
        chunk.elements[--chunk.nbElements] = null;
        size--;
        modCount++;
        handleShrunkChunk(chunk);
    }

    /**
     * Inserts a new empty chunk after the given chunk and links it into the chain.
     * If {@code chunk} is {@code null} (empty list), the new chunk becomes both
     * {@code firstChunk} and {@code lastChunk}.
     */
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

    /**
     * Handles the insertion of an overflowing element when a chunk is full,
     * according to the current {@link GrowingStrategy}:
     * <ul>
     *   <li>{@link GrowingStrategy#EXTEND_STRATEGY} — creates a new chunk after
     *       the current one and inserts {@code overflow} into it.</li>
     *   <li>{@link GrowingStrategy#OVERFLOW_STRATEGY} — shifts elements in the next
     *       chunk (created if necessary) and inserts {@code overflow} at its head.</li>
     * </ul>
     */
    private void handleFullChunk(Chunk chunk, E overflow) {
        switch (currentGrowingStrategy) {
            case EXTEND_STRATEGY:
                Chunk newChunk = addEmptyChunkAfter(chunk);
                add(overflow, newChunk, 0);
                break;
            case OVERFLOW_STRATEGY:
                Chunk next = chunk.nextChunk != null
                        ? chunk.nextChunk : addEmptyChunkAfter(chunk);
                System.arraycopy(next.elements, 0, next.elements, 1, next.nbElements);
                next.elements[0] = overflow;
                next.nbElements++;
                size++;
                modCount++;
                break;
        }
    }

    /**
     * Unlinks the given chunk from the chain, updating {@code firstChunk}
     * and {@code lastChunk} as needed.
     */
    private void removeChunk(Chunk chunk) {
        if (chunk.previousChunk != null) chunk.previousChunk.nextChunk = chunk.nextChunk;
        else firstChunk = chunk.nextChunk;
        if (chunk.nextChunk != null) chunk.nextChunk.previousChunk = chunk.previousChunk;
        else lastChunk = chunk.previousChunk;
    }

    /**
     * Handles the chunk after an element has been removed from it,
     * according to the current {@link ShrinkingStrategy}:
     * <ul>
     *   <li>{@link ShrinkingStrategy#DISAPPEAR_STRATEGY} — removes the chunk if empty.</li>
     *   <li>{@link ShrinkingStrategy#UNDERFLOW_STRATEGY} — pulls the first element of
     *       the next chunk into the current one; removes the chunk if still empty
     *       (no next chunk, or next chunk is empty).</li>
     * </ul>
     * <p>This method guarantees the invariant that no empty chunk ever remains in the chain.
     */
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
                }
                if (chunk.nbElements == 0) removeChunk(chunk);
                break;
        }
    }

    @Override
    public Spliterator<E> spliterator() {
        return new ChunkSpliterator(firstChunk, null, size);
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

        /** Current chunk being traversed. */
        private Chunk currentChunk;
        /** Index within the current chunk. */
        private int currentIndex;
        /**
         * The chunk at which this spliterator stops (exclusive).
         * {@code null} means traverse until the end of the list.
         */
        private Chunk endChunk;
        /** Remaining number of elements covered by this spliterator. */
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
            // Do not split if only one chunk remains
            if (currentChunk == null || currentChunk.nextChunk == endChunk) return null;

            // Count chunks in this spliterator to find the midpoint
            int chunkCount = 0;
            Chunk c = currentChunk;
            while (c != endChunk) {
                chunkCount++;
                c = c.nextChunk;
            }
            if (chunkCount < 2) return null;

            // Walk to the midpoint chunk
            int half = chunkCount / 2;
            Chunk mid = currentChunk;
            long firstHalfSize = 0;
            for (int i = 0; i < half; i++) {
                firstHalfSize += mid.nbElements;
                mid = mid.nextChunk;
            }
            // Adjust for elements already consumed in currentChunk
            firstHalfSize -= currentIndex;

            // Create spliterator for the first half [currentChunk, mid)
            ChunkSpliterator prefix = new ChunkSpliterator(currentChunk, mid, firstHalfSize);
            prefix.currentIndex = this.currentIndex;

            // This spliterator retains the second half [mid, endChunk)
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
