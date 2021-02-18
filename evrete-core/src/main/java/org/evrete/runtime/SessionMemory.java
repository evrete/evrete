package org.evrete.runtime;

import org.evrete.Configuration;
import org.evrete.api.*;
import org.evrete.collections.ArrayOf;
import org.evrete.runtime.evaluation.AlphaBucketMeta;

import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class SessionMemory extends MemoryComponent implements Iterable<TypeMemory> {
    private final ArrayOf<TypeMemory> typedMemories;

    SessionMemory(Configuration configuration, MemoryFactory memoryFactory) {
        super(memoryFactory, configuration);
        this.typedMemories = new ArrayOf<>(new TypeMemory[]{});
    }

    @Override
    protected void forEachChildComponent(Consumer<MemoryComponent> consumer) {
        typedMemories.forEach(consumer);
    }

    @Override
    protected void clearLocalData() {
    }

    void forEachFactEntry(BiConsumer<FactHandle, Object> consumer) {
        typedMemories.forEach(tm -> tm.forEachEntry((handle, record) -> consumer.accept(handle, record.instance)));
    }

    @Override
    public Iterator<TypeMemory> iterator() {
        return typedMemories.iterator();
    }

    void touchMemory(FieldsKey key, AlphaBucketMeta alphaMeta) {
        Type<?> t = key.getType();
        get(t).touchMemory(key, alphaMeta);
    }

    synchronized void onNewActiveField(ActiveField newField) {
        Type<?> t = newField.getDeclaringType();
        TypeMemory tm = get(t);
        tm.onNewActiveField(newField);
    }

    void onNewAlphaBucket(FieldsKey key, AlphaBucketMeta meta) {
        Type<?> t = key.getType();
        TypeMemory tm = typedMemories.get(t.getId());
        if (tm == null) {
            tm = new TypeMemory(SessionMemory.this, t);
            typedMemories.set(t.getId(), tm);
        } else {
            tm.onNewAlphaBucket(key, meta);
        }
    }

    SharedBetaFactStorage getBetaFactStorage(FactType factType) {
        Type<?> t = factType.getType();
        FieldsKey fields = factType.getFields();
        AlphaBucketMeta mask = factType.getAlphaMask();

        return get(t).get(fields).get(mask);
    }

    public TypeMemory get(Type<?> t) {
        TypeMemory m = typedMemories.get(t.getId());
        if (m == null) {
            m = new TypeMemory(this, t);
            typedMemories.set(t.getId(), m);
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
    public void insert(FactHandleVersioned value, FieldToValueHandle key) {
        throw new UnsupportedOperationException("Direct insert not supported");
    }

    @Override
    public void commitChanges() {
        forEachChildComponent(MemoryComponent::commitChanges);
    }
}
