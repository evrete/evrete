package org.evrete.api.spi;

import org.evrete.util.EnumCombinationIterator;
import org.evrete.util.FilteringIterator;

import java.util.Iterator;
import java.util.function.Predicate;

public enum MemoryScope {
    /**
     * Identifies main memory
     */
    MAIN,
    /**
     * Identifies delta memory
     */
    DELTA;

    private static boolean containsDelta(MemoryScope[] scopes) {
        for (MemoryScope scope : scopes) {
            if (scope == DELTA) return true;
        }
        return false;
    }

    /**
     * Returns all state combinations depending on the provided scope. An array of {@link MemoryScope}
     * is considered as a delta state if at least one value inside the array is {@link #DELTA}
     *
     * @param sharedResult shared array for the iteration results
     * @see #containsDelta(MemoryScope[])
     */
    public static Iterator<MemoryScope[]> states(MemoryScope scope, MemoryScope[] sharedResult) {
        Iterator<MemoryScope[]> scopesIterator = new EnumCombinationIterator<>(MemoryScope.class, sharedResult);
        Predicate<MemoryScope[]> deltaPredicate = MemoryScope::containsDelta;

        switch (scope) {
            case MAIN:
                return new FilteringIterator<>(scopesIterator, deltaPredicate.negate());
            case DELTA:
                return new FilteringIterator<>(scopesIterator, deltaPredicate);
            default:
                throw new IllegalArgumentException("Unknown scope " + scope);
        }
    }

}
