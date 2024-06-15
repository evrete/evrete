package org.evrete.runtime;

import org.evrete.Configuration;
import org.evrete.KnowledgeService;
import org.evrete.api.*;
import org.evrete.api.builders.RuleSetBuilder;
import org.evrete.util.CompilationException;
import org.evrete.util.DefaultActivationManager;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public abstract class AbstractRuntime<R extends Rule, C extends RuntimeContext<C>> extends AbstractRuntimeMeta<C> implements RuleSetContext<C, R> {
    private static final Logger LOGGER = Logger.getLogger(AbstractRuntime.class.getName());
    private final Configuration configuration;
    private final AtomicInteger noNameRuleCounter;
    private Comparator<Rule> ruleComparator = SALIENCE_COMPARATOR;
    private Class<? extends ActivationManager> activationManagerFactory;
    private ActivationMode agendaMode;
    private final String name;

    AbstractRuntime(KnowledgeService service, String name) {
        super(service);
        this.configuration = service.getConfiguration().copyOf();
        this.activationManagerFactory = DefaultActivationManager.class;
        this.agendaMode = ActivationMode.DEFAULT;
        this.noNameRuleCounter = new AtomicInteger();
        this.name = name;
    }

    /**
     * Constructor for a Session object
     *
     * @param parent parent context
     */
    AbstractRuntime(AbstractRuntime<?, ?> parent) {
        super(parent);
        this.configuration = parent.configuration.copyOf();
        this.ruleComparator = parent.ruleComparator;
        this.activationManagerFactory = parent.activationManagerFactory;
        this.agendaMode = parent.agendaMode;
        this.noNameRuleCounter = parent.noNameRuleCounter;
        this.name = parent.name;
    }

    ActivationMode getAgendaMode() {
        return agendaMode;
    }

    abstract void addRuleDescriptors(List<KnowledgeRule> ruleDescriptors);

    void addRules(DefaultRuleSetBuilder<C> ruleSetBuilder) {
        List<KnowledgeRule> descriptors = compileRuleBuilders(ruleSetBuilder);
        addRuleDescriptors(descriptors);
    }

    synchronized List<KnowledgeRule> compileRuleBuilders(DefaultRuleSetBuilder<C> ruleSetBuilder) {
        try {
            return KnowledgeRule.buildRuleDescriptors(this, ruleSetBuilder, this.compileRuleset(ruleSetBuilder));
        } catch (CompilationException e) {
            e.log(LOGGER, Level.WARNING);
            throw new RuntimeException(e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public C configureTypes(Consumer<TypeResolver> action) {
        action.accept(getTypeResolver());
        return (C) this;
    }

    @Override
    public String getName() {
        return this.name;
    }

    String unnamedRuleName() {
        return "rule_" + noNameRuleCounter.incrementAndGet();
    }


    @Override
    @SuppressWarnings("unchecked")
    public C setActivationMode(ActivationMode activationMode) {
        _assertActive();
        this.agendaMode = activationMode;
        return (C) this;
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

    Collection<RuleCompiledSources<DefaultRuleLiteralData, DefaultRuleBuilder<?>, DefaultConditionManager.Literal>> compileRuleset(DefaultRuleSetBuilder<C> ruleSetBuilder) throws CompilationException {
        List<DefaultRuleLiteralData> ruleLiteralSources = ruleSetBuilder.getRuleBuilders().stream()
                .map(DefaultRuleLiteralData::new)
                .filter(DefaultRuleLiteralData::nonEmpty)
                .collect(Collectors.toList());
        return this.compileRules(ruleLiteralSources);
    }

    Consumer<RhsContext> compileRHS(String rhs, Rule rule) {
        _assertActive();
        try {
            JustRhsRuleData sources = new JustRhsRuleData(rhs, rule);
            return compileRules(Collections.singletonList(sources)).iterator().next().rhs();
        } catch (CompilationException e) {
            e.log(LOGGER, Level.WARNING);
            throw new IllegalStateException(e);
        }
    }

    <S extends RuleLiteralData<R1, C1>, R1 extends Rule, C1 extends LiteralPredicate> Collection<RuleCompiledSources<S, R1, C1>> compileRules(Collection<S> sources) throws CompilationException {
        _assertActive();
        return getService().getLiteralSourceCompiler().compile(this, sources);
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

    @Override
    public RuleSetBuilder<C> builder() {
        _assertActive();
        return new DefaultRuleSetBuilder<>(this);
    }

    @Override
    public Configuration getConfiguration() {
        return this.configuration;
    }

//    FieldReference[] resolveFieldReferences(String[] arg, NamedType.Resolver typeMapper) {
//        _assertActive();
//        return getExpressionResolver().resolve(typeMapper, arg);
//    }

    abstract void _assertActive();

//    Collection<CompiledPredicate> compileConditions(Rule rule, Collection<LiteralPredicate> expressions) throws CompilationException {
//        if(expressions.isEmpty()) {
//            return Collections.emptyList();
//        } else {
//            return compileRules(Collections.singletonList(new JustConditionsRuleData(rule, expressions)))
//                    .iterator()
//                    .next()
//                    .conditions();
//        }
//    }
}
