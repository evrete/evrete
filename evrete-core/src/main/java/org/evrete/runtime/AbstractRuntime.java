package org.evrete.runtime;

import org.evrete.Configuration;
import org.evrete.KnowledgeService;
import org.evrete.api.*;
import org.evrete.api.builders.RuleSetBuilder;
import org.evrete.runtime.async.ForkJoinExecutor;
import org.evrete.runtime.compiler.CompilationException;
import org.evrete.runtime.compiler.RuntimeClassloader;
import org.evrete.runtime.compiler.SourceCompiler;
import org.evrete.runtime.evaluation.MemoryAddress;
import org.evrete.util.DefaultActivationManager;
import org.evrete.util.WorkUnitObject;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public abstract class AbstractRuntime<R extends Rule, C extends RuntimeContext<C>> extends RuntimeMetaData<C> implements RuleSetContext<C, R> {
    private static final Logger LOGGER = Logger.getLogger(AbstractRuntime.class.getName());
    private final List<RuleBuilder<C>> ruleBuilders = new ArrayList<>();

    private final KnowledgeService service;
    private final Configuration configuration;
    private final AtomicInteger noNameRuleCounter;
    private ExpressionResolver expressionResolver;
    private Comparator<Rule> ruleComparator = SALIENCE_COMPARATOR;
    private Class<? extends ActivationManager> activationManagerFactory;
    private ActivationMode agendaMode;
    private RuntimeClassloader classloader;

    AbstractRuntime(KnowledgeService service, TypeResolver typeResolver) {
        super(service, typeResolver);
        this.configuration = service.getConfiguration().copyOf();
        this.classloader = new RuntimeClassloader(service.getClassLoader());
        this.service = service;
        this.activationManagerFactory = DefaultActivationManager.class;
        this.agendaMode = ActivationMode.DEFAULT;
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
        this.noNameRuleCounter = parent.noNameRuleCounter;
        this.classloader = new RuntimeClassloader(parent.classloader);
    }

    protected abstract void addRuleInner(RuleBuilder<?> builder) throws CompilationException;

    abstract void addRuleDescriptors(List<RuleDescriptor> descriptors);

    ActivationMode getAgendaMode() {
        return agendaMode;
    }

    @Override
    public final void addRule(RuleBuilder<?> builder) {
        try {
            addRuleInner(builder);
        } catch (RuntimeException e) {
            //TODO
        } catch (CompilationException e) {
            e.log(LOGGER, Level.WARNING);
        }
    }

    void addRules(List<DefaultRuleBuilder<C>> rules) {
        // TODO exception handlers!!
        List<RuleDescriptor> descriptors = compileRuleBuilders(rules);
        addRuleDescriptors(descriptors);
    }

    @Override
    public final RuntimeClassloader getClassLoader() {
        return classloader;
    }

    @Override
    public void setClassLoader(ClassLoader classLoader) {
        this.classloader = new RuntimeClassloader(classLoader);
    }

    @Override
    public final JavaSourceCompiler getSourceCompiler() {
        return new SourceCompiler(classloader);
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

    FactType buildFactType(NamedType builder, Set<TypeField> fields, Set<EvaluatorHandle> alphaEvaluators, int inRuleId) {
        _assertActive();
        MemoryAddress memoryAddress = buildMemoryAddress(builder.getType(), fields, alphaEvaluators);
        return new FactType(builder.getName(), memoryAddress, inRuleId);
    }

    @Override
    public RuleBuilder<C> newRule() {
        return newRule(unnamedRuleName());
    }

    String unnamedRuleName() {
        return "rule_" + noNameRuleCounter.incrementAndGet();
    }

    @Override
    public RuleBuilder<C> newRule(String name) {
        _assertActive();
        RuleBuilderImpl<C> rb = new RuleBuilderImpl<>(this, name);
        this.ruleBuilders.add(rb);
        return rb;
    }

    @Override
    public RuleSetBuilder<C> builder() {
        _assertActive();
        return new DefaultRuleSetBuilder<>(this);
    }

    @Override
    public Configuration getConfiguration() {
        return this.configuration;
    }

    synchronized RuleDescriptor compileRuleBuilder(RuleBuilderImpl<?> ruleBuilder) throws CompilationException {
        Function<RuleBuilderImpl<?>, LhsConditionHandles> func = compileConditions(Collections.singletonList(ruleBuilder));
        return compileRuleBuilder(ruleBuilder, func);
    }

    synchronized List<RuleDescriptor> compileRuleBuilders(List<DefaultRuleBuilder<C>> rules) {

        // Collect sources to compile
        List<DefaultRuleLiteralSources> ruleLiteralSources = rules.stream()
                .map(DefaultRuleLiteralSources::new)
                .filter(DefaultRuleLiteralSources::nonEmpty)
                .collect(Collectors.toList());
        try {
            return compileRuleBuilders(rules, this.compileRules(ruleLiteralSources));
        } catch (CompilationException e) {
            //TODO !!!! exception handler
            throw new RuntimeException(e);
        }
    }

    private List<RuleDescriptor> compileRuleBuilders(List<DefaultRuleBuilder<C>> rules, Collection<RuleCompiledSources<DefaultRuleLiteralSources, DefaultRuleBuilder<?>>> compiled) {
        // Finally we have all we need to create descriptor for each rule: compiled classes and original data in rule builders
        int currentRuleCount = getRules().size();

        Map<DefaultRuleBuilder<?>, RuleCompiledSources<?,?>> mapping = new IdentityHashMap<>();
        for(RuleCompiledSources<DefaultRuleLiteralSources,?> entry : compiled) {
            DefaultRuleBuilder<?> ruleBuilder = entry.getSources().getRule();
            mapping.put(ruleBuilder, entry);
        }

        List<RuleDescriptor> descriptors = new ArrayList<>(rules.size());
        for (DefaultRuleBuilder<C> ruleBuilder : rules) {
            // 1. Check existing rules
            if (ruleExists(ruleBuilder.getName())) {
                throw new IllegalArgumentException("Rule '" + ruleBuilder.getName() + "' already exists");
            }

            // 2. Compute salience
            int salience = ruleBuilder.getSalience();
            if (salience == RuleBuilderImpl.NULL_SALIENCE) {
                salience = -1 * (currentRuleCount + 1);
            }

            // 3. Register condition handles
            LhsConditionHandles handles = new LhsConditionHandles();

            LhsConditions builderConditions = ruleBuilder.getLhs().getConditions();

            // 3.1 Register direct conditions (they're already stored in the context)
            for(EvaluatorHandle handle : builderConditions.directHandles) {
                handles.add(handle);
            }

            // 3.2 Register functional conditions
            for(WorkUnitObject<Evaluator> meta : builderConditions.evaluators) {
                EvaluatorHandle handle = addEvaluator(meta.getDelegate(), meta.getComplexity());
                handles.add(handle);
            }

            // 3.3 Register literal conditions
            Collection<WorkUnitObject<LiteralExpression>> literalConditions = builderConditions.literals;
            if(!literalConditions.isEmpty()) {
                // There must be compiled copies for each literal condition
                RuleCompiledSources<?,?> compiledData = mapping.get(ruleBuilder);

                if(compiledData == null) {
                    throw new IllegalStateException("No compiled data for literal sources");
                }

                // Create
                Map<LiteralExpression, LiteralEvaluator> conditionMap = new IdentityHashMap<>();
                for(LiteralEvaluator evaluator : compiledData.conditions()) {
                    conditionMap.put(evaluator.getSource(), evaluator);
                }


                for(WorkUnitObject<LiteralExpression> meta : literalConditions) {
                    LiteralExpression expression = meta.getDelegate();
                    double complexity = meta.getComplexity();
                    LiteralEvaluator evaluator = conditionMap.get(expression);
                    if(evaluator == null) {
                        throw new IllegalStateException("Compiled condition not found for " + expression.getSource());
                    } else {
                        EvaluatorHandle handle = addEvaluator(evaluator, complexity);
                        handles.add(handle);
                    }
                }
            }


            // Build the descriptor and append it to the result
            RuleDescriptor descriptor = RuleDescriptor.factory(this, ruleBuilder, handles, salience);
            descriptors.add(descriptor);

            currentRuleCount++;
        }

        return descriptors;
    }

    <T extends LhsConditionsHolder> Function<T, LhsConditionHandles> compileConditions(Collection<T> sources) throws CompilationException {
        Map<T, LhsConditionHandles> map = new IdentityHashMap<>();

        Map<LiteralExpression, WorkUnitObject<T>> allLiteralExpressions = new IdentityHashMap<>();
        for (T source : sources) {
            LhsConditions conditions = source.getConditions();
            for (WorkUnitObject<LiteralExpression> e : conditions.literals) {
                allLiteralExpressions.put(e.getDelegate(), new WorkUnitObject<>(source, e.getComplexity()));
            }
        }

        Collection<LiteralExpression> allSources = allLiteralExpressions.keySet();
        Map<T, Collection<WorkUnitObject<Evaluator>>> perSourceLiterals = new IdentityHashMap<>();

        if (!allSources.isEmpty()) {
            Collection<LiteralEvaluator> allCompiled = getExpressionResolver().buildExpressions(allSources);
            for (LiteralEvaluator e : allCompiled) {
                WorkUnitObject<T> helper = allLiteralExpressions.get(e.getSource());
                if (helper == null) {
                    throw new IllegalStateException("Couldn't find source condition by identity");
                } else {
                    perSourceLiterals.computeIfAbsent(helper.getDelegate(), t -> new LinkedList<>()).add(new WorkUnitObject<>(e, helper.getComplexity()));
                }
            }
        }

        // Build the result
        for (T source : sources) {
            LhsConditionHandles handles = map.computeIfAbsent(source, k -> new LhsConditionHandles());
            LhsConditions conditions = source.getConditions();

            // Add direct handles
            for (EvaluatorHandle d : conditions.directHandles) {
                handles.add(d);
            }

            // Add evaluators
            for (WorkUnitObject<Evaluator> d : conditions.evaluators) {
                EvaluatorHandle h = addEvaluator(d.getDelegate(), d.getComplexity());
                handles.add(h);
            }

            // Add compiled literal evaluators
            Collection<WorkUnitObject<Evaluator>> literals = perSourceLiterals.get(source);
            if (literals != null) {
                for (WorkUnitObject<Evaluator> d : literals) {
                    EvaluatorHandle h = addEvaluator(d.getDelegate(), d.getComplexity());
                    handles.add(h);
                }
            }
        }

        return t -> Objects.requireNonNull(map.get(t), "Illegal state");
    }


    private synchronized RuleDescriptor compileRuleBuilder(RuleBuilderImpl<?> ruleBuilder, Function<RuleBuilderImpl<?>, LhsConditionHandles> lhsConditions) {
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
                throw new IllegalArgumentException("Rule '" + ruleName + "' already exists");
            } else {
                return RuleDescriptor.factory(this, ruleBuilder, lhsConditions.apply(ruleBuilder), salience);
            }
        }
    }

    Consumer<RhsContext> compileRHS(String literalRhs, Collection<NamedType> namedTypes) {
        _assertActive();
        try {
            return service.getLiteralRhsCompiler().compileRhs(this, literalRhs, namedTypes);
        } catch (CompilationException e) {
            e.log(LOGGER, Level.WARNING);
            throw new IllegalStateException(e);
        }
    }

    <S extends RuleLiteralSources<R1>, R1 extends Rule> Collection<RuleCompiledSources<S, R1>> compileRules(Collection<S> sources) throws CompilationException {
        _assertActive();
        return service.getLiteralSourceCompiler().compile(this, sources);
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

    @Override
    public LiteralEvaluator compile(LiteralExpression expression) throws CompilationException {
        return compileRules(Collections.singletonList(new SingleConditionRuleSources(expression)))
                .iterator().next().conditions().iterator().next();
    }
}
