package org.evrete.api;

import java.util.function.Function;

public interface ReIterable<T> {
    ReIterator<T> iterator();

    default <Z> ReIterator<Z> iterator(Function<? super T, Z> mapper) {
        final ReIterator<T> it = iterator();
        return new ReIterator<Z>() {
            @Override
            public long reset() {
                return it.reset();
            }

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public Z next() {
                return mapper.apply(it.next());
            }
        };
    }
}
