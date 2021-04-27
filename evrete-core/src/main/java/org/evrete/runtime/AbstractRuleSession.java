package org.evrete.runtime;

import org.evrete.Configuration;
import org.evrete.api.*;
import org.evrete.runtime.async.*;
import org.evrete.runtime.evaluation.MemoryAddress;

import java.util.*;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.logging.Logger;

abstract class AbstractRuleSession<S extends RuleSession<S>> extends AbstractRuntime<RuntimeRule, S> implements RuleSession<S> {
    private static final Logger LOGGER = Logger.getLogger(AbstractRuleSession.class.getName());
    private final RuntimeRules ruleStorage;
    private ActivationManager activationManager;
    private BooleanSupplier fireCriteria = () -> true;
    final SessionMemory memory;
    final MemoryActionListenerImpl memoryActionListener;
    private final KnowledgeRuntime knowledge;
    private final boolean warnUnknownTypes;
    private boolean active = true;

    AbstractRuleSession(KnowledgeRuntime knowledge) {
        super(knowledge);
        this.activationManager = newActivationManager();
        this.memoryActionListener = new MemoryActionListenerImpl();
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

    private List<RuntimeRule> buildMemoryDeltas() {
        List<RuntimeRule> affectedRules = new LinkedList<>();
        Set<BetaEndNode> affectedEndNodes = new HashSet<>();

        for (RuntimeRuleImpl rule : ruleStorage) {
            rule.mergeNodeDeltas();
            boolean ruleAdded = false;

            for (TypeMemory tm : memory) {
                Type<?> t = tm.getType();
                if (!ruleAdded && rule.dependsOn(t)) {
                    affectedRules.add(rule);
                    ruleAdded = true;
                }

                for (BetaEndNode endNode : rule.getLhs().getEndNodes()) {
                    if (endNode.dependsOn(t)) {
                        affectedEndNodes.add(endNode);
                    }
                }
            }
        }

        // Ordered task 1 - process beta nodes, i.e. evaluate conditions
        List<Completer> tasks = new LinkedList<>();
        if (!affectedEndNodes.isEmpty()) {
            tasks.add(new RuleMemoryInsertTask(affectedEndNodes, true));
        }

        if (tasks.size() > 0) {
            ForkJoinExecutor executor = getExecutor();
            for (Completer task : tasks) {
                executor.invoke(task);
            }
        }
        return affectedRules;
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

    @Override
    public ActivationManager getActivationManager() {
        return activationManager;
    }

    void applyActivationManager(ActivationManager activationManager) {
        this.activationManager = activationManager;
    }

    @Override
    public <A extends ActivationManager> void setActivationManagerFactory(Class<A> managerClass) {
        super.setActivationManagerFactory(managerClass);
        this.activationManager = newActivationManager();
    }

    void applyFireCriteria(BooleanSupplier fireCriteria) {
        this.fireCriteria = fireCriteria;
    }

    @Override
    public void fire() {
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
    }

    private void invalidateSession() {
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

    private void fireContinuous(ActivationContext ctx) {
        List<RuntimeRule> agenda;
        while (fireCriteria.getAsBoolean() && memoryActionListener.hasData()) {
            processBuffer();
            if (!(agenda = buildMemoryDeltas()).isEmpty()) {
                activationManager.onAgenda(ctx.incrementFireCount(), agenda);
                for (RuntimeRule candidate : agenda) {
                    RuntimeRuleImpl rule = (RuntimeRuleImpl) candidate;
                    if (activationManager.test(candidate)) {
                        activationManager.onActivation(rule, rule.executeRhs());
                    }
                }
                commitBuffer();
            }
        }
    }

    private void fireDefault(ActivationContext ctx) {
        List<RuntimeRule> agenda;
        while (fireCriteria.getAsBoolean() && memoryActionListener.hasData()) {
            processBuffer();
            if (!(agenda = buildMemoryDeltas()).isEmpty()) {
                activationManager.onAgenda(ctx.incrementFireCount(), agenda);
                for (RuntimeRule candidate : agenda) {
                    RuntimeRuleImpl rule = (RuntimeRuleImpl) candidate;
                    if (activationManager.test(candidate)) {
                        activationManager.onActivation(rule, rule.executeRhs());
                        // Analyzing buffer
                        int deltaOperations = memoryActionListener.deltaOperations();
                        if (deltaOperations > 0) {
                            // Breaking the agenda
                            break;
                        } else {
                            // Processing deletes if any
                            processBuffer();
                        }
                    }
                }
                commitBuffer();
            }
        }
    }

    private void processBuffer() {
        Iterator<TypeMemory> it = memory.iterator();
        getExecutor().invoke(new MemoryDeltaTask(it));
        memoryActionListener.clear();
    }

    private void commitBuffer() {
        memory.commitBuffer();
    }
}
