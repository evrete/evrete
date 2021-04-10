package org.evrete.runtime;

import org.evrete.api.ActiveField;
import org.evrete.api.FactHandleVersioned;
import org.evrete.api.KeyedFactStorage;
import org.evrete.runtime.evaluation.AlphaBucketMeta;

import java.util.Collection;
import java.util.LinkedList;

abstract class KeyMemoryBucket extends MemoryComponent {
    final KeyedFactStorage fieldData;
    final ActiveField[] activeFields;
    final Collection<FactHandleVersioned> insertData = new LinkedList<>();
    RuntimeFact current = null;

    KeyMemoryBucket(MemoryComponent runtime, FieldsKey typeFields) {
        super(runtime);
        this.fieldData = memoryFactory.newBetaStorage(typeFields.getFields());
        this.activeFields = typeFields.getFields();
    }

    static KeyMemoryBucket factory(MemoryComponent runtime, FieldsKey typeFields, AlphaBucketMeta alphaMask) {
        if (alphaMask.isEmpty()) {
            return new KeyMemoryBucketNoAlpha(runtime, typeFields);
        } else {
            return new KeyMemoryBucketAlpha(runtime, typeFields, alphaMask);
        }
    }

    abstract void doStuff();

    abstract void insert(Iterable<RuntimeFact> facts);

    @Override
    protected final void clearLocalData() {
        fieldData.clear();
    }

    final KeyedFactStorage getFieldData() {
        return fieldData;
    }

    @Override
    final public void commitChanges() {
        fieldData.commitChanges();
    }

    @Override
    public final String toString() {
        return fieldData.toString();
    }
}
