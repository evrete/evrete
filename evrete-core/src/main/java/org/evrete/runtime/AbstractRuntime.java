package org.evrete.runtime;

import org.evrete.Configuration;
import org.evrete.KnowledgeService;
import org.evrete.api.*;
import org.evrete.runtime.async.ForkJoinExecutor;
import org.evrete.runtime.evaluation.MemoryAddress;
import org.evrete.util.DefaultActivationManager;
import org.evrete.util.compiler.CompilationException;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Logger;

public abstract class AbstractRuntime<R extends Rule, C extends RuntimeContext<C>> extends RuntimeMetaData<C> implements RuleSet<R>, RuntimeContext<C> {
    private static final Logger LOGGER = Logger.getLogger(AbstractRuntime.class.getName());
    private final List<RuleBuilder<C>> ruleBuilders = new ArrayList<>();

    private final KnowledgeService service;
    private final Configuration configuration;
    private final AtomicInteger noNameRuleCounter;
    private ExpressionResolver expressionResolver;
    private Comparator<Rule> ruleComparator = SALIENCE_COMPARATOR;
    private Class<? extends ActivationManager> activationManagerFactory;
    private ActivationMode agendaMode;
    private RuleBuilderExceptionHandler ruleBuilderExceptionHandler;

    AbstractRuntime(KnowledgeService service, TypeResolver typeResolver) {
        super(service, typeResolver);
        this.configuration = service.getConfiguration().copyOf();
        this.service = service;
        this.activationManagerFactory = DefaultActivationManager.class;
        this.agendaMode = ActivationMode.DEFAULT;
        this.ruleBuilderExceptionHandler = (context, ruleBuilder, e) -> {
            throw e;
        };
        this.noNameRuleCounter = new AtomicInteger();
    }

    AbstractRuntime(KnowledgeService service) {
        this(service, service.newTypeResolver());
    }

    /**
     * Constructor for a Session object
     *
     * @param parent parent context
     */
    AbstractRuntime(AbstractRuntime<?, ?> parent) {
        super(parent);
        this.configuration = parent.configuration.copyOf();
        this.service = parent.service;
        this.ruleComparator = parent.ruleComparator;
        this.activationManagerFactory = parent.activationManagerFactory;
        this.agendaMode = parent.agendaMode;
        this.expressionResolver = null;
        this.ruleBuilderExceptionHandler = parent.ruleBuilderExceptionHandler;
        this.noNameRuleCounter = parent.noNameRuleCounter;
    }

    protected abstract void addRuleInner(RuleBuilder<?> builder);

    ActivationMode getAgendaMode() {
        return agendaMode;
    }

    @Override
    public final void addRule(RuleBuilder<?> builder) {
        try {
            addRuleInner(builder);
        } catch (RuntimeException e) {
            this.ruleBuilderExceptionHandler.handle(this, builder, e);
        }
    }

    @Override
    public void setRuleBuilderExceptionHandler(RuleBuilderExceptionHandler handler) {
        this.ruleBuilderExceptionHandler = handler;
    }

    @Override
    @SuppressWarnings("unchecked")
    public C setActivationMode(ActivationMode activationMode) {
        _assertActive();
        this.agendaMode = activationMode;
        return (C) this;
    }

    @Override
    public KnowledgeService getService() {
        _assertActive();
        return service;
    }

    @Override
    public Class<? extends ActivationManager> getActivationManagerFactory() {
        _assertActive();
        return activationManagerFactory;
    }

    @Override
    public <A extends ActivationManager> void setActivationManagerFactory(Class<A> managerClass) {
        _assertActive();
        this.activationManagerFactory = managerClass;
    }

    @SuppressWarnings("unchecked")
    @Override
    public final void setActivationManagerFactory(String managerClass) {
        _assertActive();
        try {
            Class<? extends ActivationManager> factory = (Class<? extends ActivationManager>) Class.forName(managerClass, true, getClassLoader());
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
        _assertActive();
        this.ruleComparator = ruleComparator;
    }

    ForkJoinExecutor getExecutor() {
        _assertActive();
        return service.getExecutor();
    }

    Evaluator compileUnchecked(String expression, NamedType.Resolver resolver) {
        try {
            return compile(expression, resolver);
        } catch (CompilationException e) {
            throw new RuntimeException(e);
        }
    }

    FactType buildFactType(NamedType builder, Set<TypeField> fields, Set<EvaluatorHandle> alphaEvaluators, int inRuleId) {
        _assertActive();
        MemoryAddress memoryAddress = buildMemoryAddress(builder.getType(), fields, alphaEvaluators);
        return new FactType(builder.getName(), memoryAddress, inRuleId);
    }

    @Override
    public RuleBuilder<C> newRule() {
        return newRule("rule_" + noNameRuleCounter.incrementAndGet());
    }

    @Override
    public RuleBuilderImpl<C> newRule(String name) {
        _assertActive();
        RuleBuilderImpl<C> rb = new RuleBuilderImpl<>(this, name);
        this.ruleBuilders.add(rb);
        return rb;
    }

    @Override
    public Configuration getConfiguration() {
        return this.configuration;
    }

    synchronized RuleDescriptor compileRuleBuilder(RuleBuilder<?> ruleBuilder) {
        _assertActive();
        if (!this.ruleBuilders.remove(ruleBuilder)) {
            throw new IllegalArgumentException("No such rule builder");
        } else {
            int currentRuleCount = getRules().size();
            String ruleName = ruleBuilder.getName();
            int salience = ruleBuilder.getSalience();
            if (salience == RuleBuilderImpl.NULL_SALIENCE) {
                salience = -1 * (currentRuleCount + 1);
            }

            if (ruleExists(ruleBuilder.getName())) {
                throw new IllegalArgumentException("Rule '" + ruleBuilder.getName() + "' already exists");
            } else {
                RuleBuilderImpl<?> rb = (RuleBuilderImpl<?>) ruleBuilder;
                return RuleDescriptor.factory(this, rb, ruleName, salience);
            }
        }
    }

    Consumer<RhsContext> compile(String literalRhs, Collection<NamedType> namedTypes) {
        _assertActive();
        try {
            return service.getLiteralRhsCompiler().compileRhs(this, literalRhs, namedTypes);
        } catch (CompilationException e) {
            Logger.getAnonymousLogger().warning("Failed source\n: " + e.getSource());
            throw new IllegalStateException(e);
        }
    }

    private FieldReference resolveFieldReference(String arg, NamedType.Resolver typeMapper) {
        _assertActive();
        return getExpressionResolver().resolve(arg, typeMapper);
    }

    @Override
    public FieldReference[] resolveFieldReferences(String[] arg, NamedType.Resolver typeMapper) {
        _assertActive();
        FieldReference[] refs = new FieldReference[arg.length];
        for (int i = 0; i < arg.length; i++) {
            refs[i] = resolveFieldReference(arg[i], typeMapper);
        }
        return refs;
    }

    @Override
    public final synchronized ExpressionResolver getExpressionResolver() {
        _assertActive();
        if (expressionResolver == null) {
            expressionResolver = service.getExpressionResolverProvider().instance(this);
        }
        return expressionResolver;
    }

    abstract void _assertActive();
}
