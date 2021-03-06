package org.evrete.runtime;

import org.evrete.Configuration;
import org.evrete.KnowledgeService;
import org.evrete.api.*;
import org.evrete.api.spi.LiteralRhsCompiler;
import org.evrete.runtime.async.ForkJoinExecutor;
import org.evrete.runtime.builder.FactTypeBuilder;
import org.evrete.runtime.builder.RuleBuilderImpl;
import org.evrete.runtime.evaluation.AlphaBucketMeta;
import org.evrete.runtime.evaluation.EvaluatorWrapper;
import org.evrete.util.DefaultActivationManager;
import org.evrete.util.LazyInstance;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class AbstractRuntime<C extends RuntimeContext<C>> extends RuntimeMetaData<C> {
    private final List<RuleBuilder<C>> ruleBuilders = new ArrayList<>();
    private final List<RuleDescriptor> ruleDescriptors;
    private final AtomicInteger ruleCounter;

    //private final AlphaConditions alphaConditions;
    private final KnowledgeService service;
    private final LazyInstance<ExpressionResolver> expressionResolver = new LazyInstance<>(this::newExpressionResolver);
    private final LazyInstance<TypeResolver> typeResolver = new LazyInstance<>(this::newTypeResolver);
    private final LazyInstance<LiteralRhsCompiler> rhsCompiler = new LazyInstance<>(this::newLiteralLhsProvider);
    private final AbstractRuntime<?> parent;
    private ClassLoader classLoader;
    private Comparator<Rule> ruleComparator = SALIENCE_COMPARATOR;
    private Class<? extends ActivationManager> activationManagerFactory;
    private ActivationMode agendaMode;

    /**
     * Constructor for a Knowledge object
     *
     * @param service knowledge service
     */
    AbstractRuntime(KnowledgeService service) {
        super();
        this.parent = null;
        this.ruleCounter = new AtomicInteger();
        //this.alphaConditions = new AlphaConditions();
        this.ruleDescriptors = new ArrayList<>();
        this.service = service;
        this.activationManagerFactory = DefaultActivationManager.class;
        this.classLoader = service.getClassLoader();
        this.agendaMode = ActivationMode.DEFAULT;
    }

    /**
     * Constructor for a Session object
     *
     * @param parent parent context
     */
    AbstractRuntime(AbstractRuntime<?> parent) {
        super(parent);
        this.parent = parent;
        this.ruleCounter = new AtomicInteger(parent.ruleCounter.intValue());
        //this.alphaConditions = parent.alphaConditions.copyOf();
        this.ruleDescriptors = new ArrayList<>(parent.ruleDescriptors);
        this.service = parent.service;
        this.ruleComparator = parent.ruleComparator;
        this.activationManagerFactory = parent.activationManagerFactory;
        this.classLoader = parent.classLoader;
        this.agendaMode = parent.agendaMode;
    }

    protected abstract TypeResolver newTypeResolver();


    ActivationMode getAgendaMode() {
        return agendaMode;
    }

    @Override
    @SuppressWarnings("unchecked")
    public C setActivationMode(ActivationMode agendaMode) {
        this.agendaMode = agendaMode;
        return (C) this;
    }

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    @Override
    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    KnowledgeService getService() {
        return service;
    }

    @Override
    public AbstractRuntime<?> getParentContext() {
        return parent;
    }

    @Override
    public final void wrapTypeResolver(TypeResolverWrapper wrapper) {
        this.typeResolver.set(wrapper);
    }

    @Override
    public final TypeResolver getTypeResolver() {
        return typeResolver.get();
    }

    @Override
    public Class<? extends ActivationManager> getActivationManagerFactory() {
        return activationManagerFactory;
    }

    @Override
    public <A extends ActivationManager> void setActivationManagerFactory(Class<A> managerClass) {
        this.activationManagerFactory = managerClass;
    }

    @SuppressWarnings("unchecked")
    @Override
    public final void setActivationManagerFactory(String managerClass) {
        try {
            Class<? extends ActivationManager> factory = (Class<? extends ActivationManager>) Class.forName(managerClass, true, classLoader);
            setActivationManagerFactory(factory);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(managerClass);
        }
    }

    ActivationManager newActivationManager() {
        try {
            return activationManagerFactory.getDeclaredConstructor().newInstance();
        } catch (Throwable e) {
            throw new RuntimeException("Unable to create activation manager. Probably the provided factory class has no public and zero-argument constructor.", e);
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

    ForkJoinExecutor getExecutor() {
        return service.getExecutor();
    }

    public Evaluator compile(String expression, Function<String, NamedType> resolver) {
        return getExpressionResolver().buildExpression(expression, resolver);
    }

    FactType buildFactType(FactTypeBuilder builder, Set<EvaluatorWrapper> alphaEvaluators, int inRuleId) {
        FieldsKey fieldsKey = getCreateMemoryKey(builder);
        AlphaBucketMeta alphaMask = buildAlphaMask(fieldsKey, alphaEvaluators);
        return new FactType(builder.getVar(), alphaMask, fieldsKey, inRuleId);
    }

    @Override
    public List<RuleDescriptor> getRuleDescriptors() {
        return ruleDescriptors;
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
    public Configuration getConfiguration() {
        return service.getConfiguration();
    }

    private ExpressionResolver newExpressionResolver() {
        return service.getExpressionResolverProvider().instance(this);
    }

    private LiteralRhsCompiler newLiteralLhsProvider() {
        return service.getLiteralRhsProvider();
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
                RuleDescriptor rd = RuleDescriptor.factory(this, rb);
                this.ruleDescriptors.add(rd);
                return rd;
            }
        }
    }

    Consumer<RhsContext> compile(String literalRhs, FactType[] factTypes, Collection<String> imports) {
        return rhsCompiler.get().compileRhs(this, literalRhs, Arrays.asList(factTypes), imports);
    }

    public ExpressionResolver getExpressionResolver() {
        return expressionResolver.get();
    }

}
