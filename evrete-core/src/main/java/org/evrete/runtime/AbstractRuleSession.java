package org.evrete.runtime;

import org.evrete.Configuration;
import org.evrete.api.*;
import org.evrete.runtime.async.*;
import org.evrete.runtime.evaluation.MemoryAddress;
import org.evrete.util.Mask;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.logging.Logger;

public abstract class AbstractRuleSession<S extends RuleSession<S>> extends AbstractRuntime<RuntimeRule, S> implements RuleSession<S> {
    private static final Logger LOGGER = Logger.getLogger(AbstractRuleSession.class.getName());
    private final RuntimeRules ruleStorage;
    final SessionMemory memory;
    final DeltaMemoryManager deltaMemoryManager;
    private final KnowledgeRuntime knowledge;
    private final boolean warnUnknownTypes;
    private boolean active = true;
    private ActivationManager activationManager;
    private BooleanSupplier fireCriteria = () -> true;

    private final List<SessionLifecycleListener> lifecycleListeners = new ArrayList<>();


    AbstractRuleSession(KnowledgeRuntime knowledge) {
        super(knowledge);
        this.deltaMemoryManager = new DeltaMemoryManager();
        this.ruleStorage = new RuntimeRules();
        MemoryFactory memoryFactory = getService().getMemoryFactoryProvider().instance(this);
        this.memory = new SessionMemory(this, memoryFactory);
        this.knowledge = knowledge;
        this.warnUnknownTypes = knowledge.getConfiguration().getAsBoolean(Configuration.WARN_UNKNOWN_TYPES);
        this.activationManager = newActivationManager();
        // Deploy existing rules
        for (RuleDescriptor descriptor : knowledge.getRules()) {
            deployRule(descriptor, false);
        }
    }

    void fireInner() {
        for (SessionLifecycleListener e : lifecycleListeners) {
            e.onEvent(SessionLifecycleListener.Event.PRE_FIRE);
        }
        switch (getAgendaMode()) {
            case DEFAULT:
                fireDefault(new ActivationContext());
                break;
            case CONTINUOUS:
                fireContinuous(new ActivationContext());
                break;
            default:
                throw new IllegalStateException("Unknown mode " + getAgendaMode());
        }
        purge();
    }

    private void fireContinuous(ActivationContext ctx) {

        List<RuntimeRule> agenda;
        while (fireCriteriaMet() && deltaMemoryManager.hasMemoryChanges()) {
            processBuffer();
            if (!(agenda = buildMemoryDeltas()).isEmpty()) {
                activationManager.onAgenda(ctx.incrementFireCount(), agenda);
                for (RuntimeRule candidate : agenda) {
                    RuntimeRuleImpl rule = (RuntimeRuleImpl) candidate;
                    if (activationManager.test(candidate)) {
                        activationManager.onActivation(rule, rule.executeRhs());
                    }
                }
            }
            commitRuleDeltas();
            commitBuffer();
        }
    }

    //TODO !!! fix this mess with the buffer and its status, it can be simplified
    private void fireDefault(ActivationContext ctx) {
        List<RuntimeRule> agenda;
        boolean bufferProcessed = false;
        while (fireCriteriaMet() && deltaMemoryManager.hasMemoryChanges()) {
            if (!bufferProcessed) {
                processBuffer();
                bufferProcessed = true;
            }
            agenda = buildMemoryDeltas();
            if (!agenda.isEmpty()) {
                activationManager.onAgenda(ctx.incrementFireCount(), agenda);
                for (RuntimeRule candidate : agenda) {
                    RuntimeRuleImpl rule = (RuntimeRuleImpl) candidate;
                    if (activationManager.test(candidate)) {
                        activationManager.onActivation(rule, rule.executeRhs());
                        // Analyzing buffer
                        int deltaOperations = deltaMemoryManager.deltaOperations();
                        if (deltaOperations > 0) {
                            // Breaking the agenda
                            bufferProcessed = false;
                            break;
                        } else {
                            // Processing deletes if any
                            processBuffer();
                        }
                    }
                }
                commitRuleDeltas();
            }
            commitBuffer();
        }
    }


    private void processBuffer() {
        MemoryDeltaTask deltaTask = new MemoryDeltaTask(memory.iterator());
        getExecutor().invoke(deltaTask);
        deltaMemoryManager.onDelete(deltaTask.getDeleteMask());
        deltaMemoryManager.onInsert(deltaTask.getInsertMask());
        deltaMemoryManager.clearBufferData();
    }

    private List<RuntimeRule> buildMemoryDeltas() {
        List<RuntimeRule> affectedRules = new LinkedList<>();
        Set<BetaEndNode> affectedEndNodes = new HashSet<>();
        Mask<MemoryAddress> matchMask = deltaMemoryManager.getInsertDeltaMask();

        for (RuntimeRuleImpl rule : ruleStorage) {
            boolean ruleAdded = false;

            for (RhsFactGroup group : rule.getLhs().getFactGroups()) {
                if (matchMask.intersects(group.getMemoryMask())) {
                    if (!ruleAdded) {
                        // Marking the rule as active
                        affectedRules.add(rule);
                        ruleAdded = true;
                    }

                    if (group instanceof BetaEndNode) {
                        affectedEndNodes.add((BetaEndNode) group);
                    }
                }
            }
        }

        // Ordered task 1 - process beta nodes, i.e. evaluate conditions
        List<Completer> tasks = new LinkedList<>();
        if (!affectedEndNodes.isEmpty()) {
            tasks.add(new RuleMemoryInsertTask(affectedEndNodes, matchMask, true));
        }

        if (tasks.size() > 0) {
            ForkJoinExecutor executor = getExecutor();
            for (Completer task : tasks) {
                executor.invoke(task);
            }
        }

        deltaMemoryManager.clearDeltaData();
        return affectedRules;
    }

    private void purge() {
        Mask<MemoryAddress> factPurgeMask = deltaMemoryManager.getDeleteDeltaMask();
        if (factPurgeMask.cardinality() > 0) {
            ForkJoinExecutor executor = getExecutor();
            MemoryPurgeTask purgeTask = new MemoryPurgeTask(memory, factPurgeMask);
            executor.invoke(purgeTask);
            Mask<MemoryAddress> emptyKeysMask = purgeTask.getKeyPurgeMask();
            if (emptyKeysMask.cardinality() > 0) {
                // Purging rule beta-memories
                executor.invoke(new ConditionMemoryPurgeTask(ruleStorage, emptyKeysMask));
            }
            deltaMemoryManager.clearDeleteData();
        }
    }

    private void commitBuffer() {
        memory.commitBuffer();
    }

    private void commitRuleDeltas() {
        for (RuntimeRuleImpl rule : ruleStorage) {
            rule.commitDeltas();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public S setExecutionPredicate(BooleanSupplier criteria) {
        applyFireCriteria(criteria);
        return (S) this;
    }

    @Override
    public final ActivationManager getActivationManager() {
        return activationManager;
    }

    private boolean fireCriteriaMet() {
        return this.fireCriteria.getAsBoolean();
    }

    private void applyFireCriteria(BooleanSupplier fireCriteria) {
        this.fireCriteria = fireCriteria;
    }

    void applyActivationManager(ActivationManager activationManager) {
        this.activationManager = activationManager;
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
    public final RuntimeRule getRule(String name) {
        return ruleStorage.get(name);
    }

    @Override
    public List<RuntimeRule> getRules() {
        return Collections.unmodifiableList(ruleStorage.getList());
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

    @Override
    @SuppressWarnings("unchecked")
    public S addEventListener(SessionLifecycleListener listener) {
        this.lifecycleListeners.add(listener);
        return (S) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public S removeEventListener(SessionLifecycleListener listener) {
        this.lifecycleListeners.remove(listener);
        return (S) this;
    }

    void closeInner() {
        synchronized (this) {
            for (SessionLifecycleListener e : lifecycleListeners) {
                e.onEvent(SessionLifecycleListener.Event.PRE_CLOSE);
            }
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

    @Override
    void _assertActive() {
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

    @SuppressWarnings("unchecked")
    <T> T getFactInner(FactHandle handle) {
        return (T) memory.get(handle.getTypeId()).getFact(handle);
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

    final void updateInner(FactHandle handle, Object newValue) {
        _assertActive();
        if (handle == null) {
            throw new NullPointerException("Null handle provided during update");
        }
        memory.get(handle.getTypeId()).add(Action.UPDATE, handle, new FactRecord(newValue));
    }

    final void deleteInner(FactHandle handle) {
        _assertActive();
        memory.get(handle.getTypeId()).add(Action.RETRACT, handle, null);
    }

    final void forEachFactInner(BiConsumer<FactHandle, Object> consumer) {
        // Scanning main memory and making sure fact handles are not deleted
        for (TypeMemory tm : memory) {
            tm.forEachFact(consumer);
        }
    }

    @SuppressWarnings("unchecked")
    <T> void forEachFactInner(String type, Consumer<T> consumer) {
        Type<?> t = getTypeResolver().getType(type);
        if (t == null) {
            throw new IllegalArgumentException("Type not found: '" + type + "'");
        } else {
            memory
                    .getCreateUpdate(t.getId())
                    .forEachFact((handle, o) -> consumer.accept((T) o));
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

    void clearInner() {
        for (RuntimeRuleImpl rule : ruleStorage) {
            rule.clear();
        }
        memory.clear();
    }
}
