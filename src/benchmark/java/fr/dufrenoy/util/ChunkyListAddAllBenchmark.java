/*
 * ChunkyListAddAllBenchmark.java
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
 * JMH benchmark comparing {@link UnsynchronizedChunkyList#addAll} against
 * {@link ArrayList#addAll} and {@link LinkedList#addAll}.
 *
 * <p>Kept separate from {@link ChunkyListBenchmark} because {@code addAll}
 * structurally modifies the target list on every call, requiring a
 * per-invocation setup ({@link Level#Invocation}) to restore it.
 *
 * <p>The source collection is an {@link ArrayList} prepared once at trial level —
 * we measure the cost of {@code addAll} on the target, not on the source.
 *
 * <p>The {@code growingStrategy} and {@code shrinkingStrategy} parameters apply
 * only to {@link UnsynchronizedChunkyList} — they have no effect on
 * {@link ArrayList} or {@link LinkedList}, which are included as baselines.
 *
 * <p>Note: {@link Level#Invocation} setup cost is included in the measurement.
 * Results reflect "addAll + clear target", not pure addAll cost. The clear cost
 * is O(1) for all three implementations and negligible compared to addAll,
 * so relative comparisons between implementations remain valid.
 *
 * <p>Run with: {@code java -jar target/benchmarks.jar ".*AddAll.*"}
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(2)
public class ChunkyListAddAllBenchmark {

    /** Number of elements in the source collection. */
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

    /** Source collection — prepared once, never modified. */
    private List<Integer> source;

    private UnsynchronizedChunkyList<Integer> chunkyList;
    private List<Integer> arrayList;
    private List<Integer> linkedList;

    /**
     * Prepares the source collection once per trial.
     * The source is an {@link ArrayList} to ensure {@code toArray()} is O(1).
     */
    @Setup(Level.Trial)
    public void setupSource() {
        source = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            source.add(i);
        }
    }

    /**
     * Resets all three target lists to empty before each invocation.
     * Required because each {@code addAll} call fills the target list,
     * which would accumulate across invocations without this reset.
     *
     * <p>The {@link UnsynchronizedChunkyList} is fully recreated on first
     * invocation to apply the configured strategies.
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
        } else {
            chunkyList.clear();
            arrayList.clear();
            linkedList.clear();
        }
    }

    // ─── addAll ───────────────────────────────────────────────────────────────

    /** Measures the cost of appending all elements of the source collection. */
    @Benchmark
    public void addAll_ChunkyList(Blackhole bh) {
        chunkyList.addAll(source);
        bh.consume(chunkyList);
    }

    /** Measures the cost of appending all elements of the source collection. */
    @Benchmark
    public void addAll_ArrayList(Blackhole bh) {
        arrayList.addAll(source);
        bh.consume(arrayList);
    }

    /** Measures the cost of appending all elements of the source collection. */
    @Benchmark
    public void addAll_LinkedList(Blackhole bh) {
        linkedList.addAll(source);
        bh.consume(linkedList);
    }
}