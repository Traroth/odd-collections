/*
 * ChunkyListBenchmark.java
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
 * {@link ArrayList} and {@link LinkedList} on the following operations:
 * <ul>
 *   <li>Add at end</li>
 *   <li>Add at middle</li>
 *   <li>Get by index</li>
 *   <li>Full iteration</li>
 *   <li>Sequential stream</li>
 *   <li>Parallel stream</li>
 * </ul>
 *
 * <p>Remove at middle is benchmarked separately in
 * {@link ChunkyListRemoveBenchmark} due to its need for per-invocation setup.
 *
 * <p>The {@code growingStrategy} and {@code shrinkingStrategy} parameters apply
 * only to {@link UnsynchronizedChunkyList} — they have no effect on
 * {@link ArrayList} or {@link LinkedList}, which are included as baselines
 * for all strategy combinations.
 *
 * <p>Run with: {@code java -jar target/benchmarks.jar}
 *
 * <p>Filter by operation: {@code java -jar target/benchmarks.jar ".*addAtEnd.*"}
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(2)
public class ChunkyListBenchmark {

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

    private List<Integer> chunkyList;
    private List<Integer> arrayList;
    private List<Integer> linkedList;

    /**
     * Populates all three lists with {@code size} elements before each benchmark iteration.
     * The growing and shrinking strategies are applied to the {@link UnsynchronizedChunkyList}
     * before population.
     * Using {@link Level#Invocation} would be too fine-grained and distort results —
     * {@link Level#Iteration} gives a stable pre-populated state for each measurement.
     */
    @Setup(Level.Iteration)
    public void setup() {
        UnsynchronizedChunkyList<Integer> cl = new UnsynchronizedChunkyList<>(chunkSize);
        cl.setCurrentGrowingStrategy(ChunkyList.GrowingStrategy.valueOf(growingStrategy));
        cl.setCurrentShrinkingStrategy(ChunkyList.ShrinkingStrategy.valueOf(shrinkingStrategy));
        chunkyList = cl;
        arrayList = new ArrayList<>();
        linkedList = new LinkedList<>();
        for (int i = 0; i < size; i++) {
            chunkyList.add(i);
            arrayList.add(i);
            linkedList.add(i);
        }
    }

    // ─── Add at end ───────────────────────────────────────────────────────────

    /** Measures the cost of appending a single element at the end of the list. */
    @Benchmark
    public void addAtEnd_ChunkyList(Blackhole bh) {
        chunkyList.add(size);
        bh.consume(chunkyList);
    }

    /** Measures the cost of appending a single element at the end of the list. */
    @Benchmark
    public void addAtEnd_ArrayList(Blackhole bh) {
        arrayList.add(size);
        bh.consume(arrayList);
    }

    /** Measures the cost of appending a single element at the end of the list. */
    @Benchmark
    public void addAtEnd_LinkedList(Blackhole bh) {
        linkedList.add(size);
        bh.consume(linkedList);
    }

    // ─── Add at middle ────────────────────────────────────────────────────────

    /** Measures the cost of inserting a single element at the middle of the list. */
    @Benchmark
    public void addAtMiddle_ChunkyList(Blackhole bh) {
        chunkyList.add(size / 2, size);
        bh.consume(chunkyList);
    }

    /** Measures the cost of inserting a single element at the middle of the list. */
    @Benchmark
    public void addAtMiddle_ArrayList(Blackhole bh) {
        arrayList.add(size / 2, size);
        bh.consume(arrayList);
    }

    /** Measures the cost of inserting a single element at the middle of the list. */
    @Benchmark
    public void addAtMiddle_LinkedList(Blackhole bh) {
        linkedList.add(size / 2, size);
        bh.consume(linkedList);
    }

    // ─── Get by index ─────────────────────────────────────────────────────────

    /** Measures the cost of retrieving the middle element by index. */
    @Benchmark
    public void getByIndex_ChunkyList(Blackhole bh) {
        bh.consume(chunkyList.get(size / 2));
    }

    /** Measures the cost of retrieving the middle element by index. */
    @Benchmark
    public void getByIndex_ArrayList(Blackhole bh) {
        bh.consume(arrayList.get(size / 2));
    }

    /** Measures the cost of retrieving the middle element by index. */
    @Benchmark
    public void getByIndex_LinkedList(Blackhole bh) {
        bh.consume(linkedList.get(size / 2));
    }

    // ─── Full iteration ───────────────────────────────────────────────────────

    /** Measures the cost of iterating over all elements of the list. */
    @Benchmark
    public void iterate_ChunkyList(Blackhole bh) {
        for (Integer element : chunkyList) {
            bh.consume(element);
        }
    }

    /** Measures the cost of iterating over all elements of the list. */
    @Benchmark
    public void iterate_ArrayList(Blackhole bh) {
        for (Integer element : arrayList) {
            bh.consume(element);
        }
    }

    /** Measures the cost of iterating over all elements of the list. */
    @Benchmark
    public void iterate_LinkedList(Blackhole bh) {
        for (Integer element : linkedList) {
            bh.consume(element);
        }
    }

    // ─── Stream (uses Spliterator) ────────────────────────────────────────────

    /** Measures the cost of streaming all elements sequentially. */
    @Benchmark
    public void stream_ChunkyList(Blackhole bh) {
        chunkyList.stream().forEach(bh::consume);
    }

    /** Measures the cost of streaming all elements sequentially. */
    @Benchmark
    public void stream_ArrayList(Blackhole bh) {
        arrayList.stream().forEach(bh::consume);
    }

    /** Measures the cost of streaming all elements sequentially. */
    @Benchmark
    public void stream_LinkedList(Blackhole bh) {
        linkedList.stream().forEach(bh::consume);
    }

    /** Measures the cost of streaming all elements in parallel. */
    @Benchmark
    public void parallelStream_ChunkyList(Blackhole bh) {
        chunkyList.parallelStream().forEach(bh::consume);
    }

    /** Measures the cost of streaming all elements in parallel. */
    @Benchmark
    public void parallelStream_ArrayList(Blackhole bh) {
        arrayList.parallelStream().forEach(bh::consume);
    }

    /** Measures the cost of streaming all elements in parallel. */
    @Benchmark
    public void parallelStream_LinkedList(Blackhole bh) {
        linkedList.parallelStream().forEach(bh::consume);
    }
}