package org.evrete.api;

import org.evrete.api.annotations.NonNull;

import java.util.function.Function;

public interface ReIterable<T> extends Iterable<T> {

    @Override
    @NonNull
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
            public void remove() {
                it.remove();
            }

            @Override
            public Z next() {
                return mapper.apply(it.next());
            }

            @Override
            public String toString() {
                return it.toString();
            }
        };
    }
}
