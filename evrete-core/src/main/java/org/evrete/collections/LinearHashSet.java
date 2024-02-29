package org.evrete.collections;

import org.evrete.api.annotations.NonNull;

import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;

public class LinearHashSet<K> extends AbstractLinearHashSet<K> {
    private final BiPredicate<K, K> EQUALS = Objects::equals;

    public LinearHashSet(int minimalCapacity) {
        super(minimalCapacity);
    }

    public boolean contains(K e) {
        return contains(e, EQUALS);
    }

    public boolean remove(K e) {
        return remove(e, EQUALS);
    }

    /**
     * Adds an element to the collection.
     *
     * @param element the element to be added (must not be null)
     * @return true if this collection did not already contain the specified element
     */
    public final boolean add(@NonNull K element) {
        return this.replace(element) == null;
    }

    /**
     * Replaces the element in the collection with the specified element.
     *
     * @param element the element to be replaced
     * @return the replaced element, or null if the element was not found in the collection
     */
    public final K replace(K element) {
        return add(element, EQUALS, element);
    }

    public void addAll(AbstractLinearHash<K> source, BinaryOperator<K> combineFunction) {
        addAll(source, combineFunction, EQUALS);
    }
}
