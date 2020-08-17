package org.evrete.spi.minimal;

import org.evrete.api.*;
import org.evrete.api.spi.SharedBetaFactStorage;
import org.evrete.collections.FastHashSet;

import java.util.Arrays;
import java.util.function.BiPredicate;

class SharedBetaDataTuple implements SharedBetaFactStorage {
    protected final Object[] reusableValueArr;
    private final FieldsFactMap delta;

    private final FastHashSet<ValueRow> deleteTasks = new FastHashSet<>();

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
    public boolean hasDeletedKeys() {
        return deleteTasks.size() > 0;
    }

    @Override
    public boolean isKeyDeleted(ValueRow row) {
        return deleteTasks.contains(row);
    }

    @Override
    public void clear() {
        delta.clear();
        main.clear();
        deleteTasks.clear();
    }

    @Override
    public void clearDeletedKeys() {
        deleteTasks.clear();
    }

    @ThreadUnsafe
    int hash(FieldToValue key) {
        int hash = 0;
        for (int i = 0; i < fields.length; i++) {
            hash ^= (reusableValueArr[i] = key.apply(fields[i])).hashCode();
        }
        return hash;
    }

    @Override
    public void ensureDeltaCapacity(int insertCount) {
        delta.resize((int) (delta.size() + insertCount));
    }

    @Override
    public boolean delete(RuntimeFact fact) {
        int hash = hash(fact);
        int addr = main.findBinIndex(reusableValueArr, hash, SHARED_ARRAY_EQ);
        ValueRow deleted = main.deleteAndTestExisting(fact, addr);
        if (deleted != null) {
            deleteTasks.add(deleted);
            return true;
        } else {
            return false;
        }
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
        main.resize();
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
    public void insertDirect(RuntimeFact fact) {
        main.resize();
        int hash = hash(fact);
        int addr = main.findBinIndex(reusableValueArr, hash, SHARED_ARRAY_EQ);
        ValueRowImpl found = main.get(addr);
        if (found == null) {
            found = new ValueRowImpl(Arrays.copyOf(reusableValueArr, reusableValueArr.length), hash);
            main.saveDirect(found, addr);
        }
        found.addFact(fact);
    }

    @Override
    public void mergeDelta() {
        main.addAll(delta);
        delta.clear();
    }
}
