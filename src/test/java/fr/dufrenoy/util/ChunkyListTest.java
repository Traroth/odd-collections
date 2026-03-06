package fr.dufrenoy.util;

import static fr.dufrenoy.util.ChunkyList.GrowingStrategy;
import static fr.dufrenoy.util.ChunkyList.ShrinkingStrategy;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

/**
 * Tests du contrat de l'interface ChunkyList.
 * Utilise MockChunkyList pour vérifier que l'interface est bien définie.
 */
public class ChunkyListTest {

    // ===== Tests des constructeurs =====
    @Test
    public void testDefaultConstructor() {
        ChunkyList<String> list = new MockChunkyList<>(100);
        assertEquals(100, list.getChunkSize());
        assertEquals(GrowingStrategy.OVERFLOW_STRATEGY, list.getCurrentGrowingStrategy());
        assertEquals(ShrinkingStrategy.UNDERFLOW_STRATEGY, list.getCurrentShrinkingStrategy());
    }

    @Test
    public void testCustomChunkSize() {
        ChunkyList<String> list = new MockChunkyList<>(5);
        assertEquals(5, list.getChunkSize());
    }

    @Test
    public void testInvalidChunkSize() {
        assertThrows(IllegalArgumentException.class, () -> new MockChunkyList<>(0));
    }

    // ===== Tests des opérations de base =====
    @Test
    public void testAddAndGet() {
        ChunkyList<String> list = new MockChunkyList<>(2);
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
        ChunkyList<String> list = new MockChunkyList<>(2);
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
        ChunkyList<String> list = new MockChunkyList<>(2);
        list.add("A");
        list.add("B");
        assertTrue(list.remove("A"));
        assertEquals(1, list.size());
        assertFalse(list.contains("A"));
    }

    @Test
    public void testSet() {
        ChunkyList<String> list = new MockChunkyList<>(2);
        list.add("A");
        list.add("B");
        assertEquals("A", list.set(0, "X"));
        assertEquals("X", list.get(0));
        assertEquals("B", list.get(1));
    }

    @Test
    public void testContains() {
        ChunkyList<String> list = new MockChunkyList<>(2);
        list.add("A");
        list.add("B");
        assertTrue(list.contains("A"));
        assertTrue(list.contains("B"));
        assertFalse(list.contains("C"));
    }

    @Test
    public void testClear() {
        ChunkyList<String> list = new MockChunkyList<>(2);
        list.add("A");
        list.add("B");
        list.clear();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    // ===== Tests des stratégies =====
    @Test
    public void testSetGrowingStrategy() {
        ChunkyList<String> list = new MockChunkyList<>(2);
        list.setCurrentGrowingStrategy(GrowingStrategy.EXTEND_STRATEGY);
        assertEquals(GrowingStrategy.EXTEND_STRATEGY, list.getCurrentGrowingStrategy());
    }

    @Test
    public void testSetShrinkingStrategy() {
        ChunkyList<String> list = new MockChunkyList<>(2);
        list.setCurrentShrinkingStrategy(ShrinkingStrategy.DISAPPEAR_STRATEGY);
        assertEquals(ShrinkingStrategy.DISAPPEAR_STRATEGY, list.getCurrentShrinkingStrategy());
    }

    @Test
    public void testSetStrategies() {
        ChunkyList<String> list = new MockChunkyList<>(2);
        list.setStrategies(GrowingStrategy.EXTEND_STRATEGY, ShrinkingStrategy.DISAPPEAR_STRATEGY);
        assertEquals(GrowingStrategy.EXTEND_STRATEGY, list.getCurrentGrowingStrategy());
        assertEquals(ShrinkingStrategy.DISAPPEAR_STRATEGY, list.getCurrentShrinkingStrategy());
    }

    // ===== Tests des exceptions =====
    @Test
    public void testGet_IndexOutOfBounds() {
        ChunkyList<String> list = new MockChunkyList<>();
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(0));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
    }

    @Test
    public void testSet_IndexOutOfBounds() {
        ChunkyList<String> list = new MockChunkyList<>();
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(0, "A"));
    }

    @Test
    public void testRemove_IndexOutOfBounds() {
        ChunkyList<String> list = new MockChunkyList<>();
        assertThrows(IndexOutOfBoundsException.class, () -> list.remove(0));
    }
}
