package org.evrete.spi.minimal;

import org.evrete.api.annotations.NonNull;
import org.evrete.api.spi.GroupingReteMemory;
import org.evrete.api.spi.MemoryScope;

import java.util.*;
import java.util.stream.Stream;

public class DefaultGroupingReteMemory<V> implements GroupingReteMemory<V> {
    private MemoryImpl<V> main = new MemoryImpl<>();
    private MemoryImpl<V> delta = new MemoryImpl<>();

    MemoryImpl<V> getMain() {
        return main;
    }

    MemoryImpl<V> getDelta() {
        return delta;
    }

    @Override
    public void insert(long key, @NonNull V value) {
        V v = Objects.requireNonNull(value);
        delta.insert(key, v);
    }

    @Override
    public void delete(long key, @NonNull V value) {
        main.delete(key, value);
        delta.delete(key, value);
    }

    @Override
    public void commit() {
        if(!delta.isEmpty()) {
            // We create new instances as Java Hash collections don't shrink and their scan performance
            // degrades over time
            this.main = main.copy();
            this.delta.forEach((key, value) -> {
                // Main memory's keys are always have the MAIN scope
                main.computeIfAbsent(
                        key,
                        o -> new ValueCollection<>()
                ).addAll(value);
            });

            // Clearing the delta memory
            this.delta = new MemoryImpl<>();
        }
    }

    @Override
    public Iterator<Long> iterator(MemoryScope scope) {
        switch (scope) {
            case MAIN:
                return main.keySet().iterator();
            case DELTA:
                return delta.keySet().iterator();
            default:
                throw new IllegalStateException("Unknown scope " + scope);
        }
    }

    @Override
    public Iterator<V> valueIterator(MemoryScope scope, long key) {
        switch (scope) {
            case MAIN:
                return main.values(key);
            case DELTA:
                return delta.values(key);
            default:
                throw new IllegalStateException("Unknown scope " + scope);
        }
    }

    @Override
    public Stream<Long> stream(MemoryScope scope) {
        switch (scope) {
            case MAIN:
                return main.keySet().stream();
            case DELTA:
                return delta.keySet().stream();
            default:
                throw new IllegalStateException("Unknown scope " + scope);
        }
    }

    @Override
    public Stream<V> stream(MemoryScope scope, long key) {
        switch (scope) {
            case MAIN:
                return main.stream(key);
            case DELTA:
                return delta.stream(key);
            default:
                throw new IllegalStateException("Unknown scope " + scope);
        }
    }

    @Override
    public void clear() {
        this.main.clear();
        this.delta.clear();
    }

    @Override
    public String toString() {
        return "{main=" + main.size() +
                ", delta=" + delta.size() +
                '}';
    }

    static class MemoryImpl<V> extends HashMap<Long, ValueCollection<V>> {

        MemoryImpl() {
        }

        private MemoryImpl(MemoryImpl<V> m) {
            super(m);
        }

        synchronized void insert(long key, V value) {
            computeIfAbsent(
                    key,
                    k -> new ValueCollection<>()
            ).add(value);
        }

        synchronized void delete(long key, V value) {
            ValueCollection<V> v = get(key);
            if(v != null) {
                v.remove(value);
                if(v.isEmpty()) {
                    remove(key);
                }
            }
        }

        synchronized MemoryImpl<V> copy() {
            return new MemoryImpl<>(this);
        }

        Iterator<V> values(Object key) {
            ValueCollection<V> collection = get(key);
            if (collection == null || collection.isEmpty()) {
                return Collections.emptyIterator();
            } else {
                return collection.iterator();
            }
        }

        Stream<V> stream(Object key) {
            ValueCollection<V> collection = get(key);
            if (collection == null || collection.isEmpty()) {
                return Stream.empty();
            } else {
                return collection.stream();
            }
        }
    }


    static class ValueCollection<V> {
        private final LinkedHashSet<V> delegate;

        ValueCollection(LinkedHashSet<V> delegate) {
            this.delegate = delegate;
        }

        void addAll(ValueCollection<V> other) {
            if (other != null) {
                this.delegate.addAll(other.delegate);
            }
        }

        void remove(V value) {
            this.delegate.remove(value);
        }

        public Stream<V> stream() {
            return delegate.stream();
        }

        public Iterator<V> iterator() {
            return delegate.iterator();
        }

        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        void add(V value) {
            this.delegate.add(value);
        }

        ValueCollection() {
            this(new LinkedHashSet<>());
        }

        @Override
        public String toString() {
            return delegate.toString();
        }
    }
}
