package org.evrete.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

public class FilteringIterator<T> implements Iterator<T> {
    private final Iterator<T> iterator;
    private final Predicate<T> predicate;
    private T nextElement;
    private boolean nextElementSet = false;

    public FilteringIterator(Iterator<T> iterator, Predicate<T> predicate) {
        this.iterator = iterator;
        this.predicate = predicate;
    }

    @Override
    public boolean hasNext() {
        if (nextElementSet) {
            return true;
        } else {
            return setNextElement();
        }
    }

    @Override
    public T next() {
        if (!nextElementSet && !setNextElement()) {
            throw new NoSuchElementException();
        }
        nextElementSet = false;
        return nextElement;
    }

    private boolean setNextElement() {
        while (iterator.hasNext()) {
            T element = iterator.next();
            if (predicate.test(element)) {
                nextElement = element;
                nextElementSet = true;
                return true;
            }
        }
        return false;
    }
}
