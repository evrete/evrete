package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.runtime.evaluation.AlphaDelta;

import java.util.function.BiConsumer;
import java.util.logging.Logger;

abstract class AbstractWorkingMemory<S extends KnowledgeSession<S>> extends AbstractRuntime<S> implements KnowledgeSession<S> {
    private static final Logger LOGGER = Logger.getLogger(AbstractWorkingMemory.class.getName());
    final KnowledgeRuntime knowledge;
    final SessionMemory memory;
    final MemoryActionBuffer buffer = new MemoryActionBuffer();
    private final MemoryFactory memoryFactory;

    AbstractWorkingMemory(KnowledgeRuntime knowledge) {
        super(knowledge);
        this.knowledge = knowledge;
        this.memoryFactory = knowledge.getService().getMemoryFactoryProvider().instance(this);
        this.memory = new SessionMemory(knowledge.getConfiguration(), memoryFactory);
    }

    final MemoryFactory getMemoryFactory() {
        return memoryFactory;
    }

    public final SessionMemory getMemory() {
        return memory;
    }

    @Override
    public final FactHandle insert(Object fact) {
        return insert(getTypeResolver().resolve(fact), fact);
    }

    @Override
    public final FactHandle insert(String type, Object fact) {
        return insert(getTypeResolver().getType(type), fact);
    }

    @Override
    public Object getFact(FactHandle handle) {
        return memory.get(handle.getTypeId()).getFact(handle);
    }

    private FactHandle insert(Type<?> type, Object fact) {
        if (type == null) {
            LOGGER.warning("Can not resolve type for " + fact + ", insert operation skipped.");
            return null;
        } else {
            FactRecord innerFact = buildFactRecord(type, fact);
            FactHandle factHandle = memory.get(type).registerNewFact(innerFact);
            if (factHandle == null) {
                LOGGER.warning("Fact " + fact + " has been already inserted");
            } else {
                buffer.add(Action.INSERT, factHandle, innerFact);
            }
            return factHandle;
        }
    }

    @Override
    public final void update(FactHandle handle, Object newValue) {
        Type<?> type = getTypeResolver().getType(handle.getTypeId());
        if (type == null) {
            LOGGER.warning("Can not resolve type for fact handle " + handle + ", update operation skipped.");
        } else {
            buffer.add(Action.UPDATE, handle, buildFactRecord(type, newValue));
        }
    }


    @Override
    public final void delete(FactHandle handle) {
        buffer.add(Action.RETRACT, handle, null);
    }

    private FactRecord buildFactRecord(Type<?> type, Object instance) {
        ActiveField[] activeFields = getActiveFields(type);
        Object[] fieldValues = new Object[activeFields.length];
        FactRecord record = new FactRecord(instance, fieldValues);
        for (ActiveField field : activeFields) {
            fieldValues[field.getValueIndex()] = field.readValue(instance);
        }
        return record;
    }

    public final void forEachFact(BiConsumer<FactHandle, Object> consumer) {
        memory.forEachFactEntry(consumer);
    }

    @Override
    protected final void onNewActiveField(ActiveField newField) {
        memory.onNewActiveField(newField);
/*
        Type<?> t = newField.getDeclaringType();
        TypeMemory tm = memory.get(t);
        tm.onNewActiveField(newField);
*/
    }

    @Override
    protected final void onNewAlphaBucket(AlphaDelta delta) {
        memory.onNewAlphaBucket(delta);
/*
        Type<?> t = delta.getKey().getType();
        TypeMemory tm = memory.get(t.getId());
        if (tm == null) {
            tm = new TypeMemory(SessionMemory.this, t);
            typedMemories.set(t.getId(), tm);
        } else {
            tm.onNewAlphaBucket(delta);
        }
*/
    }

    @Override
    public final Kind getKind() {
        return Kind.SESSION;
    }


    public void clear() {
        memory.clear();
    }

}
