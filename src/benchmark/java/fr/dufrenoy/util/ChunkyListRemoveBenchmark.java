/*
 * ChunkyListRemoveBenchmark.java
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

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark comparing {@link UnsynchronizedChunkyList} against
 * {@link ArrayList} and {@link LinkedList} for remove-at-middle operations.
 *
 * <p>Kept separate from {@link ChunkyListBenchmark} because remove modifies the
 * list size, requiring a per-invocation setup ({@link Level#Invocation}) to
 * restore the list before each call. Using {@link Level#Invocation} on other
 * benchmarks would distort their measurements.
 *
 * <p>The {@code growingStrategy} and {@code shrinkingStrategy} parameters apply
 * only to {@link UnsynchronizedChunkyList} — they have no effect on
 * {@link ArrayList} or {@link LinkedList}, which are included as baselines
 * for all strategy combinations.
 *
 * <p>Note: {@link Level#Invocation} setup cost is included in the measurement.
 * Results should be interpreted as "remove + restore one element", not pure remove.
 * Use with care and compare relative differences between implementations.
 *
 * <p>Run with: {@code java -jar target/benchmarks.jar ".*Remove.*"}
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(2)
public class ChunkyListRemoveBenchmark {

    /** Number of elements in the list for each benchmark run. */
    @Param({"100", "1000", "10000", "100000"})
    private int size;

    /** Chunk size for UnsynchronizedChunkyList. */
    @Param({"10", "50", "100", "500"})
    private int chunkSize;

    /**
     * Growing strategy for UnsynchronizedChunkyList.
     * Has no effect on ArrayList or LinkedList.
     */
    @Param({"OVERFLOW_STRATEGY", "EXTEND_STRATEGY"})
    private String growingStrategy;

    /**
     * Shrinking strategy for UnsynchronizedChunkyList.
     * Has no effect on ArrayList or LinkedList.
     */
    @Param({"UNDERFLOW_STRATEGY", "DISAPPEAR_STRATEGY"})
    private String shrinkingStrategy;

    private UnsynchronizedChunkyList<Integer> chunkyList;
    private List<Integer> arrayList;
    private List<Integer> linkedList;

    /**
     * Restores all three lists to exactly {@code size} elements before each invocation.
     * Required because each {@code remove()} call reduces the list size by one,
     * which would cause an {@link IndexOutOfBoundsException} on subsequent invocations.
     *
     * <p>The growing and shrinking strategies are applied to the
     * {@link UnsynchronizedChunkyList} on first invocation only.
     *
     * <p>The setup cost is included in the measurement — results reflect
     * "remove + add one element at end", not pure remove cost. This is an acceptable
     * trade-off: the add cost is negligible compared to remove for large lists, and
     * relative comparisons between implementations remain valid.
     */
    @Setup(Level.Invocation)
    public void setup() {
        if (chunkyList == null) {
            chunkyList = new UnsynchronizedChunkyList<>(chunkSize);
            chunkyList.setCurrentGrowingStrategy(
                    ChunkyList.GrowingStrategy.valueOf(growingStrategy));
            chunkyList.setCurrentShrinkingStrategy(
                    ChunkyList.ShrinkingStrategy.valueOf(shrinkingStrategy));
            arrayList = new ArrayList<>();
            linkedList = new LinkedList<>();
            for (int i = 0; i < size; i++) {
                chunkyList.add(i);
                arrayList.add(i);
                linkedList.add(i);
            }
        } else {
            chunkyList.add(size);
            arrayList.add(size);
            linkedList.add(size);
        }
    }

    // ─── Remove at middle ─────────────────────────────────────────────────────

    /** Measures the cost of removing the middle element by index. */
    @Benchmark
    public void removeAtMiddle_ChunkyList(Blackhole bh) {
        bh.consume(chunkyList.remove(size / 2));
    }

    /** Measures the cost of removing the middle element by index. */
    @Benchmark
    public void removeAtMiddle_ArrayList(Blackhole bh) {
        bh.consume(arrayList.remove(size / 2));
    }

    /** Measures the cost of removing the middle element by index. */
    @Benchmark
    public void removeAtMiddle_LinkedList(Blackhole bh) {
        bh.consume(linkedList.remove(size / 2));
    }
}