package org.evrete.spi.minimal;

import org.evrete.api.*;
import org.evrete.api.spi.SharedBetaFactStorage;

import java.util.Arrays;
import java.util.function.BiPredicate;

class SharedBetaDataTuple implements SharedBetaFactStorage {
    protected final Object[] reusableValueArr;
    private final FieldsFactMap delta;
    protected final BiPredicate<ValueRowImpl, Object[]> SHARED_ARRAY_EQ = new BiPredicate<ValueRowImpl, Object[]>() {
        @Override
        public boolean test(ValueRowImpl entry, Object[] values) {
            return MiscUtils.sameData(entry.data, reusableValueArr);
        }
    };
    private final FieldsFactMap main;
    private final ActiveField[] fields;

    SharedBetaDataTuple(FieldsKey typeFields) {
        this.delta = new FieldsFactMap();
        this.main = new FieldsFactMap();
        this.fields = typeFields.getFields();
        this.reusableValueArr = new Object[fields.length];
    }

    @Override
    public void clear() {
        delta.clear();
        main.clear();
    }

/*
    @ThreadUnsafe
    int hash(FieldToValue key) {
        int hash = 0;
        for (int i = 0; i < fields.length; i++) {
            hash ^= (reusableValueArr[i] = key.apply(fields[i])).hashCode();
        }
        return hash;
    }
*/

    @ThreadUnsafe
    int hash(FieldToValue key) {
        int hash = 0;
        for (int i = 0; i < fields.length; i++) {
            hash ^= (reusableValueArr[i] = key.apply(fields[i])).hashCode();
        }
        return hash;
    }


    @Override
    public void ensureExtraCapacity(int insertCount) {
        delta.resize((int) (delta.size() + insertCount));
    }

    @Override
    public ValueRow delete(RuntimeFact fact) {
        int hash = hash(fact);
        int addr = main.findBinIndex(reusableValueArr, hash, SHARED_ARRAY_EQ);
        return main.deleteAndTestExisting(fact, addr);
    }

    @Override
    public Scope delta() {
        return delta;
    }

    @Override
    public Scope main() {
        return main;
    }

    @Override
    public boolean insert(RuntimeFact fact) {
        int hash = hash(fact);
        int addr = main.findBinIndex(reusableValueArr, hash, SHARED_ARRAY_EQ);
        ValueRowImpl found = main.get(addr);
        if (found == null) {
            // No entry in main storage, now looking in delta
            addr = delta.findBinIndex(reusableValueArr, hash, SHARED_ARRAY_EQ);
            found = delta.get(addr);
            if (found == null) {
                // No entry in delta either, creating new one
                ValueRowImpl vr = new ValueRowImpl(Arrays.copyOf(reusableValueArr, reusableValueArr.length), hash, fact);
                delta.saveDirect(vr, addr);
                return true;
            }
        }

        found.addFact(fact);
        return false;
    }

    @Override
    public void mergeDelta() {
        main.addAll(delta);
        delta.clear();
    }
}
