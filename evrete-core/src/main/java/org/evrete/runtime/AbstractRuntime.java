package org.evrete.runtime;

import org.evrete.Configuration;
import org.evrete.KnowledgeService;
import org.evrete.api.*;
import org.evrete.api.spi.DSLKnowledgeProvider;
import org.evrete.runtime.async.ForkJoinExecutor;
import org.evrete.runtime.builder.FactTypeBuilder;
import org.evrete.runtime.builder.RuleBuilderImpl;
import org.evrete.runtime.evaluation.AlphaBucketMeta;
import org.evrete.runtime.evaluation.EvaluatorWrapper;
import org.evrete.util.DefaultActivationManager;
import org.evrete.util.NextIntSupplier;
import org.evrete.util.compiler.CompilationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;

public abstract class AbstractRuntime<C extends RuntimeContext<C>> extends RuntimeMetaData<C> {
    private final List<RuleBuilder<C>> ruleBuilders = new ArrayList<>();
    private final List<RuleDescriptor> ruleDescriptors;
    private final NextIntSupplier ruleCounter;

    private final KnowledgeService service;
    private ExpressionResolver expressionResolver;
    private TypeResolver typeResolver;
    private final AbstractRuntime<?> parent;
    private ClassLoader classLoader;
    private Comparator<Rule> ruleComparator = SALIENCE_COMPARATOR;
    private Class<? extends ActivationManager> activationManagerFactory;
    private ActivationMode agendaMode;
    private final Configuration configuration;

    /**
     * Constructor for a Knowledge object
     *
     * @param service knowledge service
     */
    AbstractRuntime(KnowledgeService service) {
        super();
        this.configuration = service.getConfiguration().copyOf();
        this.parent = null;
        this.ruleCounter = new NextIntSupplier();
        this.ruleDescriptors = new ArrayList<>();
        this.service = service;
        this.activationManagerFactory = DefaultActivationManager.class;
        this.classLoader = service.getClassLoader();
        this.agendaMode = ActivationMode.DEFAULT;
        this.typeResolver = service.getTypeResolverProvider().instance(this);
    }

    /**
     * Constructor for a Session object
     *
     * @param parent parent context
     */
    AbstractRuntime(AbstractRuntime<?> parent) {
        super(parent);
        this.parent = parent;
        this.configuration = parent.configuration.copyOf();
        this.ruleCounter = parent.ruleCounter.copyOf();
        this.ruleDescriptors = new ArrayList<>(parent.ruleDescriptors);
        this.service = parent.service;
        this.ruleComparator = parent.ruleComparator;
        this.activationManagerFactory = parent.activationManagerFactory;
        this.classLoader = parent.classLoader;
        this.agendaMode = parent.agendaMode;
        this.typeResolver = parent.typeResolver.copyOf();
    }

    ActivationMode getAgendaMode() {
        return agendaMode;
    }

    @Override
    @SuppressWarnings("unchecked")
    public C setActivationMode(ActivationMode agendaMode) {
        this.agendaMode = agendaMode;
        return (C) this;
    }

    private static DSLKnowledgeProvider getDslProvider(String dsl) {
        Objects.requireNonNull(dsl);
        ServiceLoader<DSLKnowledgeProvider> loader = ServiceLoader.load(DSLKnowledgeProvider.class);

        List<DSLKnowledgeProvider> found = new LinkedList<>();
        StringJoiner knownProviders = new StringJoiner(",", "[", "]");
        for (DSLKnowledgeProvider provider : loader) {
            String name = provider.getName();
            if (dsl.equals(name)) {
                found.add(provider);
            }
            knownProviders.add("'" + name + "' = " + provider.getClass());
        }

        if (found.isEmpty()) {
            throw new IllegalStateException("DSL provider '" + dsl + "' is not found. Make sure the corresponding implementation is available on the classpath. Available providers: " + knownProviders);
        }

        if (found.size() > 1) {
            throw new IllegalStateException("Multiple DSL providers found implementing the '" + dsl + "' language. Known providers: " + knownProviders);
        } else {
            return found.iterator().next();
        }
    }

    protected void append(String dsl, InputStream... streams) throws IOException {
        getDslProvider(dsl).apply(this, streams);
    }

    protected void append(String dsl, Reader... readers) throws IOException {
        getDslProvider(dsl).apply(this, readers);
    }

    protected void append(String dsl, URL... resources) throws IOException {
        getDslProvider(dsl).apply(this, resources);
    }

    private static URL classToURL(Class<?> cl) {
        String resource = cl.getName().replaceAll("\\.", "/") + ".class";
        return cl.getClassLoader().getResource(resource);
    }

    protected void append(String dsl, Class<?>... classes) throws IOException {
        if (classes == null || classes.length == 0) return;
        URL[] urls = new URL[classes.length];
        for (int i = 0; i < classes.length; i++) {
            urls[i] = classToURL(classes[i]);
        }
        getDslProvider(dsl).apply(this, urls);
    }

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    @Override
    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public KnowledgeService getService() {
        return service;
    }

    @Override
    public AbstractRuntime<?> getParentContext() {
        return parent;
    }

    @Override
    public final void wrapTypeResolver(TypeResolverWrapper wrapper) {
        this.typeResolver = wrapper;
    }

    @Override
    public final TypeResolver getTypeResolver() {
        return typeResolver;
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

    public Evaluator compile(String expression, Function<String, NamedType> resolver, Set<String> imports) {
        try {
            return getExpressionResolver().buildExpression(expression, resolver, imports);
        } catch (CompilationException e) {
            Logger.getAnonymousLogger().warning("Failed code:\n" + e.getSource());
            throw new RuntimeException(e);
        }
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
        RuleBuilderImpl<C> rb = new RuleBuilderImpl<>(this, name, -1 * ruleCounter.next());
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
        return this.configuration;
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
        try {
            return service.getLiteralRhsCompiler().compileRhs(this, literalRhs, Arrays.asList(factTypes), imports);
        } catch (CompilationException e) {
            Logger.getAnonymousLogger().warning("Failed source\n: " + e.getSource());
            throw new IllegalStateException(e);
        }
    }

    @Override
    public final synchronized ExpressionResolver getExpressionResolver() {
        if (expressionResolver == null) {
            expressionResolver = service.getExpressionResolverProvider().instance(this);
        }
        return expressionResolver;
    }
}
