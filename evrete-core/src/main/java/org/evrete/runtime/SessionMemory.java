package org.evrete.runtime;

import org.evrete.Configuration;
import org.evrete.api.*;
import org.evrete.collections.ArrayOf;
import org.evrete.runtime.evaluation.AlphaBucketMeta;
import org.evrete.runtime.evaluation.AlphaEvaluator;

import java.util.Iterator;

public class SessionMemory extends MemoryComponent implements Iterable<TypeMemory> {
    private final ArrayOf<TypeMemory> typedMemories;

    SessionMemory(Configuration configuration, MemoryFactory memoryFactory) {
        super(memoryFactory, configuration);
        this.typedMemories = new ArrayOf<>(new TypeMemory[]{});
    }

    @Override
    protected void clearLocalData() {
    }

    @Override
    public Iterator<TypeMemory> iterator() {
        return typedMemories.iterator();
    }

    void touchMemory(Type<?> t, ActiveField[] activeFields, AlphaEvaluator[] alphaEvaluators, FieldsKey key, AlphaBucketMeta alphaMeta) {
        getCreate(t, activeFields, alphaEvaluators).touchMemory(key, alphaMeta);
    }

    synchronized void onNewActiveField(Type<?> t, AlphaEvaluator[] alphaEvaluators, ActiveField newField, ActiveField[] newFields) {
        getCreate(t, newFields, alphaEvaluators)
                .onNewActiveField(newField);
    }

    void onNewAlphaBucket(FieldsKey key, AlphaEvaluator[] newTypeAlphaEvaluators, AlphaBucketMeta meta) {
        getCreate(key.getType(), key.getFields(), newTypeAlphaEvaluators)
                .onNewAlphaBucket(key, meta);
    }

    SharedBetaFactStorage getBetaFactStorage(FactType factType) {
        Type<?> t = factType.getType();
        FieldsKey fields = factType.getFields();
        AlphaBucketMeta mask = factType.getAlphaMask();

        return get(t).get(fields).get(mask);
    }

    public TypeMemory get(Type<?> t) {
        return get(t.getId());
    }

    public TypeMemory getCreate(Type<?> t, ActiveField[] activeFields, AlphaEvaluator[] alphaEvaluators) {
        TypeMemory m = typedMemories.get(t.getId());
        if (m == null) {
            m = new TypeMemory(this, t, activeFields, alphaEvaluators);
            typedMemories.set(t.getId(), m);
        } else {
            // Making sure type uses the same alpha conditions
            m.updateCachedData(activeFields, alphaEvaluators);
        }
        return m;
    }

    public TypeMemory get(int typeId) {
        TypeMemory m = typedMemories.get(typeId);
        if (m == null) {
            throw new IllegalStateException("Unknown type id: " + typeId);
        }
        return m;
    }

    @Override
    void insert(FactHandleVersioned value, LazyInsertState insertState) {
        throw new UnsupportedOperationException("Direct insert not supported");
    }

    @Override
    public void commitChanges() {
        for (MemoryComponent child : childComponents()) {
            child.commitChanges();
        }
    }
}
