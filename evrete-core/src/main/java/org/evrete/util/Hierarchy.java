package org.evrete.util;

import org.evrete.api.annotations.Nullable;

import java.util.function.Consumer;

/**
 * The Hierarchy class represents a hierarchical structure of elements.
 *
 * @param <T> the type of the value stored in the hierarchy nodes
 */
public class Hierarchy<T> {
    private final Hierarchy<T> parent;
    private final T value;

    /**
     * Constructs a Hierarchy with a specified value and an optional parent.
     *
     * @param value the value of the current node
     * @param parent the parent node, can be null
     */
    public Hierarchy(T value, @Nullable Hierarchy<T> parent) {
        this.value = value;
        this.parent = parent;
    }

    /**
     * Constructs a Hierarchy root node.
     *
     * @param value the value of the current node
     */
    public Hierarchy(T value) {
        this(value, null);
    }

    /**
     * Returns the value of the current node.
     *
     * @return the value of the current node
     */
    public T getValue() {
        return value;
    }

    /**
     * Walks up the hierarchy, starting from the current node and applying the given consumer
     * to each node's value.
     *
     * @param consumer a Consumer to apply to each node's value
     */
    public final void walkUp(Consumer<T> consumer) {
        consumer.accept(this.value);
        if (parent != null) {
            parent.walkUp(consumer);
        }
    }
}
