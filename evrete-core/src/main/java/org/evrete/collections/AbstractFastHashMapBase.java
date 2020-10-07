package org.evrete.collections;

import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

public abstract class AbstractFastHashMapBase<K, E extends HashEntry<K>> extends AbstractLinearHash<E> {

    AbstractFastHashMapBase(int initialCapacity) {
        super(initialCapacity);
    }

    protected abstract ToIntFunction<K> keyHashFunction();

    protected abstract BiPredicate<K, K> keyHashEquals();

    @Override
    @SuppressWarnings("unchecked")
    protected final ToIntFunction<Object> getHashFunction() {
        return value -> keyHashFunction().applyAsInt(((E) value).key);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected final BiPredicate<Object, Object> getEqualsPredicate() {
        return (o1, o2) -> keyHashEquals().test(((E) o1).key, ((E) o2).key);
    }

    @SuppressWarnings("unchecked")
    final int getStorePosition(K key, boolean resize) {
        if (resize) super.resize();
        int hash = keyHashFunction().applyAsInt(key);
        Predicate<Object> predicate = o -> keyHashEquals().test(key, ((E) o).key);
        return findBinIndexFor(hash, predicate);
    }


    @SuppressWarnings("unchecked")
    E removeKey(K key) {
        int addr = getStorePosition(key, true);
        E found = (E) data[addr];
        if (found == null) {
            return null;
        } else {
            if (deletedIndices[addr]) {
                return null;
            } else {
                deletedIndices[addr] = true;
                size--;
                deletes++;
                return found;
            }
        }
    }


    E getEntry(K key) {
        int addr = getStorePosition(key, false);
        return get(addr);
    }

    final E computeEntryIfAbsent(K key, Function<K, E> function) {
        int addr = getStorePosition(key, true);
        E found = get(addr);
        if (found == null) {
            super.saveDirect((found = function.apply(key)), addr);
        }
        return found;
    }

}
