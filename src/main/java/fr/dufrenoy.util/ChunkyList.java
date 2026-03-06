package fr.dufrenoy.util;/*
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
import java.util.List;

/**
 * A {@link List} backed by a chain of fixed-size arrays (Chunks), also known
 * as an unrolled linked list.
 *
 * <p>In addition to the standard {@link List} contract, this interface exposes
 * chunk-specific configuration: chunk size, growing and shrinking strategies,
 * and a {@link #reorganize()} operation to compact sparsely filled chunks.
 *
 * <p>Two implementations are provided:
 * <ul>
 *   <li>{@link UnsynchronizedChunkyList} — not thread-safe, fail-fast iterators.</li>
 *   <li>{@link SynchronizedChunkyList} — thread-safe, backed by a
 *       {@link java.util.concurrent.locks.ReentrantReadWriteLock}.</li>
 * </ul>
 *
 * @param <E> the type of elements in this list
 */
public interface ChunkyList<E> extends List<E> {

    // ─── Enums ────────────────────────────────────────────────────────────────

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
        /** The overflowing element is pushed into the next chunk (created if necessary). */
        OVERFLOW_STRATEGY,
        /** A new chunk is created after the current one to hold the overflowing element. */
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
     * Using a symmetric pair guarantees structural reversibility: inserting and then
     * removing the same element leaves the list in its original state.
     */
    enum ShrinkingStrategy {
        /** The first element of the next chunk is pulled into the current one; the chunk is removed if still empty. */
        UNDERFLOW_STRATEGY,
        /** The chunk is simply removed if it becomes empty. */
        DISAPPEAR_STRATEGY
    }

    // ─── Accessors ────────────────────────────────────────────────────────────

    /**
     * Returns the chunk size of this list.
     *
     * @return the chunk size
     */
    int getChunkSize();

    /**
     * Returns the current growing strategy.
     *
     * @return the current growing strategy
     * @see GrowingStrategy
     */
    GrowingStrategy getCurrentGrowingStrategy();

    /**
     * Sets the growing strategy. The new strategy applies immediately to all
     * subsequent insertions.
     *
     * @param growingStrategy the new growing strategy
     * @see GrowingStrategy
     */
    void setCurrentGrowingStrategy(GrowingStrategy growingStrategy);

    /**
     * Returns the current shrinking strategy.
     *
     * @return the current shrinking strategy
     * @see ShrinkingStrategy
     */
    ShrinkingStrategy getCurrentShrinkingStrategy();

    /**
     * Sets the shrinking strategy. The new strategy applies immediately to all
     * subsequent removals.
     *
     * @param shrinkingStrategy the new shrinking strategy
     * @see ShrinkingStrategy
     */
    void setCurrentShrinkingStrategy(ShrinkingStrategy shrinkingStrategy);

    /**
     * Sets both strategies atomically. Prefer this method over calling
     * {@link #setCurrentGrowingStrategy} and {@link #setCurrentShrinkingStrategy}
     * separately when both need to change together, as it guarantees no operation
     * can observe an inconsistent intermediate state.
     *
     * @param growingStrategy  the new growing strategy
     * @param shrinkingStrategy the new shrinking strategy
     */
    void setStrategies(GrowingStrategy growingStrategy, ShrinkingStrategy shrinkingStrategy);

    // ─── Operations ───────────────────────────────────────────────────────────

    /**
     * Reorganizes the list by redistributing all elements into chunks of
     * exactly {@code chunkSize} elements (except possibly the last one).
     * The order of elements is preserved.
     *
     * <p>This is useful after many removals have left chunks sparsely filled.
     */
    void reorganize();
}
