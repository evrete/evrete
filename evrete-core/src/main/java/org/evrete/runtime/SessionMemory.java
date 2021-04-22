package org.evrete.runtime;

import org.evrete.api.KeyedFactStorage;
import org.evrete.api.MemoryFactory;
import org.evrete.api.Type;
import org.evrete.collections.ArrayOf;
import org.evrete.runtime.evaluation.AlphaBucketMeta;

import java.util.Iterator;

public class SessionMemory extends MemoryComponent implements Iterable<TypeMemory> {
    private final ArrayOf<TypeMemory> typedMemories;

    SessionMemory(AbstractWorkingMemory<?> runtime, MemoryFactory memoryFactory) {
        super(runtime, memoryFactory);
        this.typedMemories = new ArrayOf<>(new TypeMemory[]{});
    }

    @Override
    protected void clearLocalData() {
    }

    @Override
    public Iterator<TypeMemory> iterator() {
        return typedMemories.iterator();
    }

    void onNewActiveField(ActiveField newField) {
        getCreateUpdate(newField.type());
    }

    void onNewAlphaBucket(int type, FieldsKey key, AlphaBucketMeta meta) {
        getCreateUpdate(type)
                .onNewAlphaBucket(key, meta);
    }

    KeyedFactStorage getBetaFactStorage(FactType factType) {
        FieldsKey fields = factType.getFields();
        AlphaBucketMeta mask = factType.getAlphaMask();
        return get(factType.type()).get(fields).get(mask);
    }

    public TypeMemory get(Type<?> t) {
        return get(t.getId());
    }

    TypeMemory getCreateUpdate(int type) {
        TypeMemory m = typedMemories.get(type);
        if (m == null) {
            m = new TypeMemory(this, type);
            typedMemories.set(type, m);
        } else {
            // Making sure type uses the same alpha conditions
            m.updateCachedData();
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
