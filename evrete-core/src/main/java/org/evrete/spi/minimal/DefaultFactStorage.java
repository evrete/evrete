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
    private final Function<FactTuple<T>, FactStorage.Entry<T>> ITERATOR_MAPPER = t -> t;

    DefaultFactStorage(Type<?> type, BiPredicate<T, T> identityFunction) {
        this.collection = new TupleCollection<>(type, identityFunction);
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
        this.collection.add(new FactTuple<>((FactHandleImpl) handle, newInstance));
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

    static class TupleCollection<T> extends AbstractLinearHash<FactTuple<T>> {
        private final BiPredicate<T, T> identityFunction;
        private final BiPredicate<FactTuple<T>, FactTuple<T>> equalsPredicate;
        private final BiPredicate<FactTuple<T>, FactHandleImpl> searchByHandle = (factTuple, o) -> factTuple.handle.id == o.id;
        private final BiPredicate<FactTuple<T>, T> searchByFact = new BiPredicate<FactTuple<T>, T>() {
            @Override
            public boolean test(FactTuple<T> factTuple, T o) {
                return identityFunction.test(factTuple.object, o);
            }
        };
        private final Type<?> type;
        private long handleId = 0L;

        TupleCollection(Type<?> type, BiPredicate<T, T> identityFunction) {
            super();
            this.identityFunction = identityFunction;
            this.equalsPredicate = (t1, t2) -> identityFunction.test(t1.object, t2.object);
            this.type = type;
        }

        void add(FactTuple<T> factTuple) {
            this.add(factTuple, equalsPredicate, factTuple);
        }

        FactHandleImpl insert(T fact) {
            FactTuple<T> factTuple = insertIfAbsent(fact, searchByFact, (hash, t) -> {
                FactHandleImpl handle = new FactHandleImpl(handleId++, hash, type.getId());
                return new FactTuple<>(handle, fact);
            });
            return factTuple == null ? null : factTuple.handle;
        }

        void delete(FactHandle handle) {
            FactHandleImpl impl = (FactHandleImpl) handle;
            remove(impl, searchByHandle);
        }

        T getFact(FactHandleImpl impl) {
            FactTuple<T> t = get(impl, searchByHandle);
            return t == null ? null : t.object;
        }
    }

    static class FactTuple<Z> implements FactStorage.Entry<Z> {
        private final FactHandleImpl handle;
        private final Z object;

        FactTuple(FactHandleImpl handle, Z object) {
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
