package org.evrete.runtime;

import org.evrete.Configuration;
import org.evrete.api.*;
import org.evrete.runtime.async.RuleHotDeploymentTask;
import org.evrete.runtime.evaluation.MemoryAddress;
import org.evrete.util.SessionCollector;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collector;

/**
 * <p>
 * Base session class with common methods
 * </p>
 *
 * @param <S> session type parameter
 */
public abstract class AbstractRuleSession<S extends RuleSession<S>> extends AbstractRuntime<RuntimeRule, S> implements RuleSession<S> {
    private static final Logger LOGGER = Logger.getLogger(AbstractRuleSession.class.getName());
    final List<SessionLifecycleListener> lifecycleListeners = new ArrayList<>();
    final SessionMemory memory;
    final RuntimeRules ruleStorage;
    final FactActionBuffer actionBuffer;
    private final boolean warnUnknownTypes;
    private final KnowledgeRuntime knowledge;
    ActivationManager activationManager;
    private BooleanSupplier fireCriteria = () -> true;
    private volatile boolean active = true;

    AbstractRuleSession(KnowledgeRuntime knowledge) {
        super(knowledge);
        this.knowledge = knowledge;
        this.warnUnknownTypes = knowledge.getConfiguration().getAsBoolean(Configuration.WARN_UNKNOWN_TYPES);
        this.activationManager = newActivationManager();
        this.actionBuffer = newActionBuffer();

        this.ruleStorage = new RuntimeRules();
        MemoryFactory memoryFactory = getService().getMemoryFactoryProvider().instance(this);
        this.memory = new SessionMemory(this, memoryFactory);
        // Deploy existing rules
        deployRules(knowledge.getRules(), false);
    }

    static void bufferUpdate(FactHandle handle, FactRecord previous, Object updatedFact, FactActionBuffer buffer) {
        buffer.newUpdate(handle, previous, updatedFact);
    }

    static void bufferDelete(FactHandle handle, FactRecord previous, FactActionBuffer buffer) {
        buffer.newDelete(handle, previous);
    }

    private static Optional<Collection<?>> resolveCollection(Object o, boolean resolveCollection) {
        if (!resolveCollection) {
            return Optional.empty();
        }

        if (o.getClass().isArray()) {
            return Optional.of(Arrays.asList((Object[]) o));
        } else if (o instanceof Iterable) {
            Collection<Object> ret = new LinkedList<>();
            ((Iterable<?>) o).forEach((Consumer<Object>) ret::add);
            return Optional.of(ret);
        } else {
            return Optional.empty();
        }
    }

    protected abstract S thisInstance();

    FactActionBuffer newActionBuffer() {
        return new FactActionBuffer();
    }

    boolean fireCriteriaMet() {
        return this.fireCriteria.getAsBoolean();
    }

    private void applyFireCriteria(BooleanSupplier fireCriteria) {
        this.fireCriteria = fireCriteria;
    }

    @Override
    public S setActivationManager(ActivationManager activationManager) {
        this.activationManager = activationManager;
        return thisInstance();
    }

    @Override
    public S setExecutionPredicate(BooleanSupplier criteria) {
        applyFireCriteria(criteria);
        return thisInstance();
    }

    @Override
    public final ActivationManager getActivationManager() {
        return activationManager;
    }

    @Override
    public final S addEventListener(SessionLifecycleListener listener) {
        this.lifecycleListeners.add(listener);
        return thisInstance();
    }

    @Override
    public final S removeEventListener(SessionLifecycleListener listener) {
        this.lifecycleListeners.remove(listener);
        return thisInstance();
    }

    @SuppressWarnings("unchecked")
    public final <T> T getFact(FactHandle handle) {
        FactRecord rec = getFactRecord(handle);
        return rec == null ? null : (T) rec.instance;
    }

    final FactRecord getFactRecord(FactHandle handle) {
        AtomicMemoryAction bufferedAction = actionBuffer.find(handle);
        FactRecord found;
        if (bufferedAction == null) {
            found = memory.get(handle.getTypeId()).getFactRecord(handle);
        } else {
            found = bufferedAction.action == Action.RETRACT ? null : bufferedAction.getDelta().getLatest();
        }
        return found;
    }

    @Override
    final void _assertActive() {
        if (!active) {
            throw new IllegalStateException("Session has been closed");
        }
    }

    @Override
    public KnowledgeRuntime getParentContext() {
        return knowledge;
    }

    private void deployRules(Collection<RuleDescriptor> descriptors, boolean hotDeployment) {
        for(RuleDescriptor rd : descriptors) {
            deployRule(rd, hotDeployment);
        }
    }

    private synchronized void deployRule(RuleDescriptor descriptor, boolean hotDeployment) {
        for (FactType factType : descriptor.getLhs().getFactTypes()) {
            TypeMemory tm = memory.getCreateUpdate(factType.type());
            tm.touchMemory(factType.getMemoryAddress());
        }
        RuntimeRuleImpl rule = ruleStorage.addRule(descriptor, this);
        if (hotDeployment) {
            getExecutor().invoke(new RuleHotDeploymentTask(rule));
        }
        reSortRules();
    }

    private void reSortRules() {
        ruleStorage.sort(getRuleComparator());
    }


    @Override
    void addRuleDescriptors(List<RuleDescriptor> descriptors) {
        deployRules(descriptors, true);
    }

    @Override
    public void setRuleComparator(Comparator<Rule> ruleComparator) {
        super.setRuleComparator(ruleComparator);
        reSortRules();
    }

    public final SessionMemory getMemory() {
        return memory;
    }

    final void forEachFactFull(BiConsumer<FactHandle, Object> consumer) {
        Set<FactHandle> buffered = new HashSet<>();
        this.actionBuffer.forEach(a -> {
            FactHandle handle = a.handle;
            buffered.add(handle);
            if (a.action != Action.RETRACT) {
                consumer.accept(handle, a.getDelta().getLatest().instance);
            }
        });

        this.forEachFactCommitted((handle, o) -> {
            // Skip already served facts
            if (!buffered.contains(handle)) {
                consumer.accept(handle, o);
            }
        });
    }

    /**
     * <p>
     * Scans committed working memory only, no buffered actions get scanned
     * </p>
     *
     * @param consumer consumer
     */
    private void forEachFactCommitted(BiConsumer<FactHandle, Object> consumer) {
        // Scanning main memory and making sure fact handles are not deleted
        memory.forEach(tm -> tm.forEachFact(consumer));
    }

    @SuppressWarnings("unchecked")
    <T> void forEachFactFull(String type, Consumer<T> consumer) {
        Type<?> t = getTypeResolver().getType(type);
        if (t == null) {
            LOGGER.warning("Type not found: '" + type + "'");
            return;
        }

        Set<FactHandle> buffered = new HashSet<>();
        this.actionBuffer.forEach(t, a -> {
            FactHandle handle = a.handle;
            buffered.add(handle);
            if (a.action != Action.RETRACT) {
                consumer.accept((T) a.getDelta().getLatest().instance);
            }
        });

        forEachFactCommitted(t.getId(), (BiConsumer<FactHandle, T>) (handle, o) -> {
            if (!buffered.contains(handle)) {
                consumer.accept(o);
            }
        });


    }

    @SuppressWarnings("unchecked")
    private <T> void forEachFactCommitted(int t, BiConsumer<FactHandle, T> consumer) {
        memory
                .getCreateUpdate(t)
                .forEachFact((handle, o) -> consumer.accept(handle, (T) o));
    }

    @Override
    public <T> Collector<T, ?, S> asCollector() {
        return new SessionCollector<>(thisInstance());
    }

    @Override
    public final RuntimeRule getRule(String name) {
        return ruleStorage.get(name);
    }

    @Override
    public List<RuntimeRule> getRules() {
        return Collections.unmodifiableList(ruleStorage.getList());
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
        this.actionBuffer.clear();
    }

    abstract void bufferUpdate(FactHandle handle, FactRecord previous, Object fact);

    abstract void bufferDelete(FactHandle handle);

    final FactHandle bufferInsert(Object fact, boolean resolveCollections, FactActionBuffer buffer) {
        _assertActive();
        Object arg = Objects.requireNonNull(fact, "Null facts are not supported");

        Optional<Collection<?>> collection = resolveCollection(arg, resolveCollections);
        if (collection.isPresent()) {
            // Treat the argument as a collection
            for (Object o : collection.get()) {
                Optional<InsertedFact> insertResult = insertAtomic(o);
                insertResult.ifPresent(t -> buffer.newInsert(t.handle, t.record));
            }
            return null;
        } else {
            // Treat the argument as a single fact
            Optional<InsertedFact> insertResult = insertAtomic(arg);
            insertResult.ifPresent(t -> buffer.newInsert(t.handle, t.record));
            return insertResult.map(t -> t.handle).orElse(null);
        }
    }

    final FactHandle bufferInsert(Object fact, String namedType, boolean resolveCollections, FactActionBuffer buffer) {
        _assertActive();
        Object arg = Objects.requireNonNull(fact, "Null facts are not supported");
        final Type<?> type = getType(namedType);
        if (type == null) {
            if (warnUnknownTypes) {
                LOGGER.warning("Can not map type for '" + fact.getClass().getName() + "', insert operation skipped.");
            }
            return null;
        }
        Optional<Collection<?>> collection = resolveCollection(arg, resolveCollections);
        if (collection.isPresent()) {
            // Treat the argument as a collection
            for (Object o : collection.get()) {
                Optional<InsertedFact> insertResult = insertAtomic(type, o);
                insertResult.ifPresent(t -> buffer.newInsert(t.handle, t.record));
            }
            return null;
        } else {
            // Treat the argument as a single fact
            Optional<InsertedFact> insertResult = insertAtomic(type, arg);
            insertResult.ifPresent(t -> buffer.newInsert(t.handle, t.record));
            return insertResult.map(t -> t.handle).orElse(null);
        }
    }

    private Optional<InsertedFact> insertAtomic(Object o) {
        Type<?> type = resolve(o);
        if (type == null) {
            if (warnUnknownTypes) {
                LOGGER.warning("Can not map type for '" + o.getClass().getName() + "', insert operation skipped.");
            }
            return Optional.empty();
        } else {
            return insertAtomic(type, o);
        }
    }


    private Optional<InsertedFact> insertAtomic(Type<?> type, Object o) {
        return memory.get(type).registerInsertedFact(o);
    }
}
