package org.evrete.util;

import org.evrete.api.WorkUnit;

/**
 * <p>
 *     This utility class associates an arbitrary object with a level of complexity
 * </p>
 * @param <T> object type
 */
public class WorkUnitObject<T> implements WorkUnit {
    private final T delegate;
    private final double complexity;

    public WorkUnitObject(T delegate, double complexity) {
        this.delegate = delegate;
        this.complexity = complexity;
    }

    public T getDelegate() {
        return delegate;
    }

    @Override
    public double getComplexity() {
        return complexity;
    }
}
