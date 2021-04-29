package org.evrete.runtime;

import org.evrete.Configuration;
import org.evrete.api.*;
import org.evrete.runtime.async.RuleHotDeploymentTask;
import org.evrete.runtime.evaluation.MemoryAddress;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

abstract class AbstractRuleSession<S extends RuleSession<S>> extends AbstractRuntime<RuntimeRule, S> implements RuleSession<S> {
    private static final Logger LOGGER = Logger.getLogger(AbstractRuleSession.class.getName());
    private final RuntimeRules ruleStorage;
    final SessionMemory memory;
    final DeltaMemoryManager deltaMemoryManager;
    private final KnowledgeRuntime knowledge;
    private final boolean warnUnknownTypes;
    private boolean active = true;

    AbstractRuleSession(KnowledgeRuntime knowledge) {
        super(knowledge);
        this.deltaMemoryManager = new DeltaMemoryManager();
        this.ruleStorage = new RuntimeRules();
        MemoryFactory memoryFactory = getService().getMemoryFactoryProvider().instance(this);
        this.memory = new SessionMemory(this, memoryFactory);
        this.knowledge = knowledge;
        this.warnUnknownTypes = knowledge.getConfiguration().getAsBoolean(Configuration.WARN_UNKNOWN_TYPES);

        // Deploy existing rules
        for (RuleDescriptor descriptor : knowledge.getRules()) {
            deployRule(descriptor, false);
        }
    }

    private void reSortRules() {
        ruleStorage.sort(getRuleComparator());
    }

    @Override
    public RuntimeRule compileRule(RuleBuilder<?> builder) {
        RuleDescriptor rd = compileRuleBuilder(builder);
        return deployRule(rd, true);
    }

    @Override
    public List<RuntimeRule> getRules() {
        return Collections.unmodifiableList(ruleStorage.getList());
    }

    RuntimeRules getRuleStorage() {
        return ruleStorage;
    }

    @Override
    public void setRuleComparator(Comparator<Rule> ruleComparator) {
        super.setRuleComparator(ruleComparator);
        reSortRules();
    }

    private synchronized RuntimeRule deployRule(RuleDescriptor descriptor, boolean hotDeployment) {
        for (FactType factType : descriptor.getLhs().getFactTypes()) {
            TypeMemory tm = memory.getCreateUpdate(factType.type());
            tm.touchMemory(factType.getMemoryAddress());
        }
        RuntimeRuleImpl rule = ruleStorage.addRule(descriptor, this);
        if (hotDeployment) {
            getExecutor().invoke(new RuleHotDeploymentTask(rule));
        }
        reSortRules();
        return rule;
    }


    public void close() {
        synchronized (this) {
            invalidateSession();
            knowledge.close(this);
        }
    }

    private void invalidateSession() {
        this.active = false;
        this.memory.destroy();
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
            return memory.get(type).externalInsert(fact);
        }
    }

    @Override
    public final void update(FactHandle handle, Object newValue) {
        _assertActive();
        if (handle == null) {
            throw new NullPointerException("Null handle provided during update");
        }
        memory.get(handle.getTypeId()).add(Action.UPDATE, handle, new FactRecord(newValue));
    }

    @Override
    public final void delete(FactHandle handle) {
        _assertActive();
        memory.get(handle.getTypeId()).add(Action.RETRACT, handle, null);
    }

    public final void forEachFact(BiConsumer<FactHandle, Object> consumer) {
        // Scanning main memory and making sure fact handles are not deleted
        for (TypeMemory tm : memory) {
            tm.forEachFact(consumer);
        }
    }

    @Override
    public void onNewActiveField(ActiveField newField) {
        memory.onNewActiveField(newField);
    }

    @Override
    public final void onNewAlphaBucket(MemoryAddress address) {
        memory.onNewAlphaBucket(address);
    }

    @Override
    public void clear() {
        for (RuntimeRuleImpl rule : ruleStorage) {
            rule.clear();
        }
        memory.clear();
    }


    @Override
    public <T> Future<T> fireAsync(final T result) {
        return getExecutor().submit(this::fire, result);
    }

}
