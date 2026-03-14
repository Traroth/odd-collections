package fr.dufrenoy.util;

import static fr.dufrenoy.util.ChunkyList.GrowingStrategy;
import static fr.dufrenoy.util.ChunkyList.ShrinkingStrategy;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

/**
 * Tests BLACK BOX de l'implémentation UnsynchronizedChunkyList.
 * Ces tests vérifient le comportement fonctionnel sans accéder aux détails internes.
 */
public class UnsynchronizedChunkyListBlackBoxTest {

    // ===== Tests de reorganize =====
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

    @Test
    public void testAdd_Null_Throws() {
        UnsynchronizedChunkyList<String> list = new UnsynchronizedChunkyList<>();
        assertThrows(IllegalArgumentException.class, () -> list.add(null));
    }

    @Test
    public void testAddAtIndex_Null_Throws() {
        UnsynchronizedChunkyList<String> list = new UnsynchronizedChunkyList<>();
        list.add("A");
        assertThrows(IllegalArgumentException.class, () -> list.add(0, null));
    }

    @Test
    public void testSet_Null_Throws() {
        UnsynchronizedChunkyList<String> list = new UnsynchronizedChunkyList<>();
        list.add("A");
        assertThrows(IllegalArgumentException.class, () -> list.set(0, null));
    }
}
