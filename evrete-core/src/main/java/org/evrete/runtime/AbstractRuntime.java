package org.evrete.runtime;

import org.evrete.Configuration;
import org.evrete.KnowledgeService;
import org.evrete.api.*;
import org.evrete.runtime.async.ForkJoinExecutor;
import org.evrete.runtime.builder.RuleBuilderImpl;
import org.evrete.runtime.evaluation.MemoryAddress;
import org.evrete.util.DefaultActivationManager;
import org.evrete.util.compiler.CompilationException;

import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

public abstract class AbstractRuntime<R extends Rule, C extends RuntimeContext<C>> extends RuntimeMetaData<C> implements RuleSet<R> {
    private final List<RuleBuilder<C>> ruleBuilders = new ArrayList<>();

    private final KnowledgeService service;
    private ExpressionResolver expressionResolver;
    private Comparator<Rule> ruleComparator = SALIENCE_COMPARATOR;
    private Class<? extends ActivationManager> activationManagerFactory;
    private ActivationMode agendaMode;
    private final Configuration configuration;

    AbstractRuntime(KnowledgeService service) {
        this(service, service.newTypeResolver());
    }

    AbstractRuntime(KnowledgeService service, TypeResolver typeResolver) {
        super(service, typeResolver);
        this.configuration = service.getConfiguration().copyOf();
        this.service = service;
        this.activationManagerFactory = DefaultActivationManager.class;
        this.agendaMode = ActivationMode.DEFAULT;
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
    }

    ActivationMode getAgendaMode() {
        return agendaMode;
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

    public Evaluator compile(String expression, NamedType.Resolver resolver) {
        _assertActive();
        try {
            return getExpressionResolver().buildExpression(expression, resolver, getJavaImports(RuleScope.LHS, RuleScope.BOTH));
        } catch (CompilationException e) {
            Logger.getAnonymousLogger().warning("Failed code:\n" + e.getSource());
            throw new RuntimeException(e);
        }
    }

    public Evaluator compile(String expression, NamedType.Resolver resolver, Imports imports) {
        _assertActive();
        try {
            return getExpressionResolver().buildExpression(expression, resolver, imports.get(RuleScope.LHS, RuleScope.BOTH));
        } catch (CompilationException e) {
            Logger.getAnonymousLogger().warning("Failed code:\n" + e.getSource());
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
        return newRule(null);
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

            if (ruleName == null) {
                ruleName = "Rule#" + currentRuleCount;
            }

            if (ruleExists(ruleBuilder.getName())) {
                throw new IllegalArgumentException("Rule '" + ruleBuilder.getName() + "' already exists");
            } else {
                RuleBuilderImpl<?> rb = (RuleBuilderImpl<?>) ruleBuilder;
                return RuleDescriptor.factory(this, rb, ruleName, salience);
            }
        }
    }

    Consumer<RhsContext> compile(String literalRhs, Collection<NamedType> namedTypes, Imports imports, RuleScope... scopes) {
        _assertActive();
        try {
            return service.getLiteralRhsCompiler().compileRhs(this, literalRhs, namedTypes, imports.get(scopes));
        } catch (CompilationException e) {
            Logger.getAnonymousLogger().warning("Failed source\n: " + e.getSource());
            throw new IllegalStateException(e);
        }
    }

    private FieldReference resolveFieldReference(String arg, NamedType.Resolver typeMapper) {
        _assertActive();
        return getExpressionResolver().resolve(arg, typeMapper);
    }

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
