package org.evrete.runtime;

import org.evrete.Configuration;
import org.evrete.api.*;
import org.evrete.runtime.async.ForkJoinExecutor;
import org.evrete.runtime.builder.RuleBuilderImpl;
import org.evrete.runtime.structure.RuleDescriptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
    }

    public boolean getRuleBuilders(RuleBuilder<?> builder) {
        return ruleBuilders.remove(builder);
    }

    public ForkJoinExecutor getExecutor() {
        return executor;
    }

    public Evaluator compile(String expression, Function<String, NamedType> resolver) {
        return configuration.getExpressionsService().buildExpression(expression, resolver);
    }

    private RuleBuilderImpl<C> reportNewRuleCreated(RuleBuilderImpl<C> rule) {
        this.ruleCounter.incrementAndGet();
        this.ruleBuilders.add(rule);
        return rule;
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

    public AlphaBucketMeta getCreateAlphaMask(FieldsKey fields, boolean beta, Set<Evaluator> typePredicates) {
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
        return reportNewRuleCreated(new RuleBuilderImpl<>(this, name));
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
