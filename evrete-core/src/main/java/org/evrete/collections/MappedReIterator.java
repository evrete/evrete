package org.evrete.collections;

import org.evrete.api.ReIterator;

import java.util.function.Function;

public class MappedReIterator<Z, V> implements ReIterator<Z> {
    private final ReIterator<V> delegate;
    private final Function<V, Z> mapper;

    public MappedReIterator(ReIterator<V> delegate, Function<V, Z> mapper) {
        this.delegate = delegate;
        this.mapper = mapper;
    }

    @Override
    public long reset() {
        return delegate.reset();
    }

    @Override
    public void remove() {
        delegate.remove();
    }

    @Override
    public boolean hasNext() {
        return delegate.hasNext();
    }

    @Override
    public Z next() {
        return mapper.apply(delegate.next());
    }
}
