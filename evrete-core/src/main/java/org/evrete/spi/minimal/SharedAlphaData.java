package org.evrete.spi.minimal;

import org.evrete.api.RuntimeFact;
import org.evrete.api.SharedPlainFactStorage;
import org.evrete.collections.AbstractLinearHash;

import java.util.function.BiPredicate;
import java.util.function.ToIntFunction;

public class SharedAlphaData extends AbstractLinearHash<RuntimeFact> implements SharedPlainFactStorage {
    private static final ToIntFunction<Object> HASH_FUNCTION_OBJECT = System::identityHashCode;

    private static final ToIntFunction<Object> HASH_FUNCTION_HANDLE = value -> {
        RuntimeFact impl = (RuntimeFact) value;
        return HASH_FUNCTION_OBJECT.applyAsInt(impl.getDelegate());
    };

    private static final BiPredicate<Object, Object> EQ_FUNCTION_OBJECT = (o1, o2) -> o1 == o2;

    private static final BiPredicate<Object, Object> EQ_FUNCTION_HANDLE = (o1, o2) -> {
        RuntimeFact i1 = (RuntimeFact) o1;
        RuntimeFact i2 = (RuntimeFact) o2;
        return EQ_FUNCTION_OBJECT.test(i1.getDelegate(), i2.getDelegate());
    };

    private static final BiPredicate<RuntimeFact, Object> FIND_FUNCTION = (runtimeFact, o) -> EQ_FUNCTION_OBJECT.test(runtimeFact.getDelegate(), o);


    @Override
    protected ToIntFunction<Object> getHashFunction() {
        return HASH_FUNCTION_HANDLE;
    }

    @Override
    protected BiPredicate<Object, Object> getEqualsPredicate() {
        return EQ_FUNCTION_HANDLE;
    }

    public void insert(RuntimeFact fact) {
        super.add(fact);
    }

    // TODO check usage
    public void delete(RuntimeFact fact) {
        super.removeEntry(fact);
    }

    public RuntimeFact find(Object fact) {
        return get(findBinIndex(fact, HASH_FUNCTION_OBJECT.applyAsInt(fact), FIND_FUNCTION));
    }

    @Override
    public void insert(SharedPlainFactStorage other) {
        int size = other.size();
/*
        this.ensureExtraCapacity(size);
        other.iterator().forEachRemaining(new Consumer<RuntimeFact>() {
            @Override
            public void accept(RuntimeFact fact) {
                if(!fact.isDeleted()) {
                    SharedAlphaData.this.insert(fact);
                }
            }
        });
*/
        if (size == 0) return;
        if (other instanceof SharedAlphaData) {
            SharedAlphaData sad = (SharedAlphaData) other;
            this.bulkAdd(sad);
        } else {
            this.ensureExtraCapacity(size);
            other.iterator().forEachRemaining(this::insert);
        }
    }


/*
    @Override
    public void delete(RuntimeFact fact) {
        super.remove(fact);
    }

    @Override
    public void insert(RuntimeFact fact) {
        super.addNoResize(fact);
    }


    @Override
    //TODO !!!! check usage
    public RuntimeFact find(Object o) {
        int addr = fi
    }

    @Override
    public void insert(SharedPlainFactStorage other) {
        int size = other.size();
        if(size == 0) return;
        if(other instanceof SharedAlphaData) {
            SharedAlphaData sad = (SharedAlphaData) other;
            this.bulkAdd(sad);
        } else {
            this.ensureExtraCapacity(size);
            other.iterator().forEachRemaining(this::insert);
        }
    }
*/
}
