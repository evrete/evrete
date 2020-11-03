/*
package org.evrete.runtime.memory;

import org.evrete.api.Action;
import org.evrete.api.Type;
import org.evrete.api.TypeResolver;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;

public class BufferSafe extends Buffer {
    private final Lock writeLock;

    public BufferSafe() {
        super();
        ReentrantReadWriteLock l = new ReentrantReadWriteLock();
        writeLock = l.writeLock();
    }

    @Override
    public void add(TypeResolver resolver, Action action, Collection<?> objects) {
        try {
            writeLock.lock();
            super.add(resolver, action, objects);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void insert(TypeResolver resolver, String objectType, Collection<?> objects) {
        try {
            writeLock.lock();
            super.insert(resolver, objectType, objects);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    void takeAll(Action action, BiConsumer<Type<?>, Iterator<Object>> consumer) {
        try {
            writeLock.lock();
            super.takeAll(action, consumer);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void clear() {
        try {
            writeLock.lock();
            super.clear();
        } finally {
            writeLock.unlock();
        }
    }
}
*/
