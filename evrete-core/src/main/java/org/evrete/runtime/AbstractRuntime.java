package org.evrete.runtime;

import org.evrete.Configuration;
import org.evrete.api.*;
import org.evrete.runtime.async.Completer;
import org.evrete.runtime.async.ForkJoinExecutor;
import org.evrete.runtime.builder.RuleBuilderImpl;
import org.evrete.runtime.evaluation.AlphaBucketMeta;
import org.evrete.runtime.evaluation.AlphaConditions;
import org.evrete.runtime.evaluation.AlphaDelta;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public abstract class AbstractRuntime<C extends RuntimeContext<C>> implements RuntimeContext<C> {
    private final List<RuleBuilder<C>> ruleBuilders = new ArrayList<>();
    private final List<RuleDescriptor> ruleDescriptors;
    private final AtomicInteger ruleCounter;
    private final RuntimeListeners listeners;

    private final Configuration configuration;
    private TypeResolver typeResolver;
    private final AlphaConditions alphaConditions;
    private final ForkJoinExecutor executor;
    private final ActiveFields activeFields;
    private final Queue<Completer> tasksQueue = new LinkedList<>();


    private Comparator<Rule> ruleComparator = SALIENCE_COMPARATOR;

    private Class<? extends ActivationManager> activationManagerFactory;

    protected abstract void onNewActiveField(ActiveField newField);

    protected abstract void onNewAlphaBucket(AlphaDelta alphaDelta);

    /**
     * Constructor for a Knowledge object
     *
     * @param configuration global config
     */
    AbstractRuntime(Configuration configuration, ForkJoinExecutor executor) {
        this.configuration = configuration;
        this.typeResolver = configuration.getResolverService().newInstance();
        this.ruleCounter = new AtomicInteger();
        this.alphaConditions = new AlphaConditions();
        this.ruleDescriptors = new ArrayList<>();
        this.listeners = new RuntimeListeners();
        this.executor = executor;
        this.activeFields = new ActiveFields();
        this.activationManagerFactory = UnconditionalActivationManager.class;
    }

    /**
     * Constructor for a Session object
     *
     * @param parent parent context
     */
    protected AbstractRuntime(AbstractRuntime<?> parent) {
        this.configuration = parent.configuration;
        this.typeResolver = parent.typeResolver.copyOf();
        this.ruleCounter = new AtomicInteger(parent.ruleCounter.intValue());
        this.alphaConditions = parent.alphaConditions.copyOf();
        this.ruleDescriptors = new ArrayList<>(parent.ruleDescriptors);
        this.executor = parent.executor;
        this.listeners = parent.listeners.copyOf();
        this.activeFields = parent.activeFields.copyOf();
        this.ruleComparator = parent.ruleComparator;
        this.activationManagerFactory = parent.activationManagerFactory;
    }

    @Override
    public <A extends ActivationManager> void setActivationManagerFactory(Class<A> managerClass) {
        this.activationManagerFactory = managerClass;
    }

    @Override
    public Class<? extends ActivationManager> getActivationManagerFactory() {
        return activationManagerFactory;
    }

    protected ActivationManager newActivationManager() {
        try {
            return activationManagerFactory.getConstructor().newInstance();
        } catch (Throwable e) {
            throw new RuntimeException("Unable to create activation manager");
        }
    }

    protected void queueTask(Completer task) {
        this.tasksQueue.add(task);
    }

    protected void processAllTasks() {
        Completer task;
        while ((task = tasksQueue.poll()) != null) {
            executor.invoke(task);
        }
    }

    @Override
    public Comparator<Rule> getRuleComparator() {
        return ruleComparator;
    }

    @Override
    public void setRuleComparator(Comparator<Rule> ruleComparator) {
        this.ruleComparator = ruleComparator;
    }

    protected ForkJoinExecutor getExecutor() {
        return executor;
    }

    public Evaluator compile(String expression, Function<String, NamedType> resolver) {
        return configuration.getExpressionsService().buildExpression(expression, resolver);
    }

    @Override
    public void wrapTypeResolver(TypeResolverWrapper wrapper) {
        this.typeResolver = wrapper;
    }

    public ActiveField getCreateActiveField(TypeField field) {
        return activeFields.getCreate(field, this::onNewActiveField);
    }

    public ActiveField[] getActiveFields(Type type) {
        return activeFields.getActiveFields(type);
    }

    AlphaBucketMeta getCreateAlphaMask(FieldsKey fields, boolean beta, Set<Evaluator> typePredicates) {
        return alphaConditions.register(this, fields, beta, typePredicates, this::onNewAlphaBucket);
    }

    @Override
    public RuntimeListeners getListeners() {
        return listeners;
    }

    public List<RuleDescriptor> getRuleDescriptors() {
        return ruleDescriptors;
    }

    public AlphaConditions getAlphaConditions() {
        return alphaConditions;
    }

    @Override
    public RuleBuilder<C> newRule() {
        return newRule(RuleBuilderImpl.class.getName() + "#" + ruleCounter.get());
    }

    @Override
    public RuleBuilderImpl<C> newRule(String name) {
        RuleBuilderImpl<C> rb = new RuleBuilderImpl<>(this, name, -1 * ruleCounter.getAndIncrement());
        this.ruleBuilders.add(rb);
        return rb;
    }

    @Override
    public boolean ruleExists(String name) {
        Objects.requireNonNull(name);
        return Named.find(this.ruleDescriptors, name) != null;
    }

    @Override
    public RuleDescriptor getRuleDescriptor(String name) {
        return Named.find(ruleDescriptors, name);
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    public <Z> KeysStore newKeysStore(Z[][] grouping) {
        return configuration.getCollectionsService().newKeyStore(grouping);
    }

    public KeysStore newKeysStore(int[] factTypeCounts) {
        return configuration.getCollectionsService().newKeyStore(factTypeCounts);
    }

    @Override
    public final synchronized RuleDescriptor compileRule(RuleBuilder<?> ruleBuilder) {
        if (!this.ruleBuilders.remove(ruleBuilder)) {
            throw new IllegalArgumentException("No such rule builder");
        } else {
            if (ruleExists(ruleBuilder.getName())) {
                throw new IllegalArgumentException("Rule '" + ruleBuilder.getName() + "' already exists");
            } else {
                RuleBuilderImpl<?> rb = (RuleBuilderImpl<?>) ruleBuilder;
                RuleDescriptor rd = new RuleDescriptor(this, rb);
                this.ruleDescriptors.add(rd);
                return rd;
            }
        }
    }

    @Override
    public TypeResolver getTypeResolver() {
        return typeResolver;
    }

    @Override
    public void addConditionTestListener(EvaluationListener listener) {
        listeners.addConditionTestListener(listener);
    }
}
