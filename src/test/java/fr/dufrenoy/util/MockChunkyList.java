package fr.dufrenoy.util;

import java.util.*;
import static fr.dufrenoy.util.ChunkyList.GrowingStrategy;
import static fr.dufrenoy.util.ChunkyList.ShrinkingStrategy;

/**
 * Implémentation minimale de ChunkyList pour les tests de contrat d'interface.
 * Cette classe simule le comportement attendu sans implémenter la logique réelle des chunks.
 */
final class MockChunkyList<E> implements ChunkyList<E> {
    private static final int DEFAULT_CHUNK_SIZE = 100; // Même valeur que dans UnsynchronizedChunkyList

    private final List<E> delegate = new ArrayList<>();
    private final int chunkSize;
    private GrowingStrategy growingStrategy = GrowingStrategy.OVERFLOW_STRATEGY;
    private ShrinkingStrategy shrinkingStrategy = ShrinkingStrategy.UNDERFLOW_STRATEGY;

    // Constructeur par défaut (sans argument)
    public MockChunkyList() {
        this(DEFAULT_CHUNK_SIZE);
    }

    // Constructeur avec chunkSize
    public MockChunkyList(int chunkSize) {
        if (chunkSize < 1) throw new IllegalArgumentException("chunkSize must be at least 1");
        this.chunkSize = chunkSize;
    }

    // === Méthodes de ChunkyList ===
    @Override public int getChunkSize() { return chunkSize; }

    @Override public GrowingStrategy getCurrentGrowingStrategy() { return growingStrategy; }
    @Override public void setCurrentGrowingStrategy(GrowingStrategy strategy) { this.growingStrategy = strategy; }

    @Override public ShrinkingStrategy getCurrentShrinkingStrategy() { return shrinkingStrategy; }
    @Override public void setCurrentShrinkingStrategy(ShrinkingStrategy strategy) { this.shrinkingStrategy = strategy; }

    @Override public void setStrategies(GrowingStrategy growing, ShrinkingStrategy shrinking) {
        this.growingStrategy = growing;
        this.shrinkingStrategy = shrinking;
    }

    @Override public void reorganize() {
        // Simule une réorganisation sans rien faire (pour les tests)
    }

    // === Méthodes de List (délégation à ArrayList) ===
    @Override public int size() { return delegate.size(); }
    @Override public boolean isEmpty() { return delegate.isEmpty(); }
    @Override public boolean contains(Object o) { return delegate.contains(o); }
    @Override public Iterator<E> iterator() { return delegate.iterator(); }
    @Override public Object[] toArray() { return delegate.toArray(); }
    @Override public <T> T[] toArray(T[] a) { return delegate.toArray(a); }
    @Override public boolean add(E e) { return delegate.add(e); }
    @Override public boolean remove(Object o) { return delegate.remove(o); }
    @Override public boolean containsAll(Collection<?> c) { return delegate.containsAll(c); }
    @Override public boolean addAll(Collection<? extends E> c) { return delegate.addAll(c); }
    @Override public boolean addAll(int index, Collection<? extends E> c) { return delegate.addAll(index, c); }
    @Override public boolean removeAll(Collection<?> c) { return delegate.removeAll(c); }
    @Override public boolean retainAll(Collection<?> c) { return delegate.retainAll(c); }
    @Override public void clear() { delegate.clear(); }
    @Override public E get(int index) { return delegate.get(index); }
    @Override public E set(int index, E element) { return delegate.set(index, element); }
    @Override public void add(int index, E element) { delegate.add(index, element); }
    @Override public E remove(int index) { return delegate.remove(index); }
    @Override public int indexOf(Object o) { return delegate.indexOf(o); }
    @Override public int lastIndexOf(Object o) { return delegate.lastIndexOf(o); }
    @Override public ListIterator<E> listIterator() { return delegate.listIterator(); }
    @Override public ListIterator<E> listIterator(int index) { return delegate.listIterator(index); }
    @Override public List<E> subList(int fromIndex, int toIndex) { return delegate.subList(fromIndex, toIndex); }

    @Override
    public Spliterator<E> spliterator() {
        return delegate.spliterator();
    }
}
