package org.evrete.util;

import java.util.Iterator;
import java.util.function.Function;

/**
 * The {@code MappingReIterator} class implements the {@link Iterator} interface
 * and maps values using the provided functional interface.
 */
public class MappingIterator<T, Z> implements Iterator<Z> {
    private final Iterator<T> delegate;
    private final Function<? super T, Z> mapper;

    public MappingIterator(Iterator<T> delegate, Function<? super T, Z> mapper) {
        this.delegate = delegate;
        this.mapper = mapper;
    }

    @Override
    public boolean hasNext() {
        return delegate.hasNext();
    }

    @Override
    public void remove() {
        delegate.remove();
    }

    @Override
    public Z next() {
        return mapper.apply(delegate.next());
    }

    @Override
    public String toString() {
        return delegate.toString();
    }


}
