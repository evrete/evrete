package org.evrete.spi.minimal;

import org.evrete.api.FactHandle;
import org.evrete.api.FactStorage;
import org.evrete.api.RuntimeContext;
import org.evrete.api.Type;
import org.evrete.collections.AbstractLinearHash;

import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiPredicate;
import java.util.function.ToIntFunction;

//TODO !!!! optimize storage
class DefaultFactStorage<T> extends AbstractLinearHash<DefaultFactStorage.Tuple<T>> implements FactStorage<T> {
    private static final ToIntFunction<Object> HASH_FUNCTION = Object::hashCode;
    private final BiPredicate<T, T> identityFunction;
    private final AtomicLong handleId = new AtomicLong(0);
    private final Type<?> type;


    DefaultFactStorage(RuntimeContext<?> ctx, Type<?> type, BiPredicate<T, T> identityFunction) {
        this.type = type;
        this.identityFunction = identityFunction;
    }

    @Override
    protected ToIntFunction<Object> getHashFunction() {
        return HASH_FUNCTION;
    }

    @Override
    protected BiPredicate<Object, Object> getEqualsPredicate() {
        //TODO !!!! make static
        return new BiPredicate<Object, Object>() {
            @SuppressWarnings("unchecked")
            @Override
            public boolean test(Object o1, Object o2) {
                Tuple<T> t1 = (Tuple<T>) o1;
                Tuple<T> t2 = (Tuple<T>) o2;
                return identityFunction.test(t1.object, t2.object);
            }
        };
    }


    @Override
    public FactHandleImpl insert(T fact) {
        resize();
        int hash = HASH_FUNCTION.applyAsInt(fact);
        //TODO !!!! make static
        int addr = findBinIndex(fact, hash, new BiPredicate<Tuple<T>, T>() {
            @Override
            public boolean test(Tuple<T> tuple, T o) {
                return identityFunction.test(tuple.object, o);
            }
        });
        Tuple<T> tuple = get(addr);
        if (tuple == null) {
            // Object id unknown, creating new handle...
            FactHandleImpl handle = new FactHandleImpl(handleId.getAndIncrement(), hash, this.type.getId());
            tuple = new Tuple<>(handle, fact);
            saveDirect(tuple, addr);
            return handle;
        } else {
            return null;
        }
    }

    @Override
    public void update(FactHandle handle, T newInstance) {
        FactHandleImpl impl = (FactHandleImpl) handle;
        super.add(new Tuple<>(impl, newInstance));
    }

    @Override
    public void delete(FactHandle handle) {
        Objects.requireNonNull(handle);
        FactHandleImpl impl = (FactHandleImpl) handle;
        //TODO !!!! make static
        int addr = findBinIndex(impl, impl.hash, new BiPredicate<Tuple<T>, FactHandleImpl>() {
            @Override
            public boolean test(Tuple tuple, FactHandleImpl o) {
                return tuple.handle.id == o.id;
            }
        });
        if (get(addr) != null) {
            markDeleted(addr);
        }
    }

    @Override
    public Iterator<T> it() {
        return iterator(t -> t.object);
    }

    @Override
    public T getFact(FactHandle handle) {
        Objects.requireNonNull(handle);
        FactHandleImpl impl = (FactHandleImpl) handle;
        int addr = findBinIndex(impl, impl.hash, new BiPredicate<Tuple<T>, FactHandleImpl>() {
            @Override
            public boolean test(Tuple tuple, FactHandleImpl o) {
                return tuple.handle.id == o.id;
            }
        });
        Tuple<T> t = get(addr);
        return t == null ? null : t.object;
    }

    static class Tuple<Z> {
        private final FactHandleImpl handle;
        private final Z object;

        Tuple(FactHandleImpl handle, Z object) {
            this.handle = handle;
            this.object = object;
        }

        public FactHandle getHandle() {
            return handle;
        }

        public Object getFact() {
            return object;
        }

        @Override
        public int hashCode() {
            return handle.hash;
        }

        @Override
        public String toString() {
            return "Tuple{" +
                    "handle=" + handle +
                    ", object=" + object +
                    ", hash=" + hashCode() +
                    '}';
        }
    }

}
