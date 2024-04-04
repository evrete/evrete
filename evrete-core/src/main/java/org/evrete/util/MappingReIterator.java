package org.evrete.util;

import org.evrete.api.ReIterator;

import java.util.function.Function;

/**
 * The {@code MappingReIterator} class implements the {@link ReIterator} interface
 * and provides a mapping functionality to another {@link ReIterator}.
 */
public class MappingReIterator<T, Z> implements ReIterator<Z>{
    private final ReIterator<T> delegate;
    private final Function<? super T, Z> mapper;

    public MappingReIterator(ReIterator<T> delegate, Function<? super T, Z> mapper) {
        this.delegate = delegate;
        this.mapper = mapper;
    }

    @Override
    public long reset() {
        return delegate.reset();
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
