package org.evrete.util;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

public class FlatMapIterator<T, Z> implements Iterator<Z> {
    private final Iterator<T> source;
    private final Function<T, Iterator<Z>> flatMapFunction;
    private Iterator<Z> current = Collections.emptyIterator();

    public FlatMapIterator(Iterator<T> source, Function<T, Iterator<Z>> flatMapFunction) {
        this.source = source;
        this.flatMapFunction = flatMapFunction;
    }

    @Override
    public boolean hasNext() {
        while (!current.hasNext() && source.hasNext()) {
            current = flatMapFunction.apply(source.next());
        }
        return current.hasNext();
    }

    @Override
    public Z next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return current.next();
    }
}
