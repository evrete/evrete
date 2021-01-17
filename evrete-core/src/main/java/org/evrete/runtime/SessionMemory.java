package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.collections.AbstractLinearHashMap;
import org.evrete.collections.LinearHashMap;
import org.evrete.runtime.evaluation.AlphaBucketMeta;
import org.evrete.runtime.evaluation.AlphaDelta;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;

public abstract class SessionMemory extends AbstractRuntime<StatefulSession> implements WorkingMemory, Iterable<TypeMemory> {
    private static final Logger LOGGER = Logger.getLogger(SessionMemory.class.getName());
    private final LinearHashMap<Type<?>, TypeMemory> typedMemories;
    private static final Function<AbstractLinearHashMap.Entry<Type<?>, TypeMemory>, TypeMemory> TYPE_MEMORY_MAPPING = AbstractLinearHashMap.Entry::getValue;

    protected SessionMemory(KnowledgeImpl parent) {
        super(parent);
        this.typedMemories = new LinearHashMap<>(getTypeResolver().getKnownTypes().size());
    }


    @Override
    public Iterator<TypeMemory> iterator() {
        return typedMemories.iterator(TYPE_MEMORY_MAPPING);
    }

    ReIterator<TypeMemory> typeMemories() {
        return typedMemories.valueIterator();
    }

    @Override
    protected TypeResolver newTypeResolver() {
        return getParentContext().getTypeResolver().copyOf();
    }

    @Override
    public final Kind getKind() {
        return Kind.SESSION;
    }

    void touchMemory(FieldsKey key, AlphaBucketMeta alphaMeta) {
        Type<?> t = key.getType();
        typedMemories
                .computeIfAbsent(t, k -> new TypeMemory(this, t))
                .touchMemory(key, alphaMeta);
    }

    @Override
    public void clear() {
        typedMemories.forEachValue(TypeMemory::clear);
    }

    @Override
    public void insert(Object fact) {
        memoryAction(Action.INSERT, fact);
    }

    void processDeleteBuffer() {
        for (TypeMemory tm : this) {
            tm.processDeleteBuffer();
        }
    }

    void processInsertBuffer() {
        for (TypeMemory tm : this) {
            tm.processInsertBuffer();
        }
    }

    @Override
    protected synchronized void onNewActiveField(ActiveField newField) {
        Type<?> t = newField.getDeclaringType();
        TypeMemory tm = typedMemories.get(t);
        if (tm == null) {
            tm = new TypeMemory(this, t);
            typedMemories.put(t, tm);
        }
        tm.onNewActiveField(newField);
    }

    @Override
    protected void onNewAlphaBucket(AlphaDelta delta) {
        Type<?> t = delta.getKey().getType();
        TypeMemory tm = typedMemories.get(t);
        if (tm == null) {
            tm = new TypeMemory(this, t);
            typedMemories.put(t, tm);
        } else {
            tm.onNewAlphaBucket(delta);
        }
    }

    SharedBetaFactStorage getBetaFactStorage(FactType factType) {
        Type<?> t = factType.getType();
        FieldsKey fields = factType.getFields();
        AlphaBucketMeta mask = factType.getAlphaMask();

        return get(t).get(fields).get(mask);
    }

    void destroy() {
        typedMemories.clear();
    }

    void memoryAction(Action action, Object o) {
        memoryAction(action, getTypeResolver().resolve(o), o);
    }

    private void memoryAction(Action action, Type<?> t, Object o) {
        if (t == null) {
            LOGGER.warning("Unknown object type of " + o + ", action " + action + "  skipped");
        } else {
            get(t).memoryAction(action, o);
        }
    }

    @Override
    public <T> void forEachMemoryObject(String type, Consumer<T> consumer) {
        Type<?> t = getTypeResolver().getType(type);
        if (t != null) {
            TypeMemory tm = typedMemories.get(t);
            tm.forEachMemoryObject(consumer);
        }
    }

    @Override
    public void forEachMemoryObject(Consumer<Object> consumer) {
        typedMemories.forEachValue(tm -> tm.forEachObjectUnchecked(consumer));
    }

    public TypeMemory get(Type<?> t) {
        TypeMemory m = typedMemories.get(t);
        if (m == null) {
            // TODO !!!! touch TypeMemory if a corresponding type has been explicitly declared in TypeResolver
            throw new IllegalArgumentException("No type memory created for " + t);
        } else {
            return m;
        }
    }

    @Override
    public void update(Object fact) {
        memoryAction(Action.UPDATE, fact);
    }

    @Override
    public void delete(Object fact) {
        memoryAction(Action.RETRACT, fact);
    }
}
