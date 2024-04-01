package org.evrete.runtime;

import org.evrete.Configuration;
import org.evrete.KnowledgeService;
import org.evrete.api.*;
import org.evrete.api.builders.RuleSetBuilder;
import org.evrete.runtime.compiler.RuntimeClassloader;
import org.evrete.runtime.compiler.SourceCompiler;
import org.evrete.util.CompilationException;
import org.evrete.util.DefaultActivationManager;
import org.evrete.util.ForkJoinExecutor;
import org.evrete.util.WorkUnitObject;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public abstract class AbstractRuntime<R extends Rule, C extends RuntimeContext<C>> extends RuntimeMetaData<C> implements RuleSetContext<C, R> {
    private static final Logger LOGGER = Logger.getLogger(AbstractRuntime.class.getName());
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

    abstract void addRuleDescriptors(List<RuleDescriptorImpl> descriptors);

    ActivationMode getAgendaMode() {
        return agendaMode;
    }

    void addRules(List<DefaultRuleBuilder<C>> rules) {
        List<RuleDescriptorImpl> descriptors = compileRuleBuilders(rules);
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
    @Deprecated
    public RuleBuilder<C> newRule() {
        return newRule(unnamedRuleName());
    }

    String unnamedRuleName() {
        return "rule_" + noNameRuleCounter.incrementAndGet();
    }

    @Override
    @Deprecated
    public RuleBuilder<C> newRule(String name) {
        _assertActive();
        return new RuleBuilderImpl<>(this, name);
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

    synchronized List<RuleDescriptorImpl> compileRuleBuilders(List<DefaultRuleBuilder<C>> rules) {

        // Collect sources to compile
        List<DefaultRuleLiteralData> ruleLiteralSources = rules.stream()
                .map(DefaultRuleLiteralData::new)
                .filter(DefaultRuleLiteralData::nonEmpty)
                .collect(Collectors.toList());
        try {
            return compileRuleBuilders(rules, this.compileRules(ruleLiteralSources));
        } catch (CompilationException e) {
            e.log(LOGGER, Level.WARNING);
            throw new RuntimeException(e);
        }
    }

    private List<RuleDescriptorImpl> compileRuleBuilders(List<DefaultRuleBuilder<C>> rules, Collection<RuleCompiledSources<DefaultRuleLiteralData, DefaultRuleBuilder<?>>> compiled) {
        // Finally we have all we need to create descriptor for each rule: compiled classes and original data in rule builders
        int currentRuleCount = getRules().size();

        Map<DefaultRuleBuilder<?>, RuleCompiledSources<?,?>> mapping = new IdentityHashMap<>();
        for (RuleCompiledSources<DefaultRuleLiteralData, ?> entry : compiled) {
            DefaultRuleBuilder<?> ruleBuilder = entry.getSources().getRule();
            mapping.put(ruleBuilder, entry);
        }

        List<RuleDescriptorImpl> descriptors = new ArrayList<>(rules.size());
        for (DefaultRuleBuilder<C> ruleBuilder : rules) {
            // 1. Check existing rules
            if (ruleExists(ruleBuilder.getName())) {
                throw new IllegalArgumentException("Rule '" + ruleBuilder.getName() + "' already exists");
            }

            // 2. Compute salience
            int salience = ruleBuilder.getSalience();
            if (salience == DefaultRuleBuilder.NULL_SALIENCE) {
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
            Collection<WorkUnitObject<String>> literalConditions = builderConditions.literals;
            if(!literalConditions.isEmpty()) {
                // There must be compiled copies for each literal condition
                RuleCompiledSources<?,?> compiledData = mapping.get(ruleBuilder);

                if(compiledData == null) {
                    throw new IllegalStateException("No compiled data for literal sources");
                }

                // Create mapping
                Map<String, LiteralEvaluator> conditionMap = new IdentityHashMap<>();
                for(LiteralEvaluator evaluator : compiledData.conditions()) {
                    conditionMap.put(evaluator.getSource().getSource(), evaluator);
                }

                // Register condition handles
                for (WorkUnitObject<String> meta : literalConditions) {
                    String expression = meta.getDelegate();
                    double complexity = meta.getComplexity();
                    LiteralEvaluator evaluator = conditionMap.get(expression);
                    if(evaluator == null) {
                        throw new IllegalStateException("Compiled condition not found for " + expression);
                    } else {
                        EvaluatorHandle handle = addEvaluator(evaluator, complexity);
                        handles.add(handle);
                    }
                }
            }

            // 4. Handle literal RHS
            String literalRhs = ruleBuilder.getLiteralRhs();
            if (literalRhs != null) {
                RuleCompiledSources<?, ?> compiledData = mapping.get(ruleBuilder);

                if (compiledData == null) {
                    throw new IllegalStateException("No compiled data for literal sources");
                } else {
                    Consumer<RhsContext> compiledRhs = compiledData.rhs();
                    if (compiledRhs == null) {
                        throw new IllegalStateException("No compiled RHS for literal actions");
                    } else {
                        // Assign RHS action
                        ruleBuilder.setRhs(compiledRhs);
                    }
                }
            }


            // Build the descriptor and append it to the result
            RuleDescriptorImpl descriptor = RuleDescriptorImpl.factory(this, ruleBuilder, handles, salience);
            descriptors.add(descriptor);

            currentRuleCount++;
        }

        return descriptors;
    }

    Consumer<RhsContext> compileRHS(LiteralExpression rhs) {
        _assertActive();
        try {
            JustRhsRuleData sources = new JustRhsRuleData(rhs);
            return compileRules(Collections.singletonList(sources)).iterator().next().rhs();
        } catch (CompilationException e) {
            e.log(LOGGER, Level.WARNING);
            throw new IllegalStateException(e);
        }
    }

    <S extends RuleLiteralData<R1>, R1 extends Rule> Collection<RuleCompiledSources<S, R1>> compileRules(Collection<S> sources) throws CompilationException {
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
    public Collection<LiteralEvaluator> compile(Collection<LiteralExpression> expressions) throws CompilationException {
        Map<LiteralExpression, JustConditionsRuleData> mapping = new IdentityHashMap<>();
        expressions.forEach(expression -> {
            JustConditionsRuleData data = mapping.computeIfAbsent(expression, k -> new JustConditionsRuleData(expression.getContext()));
            data.conditions().add(expression.getSource());
        });

        return compileRules(mapping.values())
                .stream()
                .flatMap(compiled -> compiled.conditions().stream())
                .collect(Collectors.toList());
    }
}
