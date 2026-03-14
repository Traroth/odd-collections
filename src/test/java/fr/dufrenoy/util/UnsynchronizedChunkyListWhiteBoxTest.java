package fr.dufrenoy.util;

import static fr.dufrenoy.util.ChunkyList.GrowingStrategy;
import static fr.dufrenoy.util.ChunkyList.ShrinkingStrategy;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

/**
 * Tests WHITE BOX de l'implémentation UnsynchronizedChunkyList.
 * Ces tests accèdent aux détails internes pour vérifier la structure des chunks.
 */
public class UnsynchronizedChunkyListWhiteBoxTest {

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
}
