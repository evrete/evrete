package org.evrete.collections;


import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class AbstractLinearHashSet<K> extends AbstractLinearHash<K> {

    AbstractLinearHashSet(int minimumCapacity) {
        super(minimumCapacity);
    }

    public final boolean contains(K element) {
        return super.containsEntry(element);
    }

    public final boolean remove(K element) {
        return removeEntry(element);
    }

    public final void delete(Predicate<K> predicate) {
        super.deleteEntries(predicate);
    }

    public final void forEach(Consumer<K> consumer) {
        super.forEachDataEntry(consumer);
    }

}
