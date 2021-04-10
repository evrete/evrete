package org.evrete.runtime;

import org.evrete.Configuration;
import org.evrete.api.KeyedFactStorage;
import org.evrete.api.MemoryFactory;
import org.evrete.api.Type;
import org.evrete.collections.ArrayOf;
import org.evrete.runtime.evaluation.AlphaBucketMeta;

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

    void onNewActiveField(TypeMemoryState state) {
        getCreateUpdate(state);
    }

    void onNewAlphaBucket(TypeMemoryState newState, FieldsKey key, AlphaBucketMeta meta) {
        getCreateUpdate(newState)
                .onNewAlphaBucket(key, meta);
    }

    KeyedFactStorage getBetaFactStorage(FactType factType) {
        Type<?> t = factType.getType();
        FieldsKey fields = factType.getFields();
        AlphaBucketMeta mask = factType.getAlphaMask();

        return get(t).get(fields).get(mask);
    }

    public TypeMemory get(Type<?> t) {
        return get(t.getId());
    }

    TypeMemory getCreateUpdate(TypeMemoryState state) {
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
    public void commitChanges() {
        for (MemoryComponent child : childComponents()) {
            child.commitChanges();
        }
    }
}
