package org.evrete.collections;


import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class AbstractFastHashSet<K> extends AbstractHashData<K> {

    private static final int DEFAULT_INITIAL_SIZE = 16;

    AbstractFastHashSet(int initialCapacity) {
        super(initialCapacity);
    }

    AbstractFastHashSet() {
        this(DEFAULT_INITIAL_SIZE);
    }

    public final boolean contains(K element) {
        return super.containsEntry(element);
    }

    public final boolean remove(K element) {
        return removeEntry(element);
    }

    public final boolean delete(Predicate<K> predicate) {
        return super.deleteEntries(predicate);
    }

    public final void forEach(Consumer<K> consumer) {
        super.forEachDataEntry(consumer);
    }

}
