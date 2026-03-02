package fr.dufrenoy.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ChunkyListTest {

    // --- Tests de base ---
    @Test
    public void testConstructeurParDefaut() {
        ChunkyList<String> list = new ChunkyList<>();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void testAjoutEtTaille() {
        ChunkyList<String> list = new ChunkyList<>(2);
        assertTrue(list.add("A"));
        assertEquals(1, list.size());
        assertFalse(list.isEmpty());
    }

    @Test
    public void testAjoutAvecOverflow() {
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
    public void testStrategieExtend() {
        ChunkyList<String> list = new ChunkyList<>(2);
        list.setCurrentGrowingStrategy(ChunkyList.GrowingStrategy.EXTEND_STRATEGY);
        list.add("A");
        list.add("B");
        list.add("C");
        assertEquals(3, list.size());
    }

    @Test
    public void testStrategieOverflow() {
        ChunkyList<String> list = new ChunkyList<>(2);
        list.setCurrentGrowingStrategy(ChunkyList.GrowingStrategy.OVERFLOW_STRATEGY);
        list.add("A");
        list.add("B");
        list.add("C");
        assertEquals(3, list.size());
    }

    @Test
    public void testGetEtSet() {
        ChunkyList<String> list = new ChunkyList<>(2);
        list.add("A");
        list.add("B");
        assertEquals("A", list.get(0));
        assertEquals("B", list.set(1, "C"));
        assertEquals("C", list.get(1));
    }

    @Test
    public void testIndexOfEtLastIndexOf() {
        ChunkyList<String> list = new ChunkyList<>(2);
        list.add("A");
        list.add("B");
        list.add("A");
        assertEquals(0, list.indexOf("A"));
        assertEquals(2, list.lastIndexOf("A"));
    }

    @Test
    public void testRemoveParIndex() {
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
    public void testRemoveParObjet() {
        ChunkyList<String> list = new ChunkyList<>(2);
        list.add("A");
        list.add("B");
        assertTrue(list.remove("A"));
        assertEquals(1, list.size());
        assertFalse(list.contains("A"));
    }

    @Test
    public void testClear() {
        ChunkyList<String> list = new ChunkyList<>(2);
        list.add("A");
        list.add("B");
        list.clear();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void testStrategieDisappear() {
        ChunkyList<String> list = new ChunkyList<>(2);
        list.setCurrentShrinkingStrategy(ChunkyList.ShrinkingStrategy.DISAPPEAR_STRATEGY);
        list.add("A");
        list.add("B");
        list.remove(0);
        list.remove(0);
        assertEquals(0, list.size());
    }

    @Test
    public void testStrategieUnderflow() {
        ChunkyList<String> list = new ChunkyList<>(2);
        list.setCurrentShrinkingStrategy(ChunkyList.ShrinkingStrategy.UNDERFLOW_STRATEGY);
        list.add("A");
        list.add("B");
        list.add("C");
        list.remove(0);
        assertEquals(2, list.size());
        assertEquals("B", list.get(0));
        assertEquals("C", list.get(1));
    }

    @Test
    public void testExceptionIndexInvalide() {
        ChunkyList<String> list = new ChunkyList<>();
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(0));
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(0, "A"));
        assertThrows(IndexOutOfBoundsException.class, () -> list.remove(0));
    }

    // --- Tests pour Spliterator et Stream ---
    @Test
    public void testCustomSpliterator_Characteristics() {
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
    public void testCustomSpliterator_TryAdvance() {
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
    public void testCustomSpliterator_ForEachRemaining() {
        ChunkyList<String> list = new ChunkyList<>(2);
        list.add("A");
        list.add("B");
        list.add("C");

        Spliterator<String> spliterator = list.spliterator();
        List<String> elements = new ArrayList<>();

        spliterator.forEachRemaining(elements::add);

        assertEquals(Arrays.asList("A", "B", "C"), elements);
    }

    @Test
    public void testCustomSpliterator_TrySplit() {
        ChunkyList<Integer> list = new ChunkyList<>(3);
        for (int i = 0; i < 10; i++) {
            list.add(i);
        }

        Spliterator<Integer> spliterator = list.spliterator();
        Spliterator<Integer> split = spliterator.trySplit();

        assertNotNull(split);

        List<Integer> part1 = new ArrayList<>();
        List<Integer> part2 = new ArrayList<>();

        spliterator.forEachRemaining(part1::add);
        split.forEachRemaining(part2::add);

        List<Integer> combined = new ArrayList<>(part1);
        combined.addAll(part2);
        Collections.sort(combined);

        List<Integer> expected = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            expected.add(i);
        }

        assertEquals(expected, combined);
    }

    @Test
    public void testCustomSpliterator_EstimateSize() {
        ChunkyList<String> list = new ChunkyList<>(2);
        list.add("A");
        list.add("B");
        list.add("C");

        Spliterator<String> spliterator = list.spliterator();
        assertEquals(3, spliterator.estimateSize());
    }

    @Test
    public void testCustomSpliterator_EmptyList() {
        ChunkyList<String> list = new ChunkyList<>();
        Spliterator<String> spliterator = list.spliterator();

        assertEquals(0, spliterator.estimateSize());
        assertFalse(spliterator.tryAdvance(e -> {}));
    }

    @Test
    public void testCustomSpliterator_StreamSequential() {
        ChunkyList<String> list = new ChunkyList<>(2);
        list.add("A");
        list.add("B");
        list.add("C");

        List<String> result = StreamSupport.stream(list.spliterator(), false)
                .collect(Collectors.toList());

        assertEquals(Arrays.asList("A", "B", "C"), result);
    }

    @Test
    public void testCustomSpliterator_StreamParallel() {
        ChunkyList<Integer> list = new ChunkyList<>(10);
        for (int i = 0; i < 100; i++) {
            list.add(i);
        }

        List<Integer> result = StreamSupport.stream(list.spliterator(), true)
                .filter(i -> i % 2 == 0)
                .collect(Collectors.toList());

        assertEquals(50, result.size());
        assertTrue(result.stream().allMatch(i -> i % 2 == 0));
    }
}
