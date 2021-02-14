package org.evrete.spi.minimal;

import org.evrete.api.FactHandleVersioned;
import org.evrete.api.SharedPlainFactStorage;
import org.evrete.collections.AbstractLinearHash;

import java.util.function.BiPredicate;
import java.util.function.ToIntFunction;

public class SharedAlphaDataOld extends AbstractLinearHash<FactHandleVersioned> implements SharedPlainFactStorage {
    private static final ToIntFunction<Object> HASH_FUNCTION_HANDLE = h -> ((FactHandleVersioned) h).hashCode();

    private static final BiPredicate<Object, Object> EQ_FUNCTION_HANDLE = (o1, o2) -> {
        FactHandleVersioned i1 = (FactHandleVersioned) o1;
        FactHandleVersioned i2 = (FactHandleVersioned) o2;
        return i1.equals(i2);
    };

    @Override
    protected ToIntFunction<Object> getHashFunction() {
        return HASH_FUNCTION_HANDLE;
    }

    @Override
    protected BiPredicate<Object, Object> getEqualsPredicate() {
        return EQ_FUNCTION_HANDLE;
    }

    @Override
    public void insert(FactHandleVersioned fact) {
        super.addSilent(fact);
    }

    @Override
    public void insert(SharedPlainFactStorage other) {
        int size = other.size();
        if (size == 0) return;
        if (other instanceof SharedAlphaDataOld) {
            SharedAlphaDataOld sad = (SharedAlphaDataOld) other;
            this.bulkAdd(sad);
        } else {
            this.ensureExtraCapacity(size);
            other.iterator().forEachRemaining(this::insert);
        }
    }
}
