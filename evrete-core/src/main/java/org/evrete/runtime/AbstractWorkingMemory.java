package org.evrete.runtime;

import org.evrete.Configuration;
import org.evrete.api.*;
import org.evrete.runtime.evaluation.AlphaBucketMeta;

import java.util.function.BiConsumer;
import java.util.logging.Logger;

abstract class AbstractWorkingMemory<S extends KnowledgeSession<S>> extends AbstractRuntime<S> implements KnowledgeSession<S> {
    private static final Logger LOGGER = Logger.getLogger(AbstractWorkingMemory.class.getName());
    final KnowledgeRuntime knowledge;
    final SessionMemory memory;
    final MemoryActionBuffer buffer = new MemoryActionBuffer();
    private final MemoryFactory memoryFactory;
    private boolean active = true;


    AbstractWorkingMemory(KnowledgeRuntime knowledge) {
        super(knowledge);
        this.knowledge = knowledge;
        this.memoryFactory = knowledge.getService().getMemoryFactoryProvider().instance(this);
        this.memory = new SessionMemory(knowledge.getConfiguration(), memoryFactory);
    }

    void invalidateSession() {
        this.active = false;
    }

    final MemoryFactory getMemoryFactory() {
        return memoryFactory;
    }

    private void _assertActive() {
        if (!active) {
            throw new IllegalStateException("Session has been closed");
        }
    }

    public final SessionMemory getMemory() {
        return memory;
    }

    @Override
    public final FactHandle insert(Object fact) {
        _assertActive();
        return insert(getTypeResolver().resolve(fact), fact);
    }

    @SuppressWarnings("unused")
    @Override
    public final FactHandle insert(String type, Object fact) {
        _assertActive();
        return insert(getTypeResolver().getType(type), fact);
    }

    @Override
    public Object getFact(FactHandle handle) {
        return memory.get(handle.getTypeId()).getFact(handle).instance;
    }

    private FactHandle insert(Type<?> type, Object fact) {
        if (type == null) {
            if (getConfiguration().getAsBoolean(Configuration.WARN_UNKNOWN_TYPES)) {
                LOGGER.warning("Can not resolve type for " + fact + ", insert operation skipped.");
            }
            return null;
        } else {
            LazyInsertState innerFact = buildFactRecord(type, fact);
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
        _assertActive();
        Type<?> type = getTypeResolver().getType(handle.getTypeId());
        if (type == null) {
            if (getConfiguration().getAsBoolean(Configuration.WARN_UNKNOWN_TYPES)) {
                LOGGER.warning("Can not resolve type for fact handle " + handle + ", update operation skipped.");
            }
        } else {
            buffer.add(Action.UPDATE, handle, buildFactRecord(type, newValue));
        }
    }


    @Override
    public final void delete(FactHandle handle) {
        _assertActive();
        buffer.add(Action.RETRACT, handle, null);
    }

    private LazyInsertState buildFactRecord(Type<?> type, Object instance) {
        ValueResolver valueResolver = memory.valueResolver;
        ActiveField[] activeFields = getActiveFields(type);
        ValueHandle[] valueHandles = new ValueHandle[activeFields.length];
        // We will need field values for lazy alpha tests thus avoiding
        // extra calls to ValueResolver
        Object[] transientFieldValues = new Object[activeFields.length];
        FactRecord record = new FactRecord(instance, valueHandles);
        for (ActiveField field : activeFields) {
            int idx = field.getValueIndex();
            Object fieldValue = field.readValue(instance);
            valueHandles[idx] = valueResolver.getValueHandle(field.getValueType(), fieldValue);
            transientFieldValues[idx] = fieldValue;
        }
        return new LazyInsertState(record, transientFieldValues);
    }

    public final void forEachFact(BiConsumer<FactHandle, Object> consumer) {
        memory.forEachFactEntry(consumer);
    }

    @Override
    protected final void onNewActiveField(ActiveField newField) {
        memory.onNewActiveField(newField);
    }

    @Override
    public final void onNewAlphaBucket(FieldsKey key, AlphaBucketMeta meta) {
        memory.onNewAlphaBucket(key, meta);
    }

    @Override
    public final Kind getKind() {
        return Kind.SESSION;
    }


    @Override
    public void clear() {
        memory.clear();
    }

}
