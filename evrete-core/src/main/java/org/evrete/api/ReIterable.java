package org.evrete.api;

import org.evrete.api.annotations.NonNull;
import org.evrete.util.MappingReIterator;

import java.util.function.Function;

/**
 * The ReIterable interface is essentially an {@link Iterable} that returns
 * a {@link ReIterator} instead of a {@link java.util.Iterator}.
 *
 * @param <T> the type of elements in the collection.
 */
public interface ReIterable<T> extends Iterable<T> {

    @Override
    @NonNull
    ReIterator<T> iterator();

    default <Z> ReIterator<Z> iterator(Function<? super T, Z> mapper) {
        return new MappingReIterator<>(iterator(), mapper);
    }
}
