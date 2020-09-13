package org.evrete.runtime;

import org.evrete.Configuration;
import org.evrete.KnowledgeService;
import org.evrete.api.*;
import org.evrete.api.spi.LiteralRhsProvider;
import org.evrete.runtime.async.Completer;
import org.evrete.runtime.async.ForkJoinExecutor;
import org.evrete.runtime.builder.RuleBuilderImpl;
import org.evrete.runtime.evaluation.AlphaBucketMeta;
import org.evrete.runtime.evaluation.AlphaConditions;
import org.evrete.runtime.evaluation.AlphaDelta;
import org.evrete.util.LazyInstance;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class AbstractRuntime<C extends RuntimeContext<C>> implements RuntimeContext<C> {
    private final List<RuleBuilder<C>> ruleBuilders = new ArrayList<>();
    private final List<RuleDescriptor> ruleDescriptors;
    private final AtomicInteger ruleCounter;
    private final RuntimeListeners listeners;

    private final Set<String> imports;

    private final AlphaConditions alphaConditions;
    private final KnowledgeService service;
    private final ActiveFields activeFields;
    private final Queue<Completer> tasksQueue = new LinkedList<>();
    private ClassLoader classLoader;

    private final LazyInstance<MemoryCollections> collectionsService = new LazyInstance<>(this::newCollectionsService);
    private final LazyInstance<ExpressionResolver> expressionResolver = new LazyInstance<>(this::newExpressionResolver);
    private final LazyInstance<TypeResolver> typeResolver = new LazyInstance<>(this::newTypeResolver);
    private final LazyInstance<LiteralRhsProvider> rhsCompiler = new LazyInstance<>(this::newLiteralLhsProvider);

    private Comparator<Rule> ruleComparator = SALIENCE_COMPARATOR;

    private Class<? extends ActivationManager> activationManagerFactory;

    protected abstract TypeResolver newTypeResolver();

    protected abstract void onNewActiveField(ActiveField newField);

    protected abstract void onNewAlphaBucket(AlphaDelta alphaDelta);

    private final AbstractRuntime<?> parent;

    /**
     * Constructor for a Knowledge object
     *
     * @param service knowledge service
     */
    AbstractRuntime(KnowledgeService service) {
        this.parent = null;
        this.ruleCounter = new AtomicInteger();
        this.alphaConditions = new AlphaConditions();
        this.ruleDescriptors = new ArrayList<>();
        this.listeners = new RuntimeListeners();
        this.service = service;
        this.activeFields = new ActiveFields();
        this.activationManagerFactory = UnconditionalActivationManager.class;
        this.classLoader = service.getClassLoader();
        this.imports = new HashSet<>();
    }

    /**
     * Constructor for a Session object
     *
     * @param parent parent context
     */
    protected AbstractRuntime(AbstractRuntime<?> parent) {
        this.parent = parent;
        this.ruleCounter = new AtomicInteger(parent.ruleCounter.intValue());
        this.alphaConditions = parent.alphaConditions.copyOf();
        this.ruleDescriptors = new ArrayList<>(parent.ruleDescriptors);
        this.service = parent.service;
        this.listeners = parent.listeners.copyOf();
        this.activeFields = parent.activeFields.copyOf();
        this.ruleComparator = parent.ruleComparator;
        this.activationManagerFactory = parent.activationManagerFactory;
        this.classLoader = parent.classLoader;
        this.imports = new HashSet<>(parent.imports);
    }

    @Override
    public RuntimeContext<?> addImport(String imp) {
        this.imports.add(imp);
        return this;
    }

    @Override
    public <A extends ActivationManager> void setActivationManagerFactory(Class<A> managerClass) {
        this.activationManagerFactory = managerClass;
    }

    @Override
    public Set<String> getImports() {
        return imports;
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

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    @Override
    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    protected KnowledgeService getService() {
        return service;
    }

    @Override
    public AbstractRuntime<?> getParentContext() {
        return parent;
    }

    @Override
    public final void wrapTypeResolver(TypeResolverWrapper wrapper) {
        typeResolver.set(wrapper);
    }

    @Override
    public final TypeResolver getTypeResolver() {
        return typeResolver.get();
    }

    @Override
    public Class<? extends ActivationManager> getActivationManagerFactory() {
        return activationManagerFactory;
    }

    protected ActivationManager newActivationManager() {
        try {
            return activationManagerFactory.getDeclaredConstructor().newInstance();
        } catch (Throwable e) {
            throw new RuntimeException("Unable to create activation manager. Probably the provided factory class has no public and zero-argument constructor.", e);
        }
    }

    protected void queueTask(Completer task) {
        this.tasksQueue.add(task);
    }

    protected void processAllTasks() {
        Completer task;
        ForkJoinExecutor executor = getExecutor();
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
        return service.getExecutor();
    }

    public LazyInstance<LiteralRhsProvider> getRhsCompiler() {
        return rhsCompiler;
    }

    public Evaluator compile(String expression, Function<String, NamedType> resolver) {
        return getExpressionResolver().buildExpression(expression, resolver);
    }

    public ActiveField getCreateActiveField(TypeField field) {
        return activeFields.getCreate(field, this::onNewActiveField);
    }

    public ActiveField[] getActiveFields(Type<?> type) {
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
        return service.getConfiguration();
    }

    private MemoryCollections newCollectionsService() {
        return service.getCollectionsServiceProvider().instance(this);
    }

    private ExpressionResolver newExpressionResolver() {
        return service.getExpressionResolverProvider().instance(this);
    }

    private LiteralRhsProvider newLiteralLhsProvider() {
        return service.getLiteralRhsProvider();
    }

    public <Z> KeysStore newKeysStore(Z[][] grouping) {
        return collectionsService.get().newKeyStore(grouping);
    }

    public KeysStore newKeysStore(int[] factTypeCounts) {
        return collectionsService.get().newKeyStore(factTypeCounts);
    }

    public SharedPlainFactStorage newSharedPlainStorage() {
        return collectionsService.get().newPlainStorage();
    }

    public SharedBetaFactStorage newSharedKeyStorage(FieldsKey fieldsKey) {
        return collectionsService.get().newBetaStorage(fieldsKey);
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

    Consumer<RhsContext> compile(String literalRhs, Collection<FactType> factTypes, Collection<String> imports) {
        return getRhsCompiler().get().buildRhs(this, literalRhs, factTypes, imports);
    }

    public ExpressionResolver getExpressionResolver() {
        return expressionResolver.get();
    }

    @Override
    public void addConditionTestListener(EvaluationListener listener) {
        listeners.addConditionTestListener(listener);
    }

    static class UnconditionalActivationManager implements ActivationManager {
    }

}
