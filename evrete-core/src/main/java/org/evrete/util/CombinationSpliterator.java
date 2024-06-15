package org.evrete.util;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * CombinationSpliterator is a simple spliterator that generates all possible combinations
 * of elements from an array of source objects.
 *
 * @param <T> the type of elements returned by this iterator
 * @param <S> the type of iterable sources containing elements of type T
 */
public class CombinationSpliterator<S, T> implements Spliterator<T[]> {
    private final S[] sources;
    private final Function<S, Iterator<T>> iteratorFunction;
    final T[] result;
    private final Iterator<T>[] iterators;
    private boolean hasOutput = true;


    @SuppressWarnings("unchecked")
    public CombinationSpliterator(S[] sources, Function<S, Iterator<T>> iteratorFunction, T[] result) {
        if (sources.length != result.length) {
            throw new IllegalArgumentException("The length of sources and result must be the same");
        }
        this.sources = sources;
        this.iteratorFunction = iteratorFunction;
        this.result = result;
        this.iterators = new Iterator[sources.length];

        // Initialize the first set of iterators and the result array
        for (int i = 0; i < sources.length; i++) {
            iterators[i] = iteratorFunction.apply(sources[i]);
            if (iterators[i].hasNext()) {
                result[i] = iterators[i].next();
            } else {
                hasOutput = false;
                break;
            }
        }
    }

    @Override
    public boolean tryAdvance(Consumer<? super T[]> action) {
        if (hasOutput) {
            action.accept(result);
            return advance();
        } else {
            return false;
        }
    }

    @Override
    public Spliterator<T[]> trySplit() {
        return null; // Does not support splitting
    }

    @Override
    public long estimateSize() {
        return Long.MAX_VALUE; // Undefined size
    }

    @Override
    public int characteristics() {
        return NONNULL | ORDERED;
    }

    private boolean advance() {
        for (int i = sources.length - 1; i >= 0; i--) {
            Iterator<T> it = iterators[i];
            if (it.hasNext()) {
                result[i] = it.next();
                return true;
            } else {
                iterators[i] = iteratorFunction.apply(sources[i]);
                result[i] = iterators[i].next();
            }
        }
        return false;
    }
}

