package org.evrete.runtime;

import org.evrete.Configuration;
import org.evrete.api.*;
import org.evrete.collections.ArrayOf;
import org.evrete.runtime.evaluation.AlphaBucketMeta;
import org.evrete.util.Bits;

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

/*
    void touchMemory(Type<?> t, ActiveField[] activeFields, AlphaEvaluator[] alphaEvaluators, FieldsKey key, AlphaBucketMeta alphaMeta) {
        getCreate(t, activeFields, alphaEvaluators).touchMemory(key, alphaMeta);
    }
*/

    void onNewActiveField(TypeMemoryState state, ActiveField newField) {
        // This will update type memory's fields and alpha-conditions
        getCreate(state);
    }

    void onNewAlphaBucket(TypeMemoryState newState, FieldsKey key, AlphaBucketMeta meta) {
        getCreate(newState)
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

    //TODO method updates type memory's state variables, rename
    TypeMemory getCreate(TypeMemoryState state) {
        Type<?> t = state.type;
        TypeMemory m = typedMemories.get(t.getId());
        if (m == null) {
            m = new TypeMemory(this, state);
            typedMemories.set(t.getId(), m);
        } else {
            // Making sure type uses the same alpha conditions
            m.updateCachedData(state);
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
    void insert(LazyValues values, Bits alphaTests, FactHandleVersioned value) {
        throw new UnsupportedOperationException("Direct insert not supported");
    }

    @Override
    public void commitChanges() {
        for (MemoryComponent child : childComponents()) {
            child.commitChanges();
        }
    }
}
