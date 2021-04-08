package org.evrete.runtime;

import org.evrete.Configuration;
import org.evrete.api.*;
import org.evrete.runtime.evaluation.AlphaBucketMeta;

import java.util.function.BiConsumer;
import java.util.logging.Logger;

abstract class AbstractWorkingMemory<S extends RuleSession<S>> extends AbstractRuntime<RuntimeRule, S> implements RuleSession<S> {
    private static final Logger LOGGER = Logger.getLogger(AbstractWorkingMemory.class.getName());
    final KnowledgeRuntime knowledge;
    final SessionMemory memory;
    final MemoryActionCounter actionCounter;
    private boolean active = true;
    private final boolean warnUnknownTypes;

    AbstractWorkingMemory(KnowledgeRuntime knowledge) {
        super(knowledge);
        this.knowledge = knowledge;
        this.actionCounter = new MemoryActionCounter();
        MemoryFactory memoryFactory = knowledge.getService().getMemoryFactoryProvider().instance(this);
        this.memory = new SessionMemory(knowledge.getConfiguration(), memoryFactory);
        this.warnUnknownTypes = knowledge.getConfiguration().getAsBoolean(Configuration.WARN_UNKNOWN_TYPES);
    }

    void invalidateSession() {
        this.active = false;
    }

    @Override
    public Knowledge getParentContext() {
        return knowledge;
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
        return memory.get(handle.getTypeId()).getFact(handle);
    }

    private FactHandle insert(Type<?> type, Object fact) {
        if (fact == null) throw new NullPointerException("Null facts are not supported");
        if (type == null) {
            if (warnUnknownTypes) {
                LOGGER.warning("Can not resolve type for " + fact + ", insert operation skipped.");
            }
            return null;
        } else {
            //TODO move the entire block to the TypeMemory
            TypeMemory tm = memory.get(type);
            LazyInsertState innerFact = tm.buildFactRecord(fact);
            FactHandle factHandle = tm.registerNewFact(innerFact);
            if (factHandle == null) {
                LOGGER.warning("Fact " + fact + " has been already inserted");
            } else {
                tm.add(Action.INSERT, factHandle, innerFact, actionCounter);
            }
            return factHandle;
        }
    }

    @Override
    public final void update(FactHandle handle, Object newValue) {
        _assertActive();
        Type<?> type = getTypeResolver().getType(handle.getTypeId());
        if (type == null) {
            if (warnUnknownTypes) {
                LOGGER.warning("Can not resolve type for fact handle " + handle + ", update operation skipped.");
            }
        } else {
            memory.get(type).add(Action.UPDATE, handle, memory.get(type).buildFactRecord(newValue), actionCounter);
        }
    }


    @Override
    public final void delete(FactHandle handle) {
        _assertActive();
        memory.get(handle.getTypeId()).add(Action.RETRACT, handle, null, actionCounter);
    }

    public final void forEachFact(BiConsumer<FactHandle, Object> consumer) {
        // Scanning main memory and making sure fact handles are not deleted
        for (TypeMemory tm : memory) {
            tm.forEachFact(consumer);
        }
    }

    @Override
    protected void onNewActiveField(Type<?> type, ActiveField newField, ActiveField[] newFields) {
        memory.onNewActiveField(type, getAlphaEvaluators(type), newField, newFields);
    }

    @Override
    public final void onNewAlphaBucket(FieldsKey key, AlphaBucketMeta meta) {
        memory.onNewAlphaBucket(key, getAlphaEvaluators(key.getType()), meta);
    }

    @Override
    public void clear() {
        memory.clear();
    }

}
