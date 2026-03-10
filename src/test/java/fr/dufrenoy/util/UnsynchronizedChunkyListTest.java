package fr.dufrenoy.util;

import static fr.dufrenoy.util.ChunkyList.GrowingStrategy;
import static fr.dufrenoy.util.ChunkyList.ShrinkingStrategy;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

/**
 * Tests COMPLETS spécifiques à l'implémentation UnsynchronizedChunkyList.
 */
public class UnsynchronizedChunkyListTest {

    // ===== Tests de la structure interne =====
    @Test
    public void testInternalChunkStructure_WithSmallChunks() {
        UnsynchronizedChunkyList<String> list = new UnsynchronizedChunkyList<>(2);
        list.add("A");
        list.add("B");
        list.add("C");

        assertEquals(3, list.size());
        assertEquals(2, list.countChunks());
    }

    @Test
    public void testInternalChunkStructure_WithLargeChunks() {
        UnsynchronizedChunkyList<String> list = new UnsynchronizedChunkyList<>(100);
        for (int i = 0; i < 250; i++) {
            list.add("Item" + i);
        }

        assertEquals(250, list.size());
        assertEquals(3, list.countChunks());
    }

    @Test
    public void testInternalChunkStructure_AfterRemovals() {
        UnsynchronizedChunkyList<String> list = new UnsynchronizedChunkyList<>(3);
        for (int i = 0; i < 10; i++) {
            list.add("Item" + i);
        }
        list.remove(5);
        list.remove(3);
        list.remove(1);

        assertEquals(7, list.size());
        assertTrue(list.countChunks() >= 2);
    }

    // ===== Tests des stratégies =====
    @Test
    public void testOverflowStrategy() {
        UnsynchronizedChunkyList<String> list = new UnsynchronizedChunkyList<>(2);
        list.setCurrentGrowingStrategy(GrowingStrategy.OVERFLOW_STRATEGY);
        list.add("A");
        list.add("B");
        list.add("C");

        assertEquals(3, list.size());
        assertEquals(2, list.countChunks());
    }

    @Test
    public void testExtendStrategy() {
        UnsynchronizedChunkyList<String> list = new UnsynchronizedChunkyList<>(2);
        list.setCurrentGrowingStrategy(GrowingStrategy.EXTEND_STRATEGY);
        list.add("A");
        list.add("B");
        list.add("C");

        assertEquals(3, list.size());
        assertEquals(2, list.countChunks());
    }

    @Test
    public void testUnderflowStrategy() {
        UnsynchronizedChunkyList<String> list = new UnsynchronizedChunkyList<>(2);
        list.setCurrentShrinkingStrategy(ShrinkingStrategy.UNDERFLOW_STRATEGY);
        list.add("A");
        list.add("B");
        list.add("C");
        list.remove(0);

        assertEquals(2, list.size());
        assertEquals(1, list.countChunks());
    }

    @Test
    public void testDisappearStrategy() {
        UnsynchronizedChunkyList<String> list = new UnsynchronizedChunkyList<>(2);
        list.setCurrentShrinkingStrategy(ShrinkingStrategy.DISAPPEAR_STRATEGY);
        list.add("A");
        list.add("B");
        list.add("C");
        list.remove(0);
        list.remove(0);

        assertEquals(1, list.size());
        assertEquals(1, list.countChunks());
    }

    // ===== Tests de reorganize =====
    @Test
    public void testReorganize_WithSparseChunks() {
        UnsynchronizedChunkyList<String> list = new UnsynchronizedChunkyList<>(3);
        for (int i = 0; i < 10; i++) {
            list.add("Item" + i);
        }
        list.remove(5);
        list.remove(3);
        list.remove(1);

        int chunksBefore = list.countChunks();
        list.reorganize();
        int chunksAfter = list.countChunks();

        assertEquals(7, list.size());
        assertTrue(chunksAfter <= chunksBefore);
    }

    @Test
    public void testReorganize_PreservesOrder() {
        UnsynchronizedChunkyList<String> list = new UnsynchronizedChunkyList<>(3);
        for (int i = 0; i < 20; i++) {
            list.add("Item" + i);
        }

        List<String> before = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            before.add(list.get(i));
        }

        list.reorganize();

        for (int i = 0; i < list.size(); i++) {
            assertEquals(before.get(i), list.get(i));
        }
    }

    // ===== Tests de Spliterator et Stream =====
    @Test
    public void testSpliterator_Characteristics() {
        UnsynchronizedChunkyList<String> list = new UnsynchronizedChunkyList<>(2);
        list.add("A");
        list.add("B");
        list.add("C");

        Spliterator<String> spliterator = list.spliterator();
        assertTrue(spliterator.hasCharacteristics(Spliterator.SIZED));
        assertTrue(spliterator.hasCharacteristics(Spliterator.SUBSIZED));
        assertTrue(spliterator.hasCharacteristics(Spliterator.ORDERED));
    }

    @Test
    public void testSpliterator_TryAdvance() {
        UnsynchronizedChunkyList<String> list = new UnsynchronizedChunkyList<>(2);
        list.add("A");
        list.add("B");
        list.add("C");

        Spliterator<String> spliterator = list.spliterator();
        List<String> result = new ArrayList<>();
        while (spliterator.tryAdvance(result::add));
        assertEquals(Arrays.asList("A", "B", "C"), result);
    }

    @Test
    public void testStream_Sequential() {
        UnsynchronizedChunkyList<String> list = new UnsynchronizedChunkyList<>(2);
        list.add("A");
        list.add("B");
        list.add("C");

        List<String> result = list.stream()
                .map(String::toLowerCase)
                .collect(java.util.stream.Collectors.toList());
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testStream_Parallel() {
        UnsynchronizedChunkyList<Integer> list = new UnsynchronizedChunkyList<>(10);
        for (int i = 0; i < 100; i++) list.add(i);

        List<Integer> result = list.parallelStream()
                .filter(i -> i % 2 == 0)
                .collect(java.util.stream.Collectors.toList());
        assertEquals(50, result.size());
        assertTrue(result.stream().allMatch(i -> i % 2 == 0));
    }

    // ===== Tests du constructeur de copie avec nouvelle chunkSize =====

    @Test
    public void testCopyConstructor_ChunksFit_PreservesStructure() {
        UnsynchronizedChunkyList<String> original = new UnsynchronizedChunkyList<>(2);
        original.add("A");
        original.add("B");
        original.add("C");
        original.add("D");

        UnsynchronizedChunkyList<String> copy = new UnsynchronizedChunkyList<>(4, original);

        assertEquals(4, copy.size());
        assertEquals("A", copy.get(0));
        assertEquals("B", copy.get(1));
        assertEquals("C", copy.get(2));
        assertEquals("D", copy.get(3));
        assertEquals(2, copy.countChunks());
    }

    @Test
    public void testCopyConstructor_ChunksExceed_OverflowStrategy() {
        UnsynchronizedChunkyList<String> original = new UnsynchronizedChunkyList<>(4);
        original.add("A");
        original.add("B");
        original.add("C");
        original.add("D");

        UnsynchronizedChunkyList<String> copy = new UnsynchronizedChunkyList<>(2, original);
        copy.setCurrentGrowingStrategy(GrowingStrategy.OVERFLOW_STRATEGY);

        assertEquals(4, copy.size());
        assertEquals("A", copy.get(0));
        assertEquals("B", copy.get(1));
        assertEquals("C", copy.get(2));
        assertEquals("D", copy.get(3));
        assertEquals(2, copy.countChunks());
    }

    @Test
    public void testCopyConstructor_ChunksExceed_ExtendStrategy() {
        UnsynchronizedChunkyList<String> original = new UnsynchronizedChunkyList<>(4);
        original.add("A");
        original.add("B");
        original.add("C");
        original.add("D");

        UnsynchronizedChunkyList<String> copy = new UnsynchronizedChunkyList<>(2, original);
        copy.setCurrentGrowingStrategy(GrowingStrategy.EXTEND_STRATEGY);

        assertEquals(4, copy.size());
        assertEquals("A", copy.get(0));
        assertEquals("B", copy.get(1));
        assertEquals("C", copy.get(2));
        assertEquals("D", copy.get(3));
        assertEquals(2, copy.countChunks());
    }

    @Test
    public void testCopyConstructor_MixedChunks_PreservesOrder() {
        UnsynchronizedChunkyList<String> original = new UnsynchronizedChunkyList<>(4);
        original.add("A");
        original.add("B");
        original.add("C");
        original.add("D");
        original.add("E");

        UnsynchronizedChunkyList<String> copy = new UnsynchronizedChunkyList<>(2, original);

        assertEquals(5, copy.size());
        for (int i = 0; i < 5; i++) {
            assertEquals(original.get(i), copy.get(i));
        }
    }

    @Test
    public void testCopyConstructor_PreservesStrategies() {
        UnsynchronizedChunkyList<String> original = new UnsynchronizedChunkyList<>(4);
        original.setStrategies(GrowingStrategy.EXTEND_STRATEGY, ShrinkingStrategy.DISAPPEAR_STRATEGY);
        original.add("A");
        original.add("B");

        UnsynchronizedChunkyList<String> copy = new UnsynchronizedChunkyList<>(2, original);

        assertEquals(GrowingStrategy.EXTEND_STRATEGY, copy.getCurrentGrowingStrategy());
        assertEquals(ShrinkingStrategy.DISAPPEAR_STRATEGY, copy.getCurrentShrinkingStrategy());
    }
}
