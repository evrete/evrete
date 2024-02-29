package org.evrete.spi.minimal;

import org.evrete.api.FactHandle;
import org.evrete.api.FactStorage;
import org.evrete.api.ReIterator;
import org.evrete.api.Type;
import org.evrete.api.annotations.NonNull;
import org.evrete.collections.AbstractLinearHash;

import java.util.StringJoiner;
import java.util.function.BiPredicate;
import java.util.function.Function;

class DefaultFactStorage<T> implements FactStorage<T> {
    private final TupleCollection<T> collection;
    private final Function<Tuple<T>, FactStorage.Entry<T>> ITERATOR_MAPPER = t -> t;

    DefaultFactStorage(Type<?> type, BiPredicate<T, T> identityFunction, int minCapacity) {
        this.collection = new TupleCollection<>(minCapacity, type, identityFunction);
    }

    @Override
    public FactHandle insert(T fact) {
        return collection.insert(fact);
    }

    @Override
    public void delete(FactHandle handle) {
        this.collection.delete(handle);
    }

    @Override
    public void update(FactHandle handle, T newInstance) {
        this.collection.add(new Tuple<>((FactHandleImpl) handle, newInstance));
    }

    @Override
    public T getFact(FactHandle handle) {
        return this.collection.getFact((FactHandleImpl) handle);
    }

    @Override
    public void clear() {
        this.collection.clear();
    }

    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner("\n");
        collection.forEachDataEntry(t -> sj.add(t.toString()));
        return sj.toString();
    }

    @NonNull
    @Override
    public ReIterator<Entry<T>> iterator() {
        return collection.iterator(ITERATOR_MAPPER);
    }

    static class TupleCollection<T> extends AbstractLinearHash<DefaultFactStorage.Tuple<T>> {
        private final BiPredicate<T, T> identityFunction;
        private final BiPredicate<Tuple<T>, Tuple<T>> equalsPredicate;
        private final BiPredicate<Tuple<T>, FactHandleImpl> searchByHandle = (tuple, o) -> tuple.handle.id == o.id;
        private final BiPredicate<Tuple<T>, T> searchByFact = new BiPredicate<Tuple<T>, T>() {
            @Override
            public boolean test(Tuple<T> tuple, T o) {
                return identityFunction.test(tuple.object, o);
            }
        };
        private final Type<?> type;
        private long handleId = 0L;

        TupleCollection(int minCapacity, Type<?> type, BiPredicate<T, T> identityFunction) {
            super(minCapacity);
            this.identityFunction = identityFunction;
            this.equalsPredicate = (t1, t2) -> identityFunction.test(t1.object, t2.object);
            this.type = type;
        }

        void add(Tuple<T> tuple) {
            this.add(tuple, equalsPredicate, tuple);
        }

        FactHandleImpl insert(T fact) {
            Tuple<T> tuple = insertIfAbsent(fact, searchByFact, (hash, t) -> {
                FactHandleImpl handle = new FactHandleImpl(handleId++, hash, type.getId());
                return new Tuple<>(handle, fact);
            });
            return tuple == null ? null : tuple.handle;
        }

        void delete(FactHandle handle) {
            FactHandleImpl impl = (FactHandleImpl) handle;
            remove(impl, searchByHandle);
        }

        T getFact(FactHandleImpl impl) {
            Tuple<T> t = get(impl, searchByHandle);
            return t == null ? null : t.object;
        }
    }

    static class Tuple<Z> implements FactStorage.Entry<Z> {
        private final FactHandleImpl handle;
        private final Z object;

        Tuple(FactHandleImpl handle, Z object) {
            this.handle = handle;
            this.object = object;
        }

        @Override
        public FactHandle getHandle() {
            return handle;
        }

        @Override
        public Z getInstance() {
            return object;
        }

        @Override
        public int hashCode() {
            return handle.hash;
        }

        @Override
        public String toString() {
            return handle + " -> " + object;
        }

    }

}
