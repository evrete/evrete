package org.evrete.util;

import java.util.Set;

public class Node<T extends Node<?>> {
    private static final int DEFAULT_ID = -1;
    private final int index;
    private int sourceIndex = DEFAULT_ID;

    protected Node(NextIntSupplier idSupplier, Set<? extends T> sourceNodes) {
        this(idSupplier);

        int sourceIndex = 0;
        for (T source : sourceNodes) {
            source.setSourceIndex(sourceIndex);
            sourceIndex++;
        }
    }

    protected Node(NextIntSupplier idSupplier) {
        this.index = idSupplier.next();
    }

    protected final int getIndex() {
        return index;
    }

    public int getSourceIndex() {
        return sourceIndex;
    }

    final void setSourceIndex(int index) {
        if (this.sourceIndex == DEFAULT_ID) {
            this.sourceIndex = index;
        } else {
            throw new IllegalStateException(this.toString());
        }
    }
}
