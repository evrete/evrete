package org.evrete.runtime;

public interface NestedFactRunnable {
    void forEachFact();

    /**
     * Sets the nested runnable which will be called for each fact.
     * We provide a default implementation so that the interface could
     * be used as a method reference (functional interface)
     * @param delegate nested runnable
     */
    default void setDelegate(NestedFactRunnable delegate) {
        throw new UnsupportedOperationException();
    }
}
