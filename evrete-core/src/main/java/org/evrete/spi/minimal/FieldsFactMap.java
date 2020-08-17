package org.evrete.spi.minimal;

import org.evrete.api.ReIterator;
import org.evrete.api.RuntimeFact;
import org.evrete.api.ValueRow;
import org.evrete.api.spi.SharedBetaFactStorage;

import java.util.function.BiPredicate;
import java.util.function.ToIntFunction;

class FieldsFactMap extends org.evrete.collections.AbstractHashData<ValueRowImpl> implements SharedBetaFactStorage.Scope {
    private static final ToIntFunction<Object> HASH_FUNCTION = Object::hashCode;
    private static final BiPredicate<ValueRowImpl, ValueRowImpl> EQ_FUNCTION_TYPED = ValueRowImpl::equals;


    @Override
    public ReIterator<ValueRow[]> keyIterator() {
        final ValueRowImpl[] arr = new ValueRowImpl[1];
        return iterator(vr -> {
            arr[0] = vr;
            return arr;
        });
    }

    @Override
    public void add(ValueRow save) {
        super.add((ValueRowImpl) save);
    }

    @Override
    public final long keyCount() {
        return size();
    }

    @Override
    protected ToIntFunction<Object> getHashFunction() {
        return HASH_FUNCTION;
    }

    public void addAll(FieldsFactMap other) {
        resize((int) (this.size() + other.size() + 1));
        other.forEachDataEntry(this::insertOtherNoResize);
    }

    @Override
    protected BiPredicate<Object, Object> getEqualsPredicate() {
        return IDENTITY_EQUALS;
    }

    private void insertOtherNoResize(ValueRowImpl other) {
        int hash = other.hashCode();
        int addr = findBinIndex(other, hash, EQ_FUNCTION_TYPED);
        ValueRowImpl found = get(addr);
        if (found == null) {
            saveDirect(other, addr);
        } else {
            found.mergeDataFrom(other);
        }
    }


    ValueRowImpl deleteAndTestExisting(RuntimeFact fact, int addr) {
        ValueRowImpl entry = get(addr);
        if (entry == null) {
            return null;
        } else {
            if (entry.removeFact(fact) == 0) {
                //Clear the whole key
                markDeleted(addr);
                return entry;
            } else {
                return null;
            }
        }
    }
}
