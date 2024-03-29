package org.evrete.collections;


import java.util.function.Predicate;

public abstract class AbstractLinearHashSet<K> extends AbstractLinearHash<K> {


    public final void delete(Predicate<K> predicate) {
        super.deleteEntries(predicate);
    }

}
