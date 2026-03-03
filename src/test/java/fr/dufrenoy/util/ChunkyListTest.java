package fr.dufrenoy.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import java.lang.reflect.Field;

/**
 * Comprehensive test suite for ChunkyList.
 */
public class ChunkyListTest {

    // ======================
    // Tests des constructeurs
    // ======================

    @Test
    public void testDefaultConstructor() {
        ChunkyList<String> list = new ChunkyList<>();
        assertEquals(0, list.size());
        assertEquals(100, list.getChunkSize());
        assertEquals(ChunkyList.GrowingStrategy.OVERFLOW_STRATEGY, list.getCurrentGrowingStrategy());
        assertEquals(ChunkyList.ShrinkingStrategy.UNDERFLOW_STRATEGY, list.getCurrentShrinkingStrategy());
    }

    @Test
    public void testConstructor_WithChunkSize() {
        ChunkyList<String> list = new ChunkyList<>(5);
        assertEquals(0, list.size());
        assertEquals(5, list.getChunkSize());
    }

    @Test
    public void testConstructor_InvalidChunkSize() {
        assertThrows(IllegalArgumentException.class, () -> new ChunkyList<>(0));
    }

    @Test
    public void testCopyConstructor_PreservesStructure() {
        ChunkyList<String> original = new ChunkyList<>(2);
        original.add("A");
        original.add("B");
        original.add("C");
        original.setCurrentGrowingStrategy(ChunkyList.GrowingStrategy.EXTEND_STRATEGY);
        original.setCurrentShrinkingStrategy(ChunkyList.ShrinkingStrategy.DISAPPEAR_STRATEGY);

        ChunkyList<String> copy = new ChunkyList<>(original);

        assertEquals(3, copy.size());
        assertEquals("A", copy.get(0));
        assertEquals("B", copy.get(1));
        assertEquals("C", copy.get(2));
        assertEquals(ChunkyList.GrowingStrategy.EXTEND_STRATEGY, copy.getCurrentGrowingStrategy());
        assertEquals(ChunkyList.ShrinkingStrategy.DISAPPEAR_STRATEGY, copy.getCurrentShrinkingStrategy());
    }

    @Test
    public void testCopyConstructor_WithNewChunkSize() {
        ChunkyList<String> original = new ChunkyList<>(4);
        original.add("A");
        original.add("B");
        original.add("C");
        original.add("D");
        original.add("E");

        ChunkyList<String> copy = new ChunkyList<>(2, original);

        assertEquals(5, copy.size());
        assertEquals(2, copy.getChunkSize());
        assertEquals("A", copy.get(0));
        assertEquals("E", copy.get(4));
    }

    @Test
    public void testConstructor_FromCollection() {
        List<String> original = Arrays.asList("A", "B", "C", "D", "E");
        ChunkyList<String> list = new ChunkyList<>(original);

        assertEquals(5, list.size());
        for (int i = 0; i < 5; i++) {
            assertEquals((char)('A' + i) + "", list.get(i));
        }
    }

    @Test
    public void testConstructor_FromCollection_WithCustomChunkSize() {
        List<String> original = Arrays.asList("A", "B", "C", "D", "E", "F", "G");
        ChunkyList<String> list = new ChunkyList<>(3, original);

        assertEquals(7, list.size());
        assertEquals(3, list.getChunkSize());
        for (int i = 0; i < 7; i++) {
            assertEquals((char)('A' + i) + "", list.get(i));
        }
    }

    // ======================
    // Tests pour reorganize()
    // ======================

    @Test
    public void testReorganize_PartiallyFilledChunks() {
        ChunkyList<String> list = new ChunkyList<>(2);
        list.add("A");
        list.add("B");
        list.add("C");

        int chunksBefore = countChunks(list);
        list.reorganize();
        int chunksAfter = countChunks(list);

        assertEquals(3, list.size());
        assertEquals("A", list.get(0));
        assertEquals("B", list.get(1));
        assertEquals("C", list.get(2));
        // Dans ce cas, le nombre de chunks ne change pas
        assertEquals(chunksBefore, chunksAfter);
    }

    @Test
    public void testReorganize_WithSparseChunks() {
        ChunkyList<String> list = new ChunkyList<>(3);
        for (int i = 0; i < 10; i++) {
            list.add("Item " + i);
        }
        // Crée des trous en supprimant des éléments
        list.remove(5);
        list.remove(3);
        list.remove(1);

        int chunksBefore = countChunks(list);
        list.reorganize();
        int chunksAfter = countChunks(list);

        assertEquals(7, list.size());
        assertEquals("Item 0", list.get(0));
        assertEquals("Item 2", list.get(1));
        assertEquals("Item 4", list.get(2));
        // Après réorganisation, le nombre de chunks devrait être optimisé
        assertTrue(chunksAfter <= chunksBefore);
    }

    @Test
    public void testReorganize_EmptyList() {
        ChunkyList<String> list = new ChunkyList<>(10);
        list.reorganize();
        assertEquals(0, list.size());
        assertEquals(0, countChunks(list));
    }

    @Test
    public void testReorganize_PreservesOrder() {
        ChunkyList<String> list = new ChunkyList<>(3);
        for (int i = 0; i < 20; i++) {
            list.add("Item " + i);
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

    // ======================
    // Tests des fonctionnalités de base
    // ======================

    @Test
    public void testAddAndGet() {
        ChunkyList<String> list = new ChunkyList<>(2);
        list.add("A");
        list.add("B");
        list.add("C");

        assertEquals(3, list.size());
        assertEquals("A", list.get(0));
        assertEquals("B", list.get(1));
        assertEquals("C", list.get(2));
    }

    @Test
    public void testRemoveByIndex() {
        ChunkyList<String> list = new ChunkyList<>(2);
        list.add("A");
        list.add("B");
        list.add("C");

        assertEquals("B", list.remove(1));
        assertEquals(2, list.size());
        assertEquals("A", list.get(0));
        assertEquals("C", list.get(1));
    }

    @Test
    public void testRemoveByObject() {
        ChunkyList<String> list = new ChunkyList<>(2);
        list.add("A");
        list.add("B");
        assertTrue(list.remove("A"));
        assertEquals(1, list.size());
        assertFalse(list.contains("A"));
    }

    @Test
    public void testSet() {
        ChunkyList<String> list = new ChunkyList<>(2);
        list.add("A");
        list.add("B");

        assertEquals("A", list.set(0, "X"));
        assertEquals("X", list.get(0));
        assertEquals("B", list.get(1));
    }

    @Test
    public void testIndexOf() {
        ChunkyList<String> list = new ChunkyList<>(2);
        list.add("A");
        list.add("B");
        list.add("A");

        assertEquals(0, list.indexOf("A"));
        assertEquals(2, list.lastIndexOf("A"));
        assertEquals(1, list.indexOf("B"));
        assertEquals(-1, list.indexOf("C"));
    }

    @Test
    public void testClear() {
        ChunkyList<String> list = new ChunkyList<>(2);
        list.add("A");
        list.add("B");
        list.clear();

        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
        assertEquals(0, countChunks(list));
    }

    @Test
    public void testContains() {
        ChunkyList<String> list = new ChunkyList<>(2);
        list.add("A");
        list.add("B");

        assertTrue(list.contains("A"));
        assertTrue(list.contains("B"));
        assertFalse(list.contains("C"));
    }

    // ======================
    // Tests des stratégies
    // ======================

    @Test
    public void testGrowingStrategy_Extend() {
        ChunkyList<String> list = new ChunkyList<>(2);
        list.setCurrentGrowingStrategy(ChunkyList.GrowingStrategy.EXTEND_STRATEGY);
        list.add("A");
        list.add("B");
        list.add("C");

        assertEquals(3, list.size());
        assertEquals("A", list.get(0));
        assertEquals("B", list.get(1));
        assertEquals("C", list.get(2));
        assertEquals(2, countChunks(list)); // 2 chunks: [A,B], [C]
    }

    @Test
    public void testGrowingStrategy_Overflow() {
        ChunkyList<String> list = new ChunkyList<>(2);
        list.setCurrentGrowingStrategy(ChunkyList.GrowingStrategy.OVERFLOW_STRATEGY);
        list.add("A");
        list.add("B");
        list.add("C");

        assertEquals(3, list.size());
        assertEquals("A", list.get(0));
        assertEquals("B", list.get(1));
        assertEquals("C", list.get(2));
        assertEquals(2, countChunks(list)); // 2 chunks: [A,B], [C]
    }

    @Test
    public void testShrinkingStrategy_Underflow() {
        ChunkyList<String> list = new ChunkyList<>(2);
        list.setCurrentShrinkingStrategy(ChunkyList.ShrinkingStrategy.UNDERFLOW_STRATEGY);
        list.add("A");
        list.add("B");
        list.add("C");
        list.remove(0);

        assertEquals(2, list.size());
        assertEquals("B", list.get(0));
        assertEquals("C", list.get(1));
        assertEquals(1, countChunks(list)); // 1 chunk: [B,C]
    }

    @Test
    public void testShrinkingStrategy_Disappear() {
        ChunkyList<String> list = new ChunkyList<>(2);
        list.setCurrentShrinkingStrategy(ChunkyList.ShrinkingStrategy.DISAPPEAR_STRATEGY);
        list.add("A");
        list.add("B");
        list.add("C");
        list.remove(0);
        list.remove(0);

        assertEquals(1, list.size());
        assertEquals("C", list.get(0));
        assertEquals(1, countChunks(list)); // 1 chunk: [C]
    }

    // ======================
    // Tests pour Spliterator et Streams
    // ======================

    @Test
    public void testSpliterator_Characteristics() {
        ChunkyList<String> list = new ChunkyList<>(2);
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
        ChunkyList<String> list = new ChunkyList<>(2);
        list.add("A");
        list.add("B");
        list.add("C");

        Spliterator<String> spliterator = list.spliterator();
        List<String> elements = new ArrayList<>();

        while (spliterator.tryAdvance(elements::add));

        assertEquals(Arrays.asList("A", "B", "C"), elements);
    }

    @Test
    public void testStream_Sequential() {
        ChunkyList<String> list = new ChunkyList<>(2);
        list.add("A");
        list.add("B");
        list.add("C");

        List<String> result = list.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testStream_Parallel() {
        ChunkyList<Integer> list = new ChunkyList<>(10);
        for (int i = 0; i < 100; i++) {
            list.add(i);
        }

        List<Integer> result = list.parallelStream()
                .filter(i -> i % 2 == 0)
                .collect(Collectors.toList());

        assertEquals(50, result.size());
        assertTrue(result.stream().allMatch(i -> i % 2 == 0));
    }

    // ======================
    // Méthodes utilitaires pour les tests
    // ======================

    /**
     * Compte le nombre de chunks dans la liste (pour les tests internes).
     */
    private int countChunks(ChunkyList<?> list) {
        try {
            Field firstChunkField = ChunkyList.class.getDeclaredField("firstChunk");
            firstChunkField.setAccessible(true);
            Object firstChunk = firstChunkField.get(list);

            if (firstChunk == null) return 0;

            int count = 0;
            Object current = firstChunk;
            while (current != null) {
                count++;
                Field nextChunkField = current.getClass().getDeclaredField("nextChunk");
                nextChunkField.setAccessible(true);
                current = nextChunkField.get(current);
            }
            return count;
        } catch (Exception e) {
            throw new RuntimeException("Impossible de compter les chunks", e);
        }
    }
}
