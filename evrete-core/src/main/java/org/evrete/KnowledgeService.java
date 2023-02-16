package org.evrete;

import org.evrete.api.*;
import org.evrete.api.spi.*;
import org.evrete.runtime.KnowledgeRuntime;
import org.evrete.runtime.async.ForkJoinExecutor;

import java.io.*;
import java.net.URL;
import java.util.*;

/**
 * <p>
 * KnowledgeService is a root element of every Evrete-based application.
 * It holds initial {@link Configuration}, security settings, references to all
 * required SPI implementations, and an instance of Java ExecutorService.
 * </p>
 */
public class KnowledgeService {
    private final Configuration configuration;
    private final ForkJoinExecutor executor;
    private final MemoryFactoryProvider collectionsServiceProvider;
    private final ExpressionResolverProvider expressionResolverProvider;
    private final TypeResolverProvider typeResolverProvider;
    private final LiteralRhsCompiler literalRhsProvider;
    private ClassLoader classLoader;

    public KnowledgeService(Configuration conf) {
        this(new Builder(conf));
    }

    private KnowledgeService(Builder builder) {
        this.configuration = builder.conf;
        this.executor = new ForkJoinExecutor(builder.conf.getAsInteger(Configuration.PARALLELISM, Runtime.getRuntime().availableProcessors()));
        this.collectionsServiceProvider = builder.getMemoryFactoryProvider();
        this.expressionResolverProvider = builder.getExpressionResolverProvider();
        this.typeResolverProvider = builder.getTypeResolverProvider();
        this.literalRhsProvider = builder.getLiteralRhsCompiler();
        this.classLoader = Thread.currentThread().getContextClassLoader();
    }

    public KnowledgeService() {
        this(new Configuration());
    }

    private static Reader[] toReaders(Class<?>... resources) throws IOException {
        if (resources == null || resources.length == 0) throw new IOException("Empty resources");
        Reader[] urls = new Reader[resources.length];
        for (int i = 0; i < resources.length; i++) {
            urls[i] = new StringReader(resources[i].getName());
        }
        return urls;
    }

    private static Reader[] readers(String... resources) throws IOException {
        if (resources == null || resources.length == 0) throw new IOException("Empty resources");
        Reader[] readers = new Reader[resources.length];
        for (int i = 0; i < resources.length; i++) {
            readers[i] = new StringReader(resources[i]);
        }
        return readers;
    }

    public static Builder builder() {
        return new Builder(new Configuration());
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    @SuppressWarnings("unused")
    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * @return an empty {@link Knowledge} instance
     */
    public Knowledge newKnowledge() {
        return new KnowledgeRuntime(this);
    }

    /**
     * @return an empty {@link Knowledge} instance
     */
    public Knowledge newKnowledge(TypeResolver typeResolver) {
        return new KnowledgeRuntime(this, typeResolver);
    }

    public TypeResolver newTypeResolver() {
        return typeResolverProvider.instance(this.classLoader);
    }

    public static Builder builder(Configuration configuration) {
        return new Builder(configuration);
    }

    /**
     * <p>
     * A convenience method to load specific DSL implementation;
     * </p>
     *
     * @param dsl DSL name
     * @return new instance of DSL provider
     * @throws IllegalStateException if no implementation found by the given name
     */
    public DSLKnowledgeProvider getDSL(String dsl) {
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

    /**
     * <p>
     * A convenience method to load specific DSL implementation;
     * </p>
     *
     * @param dsl DSL implementation class
     * @return new instance of DSL provider
     * @throws IllegalStateException if implementation could not be instantiated
     */
    public DSLKnowledgeProvider getDSL(Class<? extends DSLKnowledgeProvider> dsl) {
        Objects.requireNonNull(dsl);
        try {
            return dsl.getConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to instantiate DSL class instance", e);
        }
    }

    /**
     * @param dsl       DSL name
     * @param resources DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     */
    public Knowledge newKnowledge(String dsl, URL... resources) throws IOException {
        return getDSL(dsl).create(this, resources);
    }

    /**
     * @param dsl       DSL class
     * @param resources DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     */
    public Knowledge newKnowledge(Class<? extends DSLKnowledgeProvider> dsl, URL... resources) throws IOException {
        return getDSL(dsl).create(this, resources);
    }

    /**
     * @param dsl       DSL name
     * @param resources DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     */
    public Knowledge newKnowledge(String dsl, File... resources) throws IOException {
        return getDSL(dsl).create(this, resources);
    }

    /**
     * @param dsl       DSL class
     * @param resources DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     */
    public Knowledge newKnowledge(Class<? extends DSLKnowledgeProvider> dsl, File... resources) throws IOException {
        return getDSL(dsl).create(this, resources);
    }

    /**
     * @param dsl       DSL class
     * @param resolver  TypeResolver to use
     * @param resources DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     */
    public Knowledge newKnowledge(Class<? extends DSLKnowledgeProvider> dsl, TypeResolver resolver, File... resources) throws IOException {
        return getDSL(dsl).create(this, resolver, resources);
    }

    /**
     * @param dsl       DSL class
     * @param resolver  TypeResolver to use
     * @param resources DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     */
    public Knowledge newKnowledge(String dsl, TypeResolver resolver, File... resources) throws IOException {
        return getDSL(dsl).create(this, resolver, resources);
    }

    /**
     * @param dsl          DSL name
     * @param typeResolver TypeResolver to use
     * @param resources    DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     */
    public Knowledge newKnowledge(String dsl, TypeResolver typeResolver, URL... resources) throws IOException {
        return getDSL(dsl).create(this, typeResolver, resources);
    }

    /**
     * @param dsl          DSL class
     * @param typeResolver TypeResolver to use
     * @param resources    DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     */
    public Knowledge newKnowledge(Class<? extends DSLKnowledgeProvider> dsl, TypeResolver typeResolver, URL... resources) throws IOException {
        return getDSL(dsl).create(this, typeResolver, resources);
    }

    /**
     * @param dsl       DSL name
     * @param resources DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     */
    public Knowledge newKnowledge(String dsl, Reader... resources) throws IOException {
        return getDSL(dsl).create(this, resources);
    }

    /**
     * @param dsl       DSL class
     * @param resources DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     */
    public Knowledge newKnowledge(Class<? extends DSLKnowledgeProvider> dsl, Reader... resources) throws IOException {
        return getDSL(dsl).create(this, resources);
    }

    /**
     * @param dsl          DSL name
     * @param typeResolver TypeResolver to use
     * @param resources    DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     */
    public Knowledge newKnowledge(String dsl, TypeResolver typeResolver, Reader... resources) throws IOException {
        return getDSL(dsl).create(this, typeResolver, resources);
    }

    /**
     * @param dsl          DSL class
     * @param typeResolver TypeResolver to use
     * @param resources    DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     */
    public Knowledge newKnowledge(Class<? extends DSLKnowledgeProvider> dsl, TypeResolver typeResolver, Reader... resources) throws IOException {
        return getDSL(dsl).create(this, typeResolver, resources);
    }

    /**
     * @param dsl       DSL name
     * @param resources DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     */
    public Knowledge newKnowledge(String dsl, InputStream... resources) throws IOException {
        return getDSL(dsl).create(this, resources);
    }

    /**
     * @param dsl       DSL class
     * @param resources DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     */
    public Knowledge newKnowledge(Class<? extends DSLKnowledgeProvider> dsl, InputStream... resources) throws IOException {
        return getDSL(dsl).create(this, resources);
    }

    /**
     * @param dsl          DSL name
     * @param typeResolver TypeResolver to use
     * @param resources    DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     */
    public Knowledge newKnowledge(String dsl, TypeResolver typeResolver, InputStream... resources) throws IOException {
        return getDSL(dsl).create(this, typeResolver, resources);
    }

    /**
     * @param dsl          DSL class
     * @param typeResolver TypeResolver to use
     * @param resources    DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     */
    public Knowledge newKnowledge(Class<? extends DSLKnowledgeProvider> dsl, TypeResolver typeResolver, InputStream... resources) throws IOException {
        return getDSL(dsl).create(this, typeResolver, resources);
    }

    /**
     * <p>
     * This is a convenience method. The implementation gets URLs of each class and calls {@link #newKnowledge(String, URL...)}
     * </p>
     *
     * @param dsl       DSL name
     * @param resources DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     */
    public Knowledge newKnowledge(String dsl, Class<?>... resources) throws IOException {
        return getDSL(dsl).create(this, toReaders(resources));
    }


    /**
     * <p>
     *     Shuts down the service and releases its internal resources.
     *     Once a service is shutdown, it can not be reused in the future.
     * </p>
     */
    public void shutdown() {
        this.executor.shutdown();
    }

    public ForkJoinExecutor getExecutor() {
        return executor;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public MemoryFactoryProvider getMemoryFactoryProvider() {
        return collectionsServiceProvider;
    }

    public ExpressionResolverProvider getExpressionResolverProvider() {
        return expressionResolverProvider;
    }

    public LiteralRhsCompiler getLiteralRhsCompiler() {
        return literalRhsProvider;
    }

    @SuppressWarnings("unused")
    public TypeResolverProvider getTypeResolverProvider() {
        return typeResolverProvider;
    }

    /**
     * <p>
     * This is a convenience method. The implementation gets URLs of each class and calls {@link #newKnowledge(String, URL...)}
     * </p>
     *
     * @param dsl       DSL class
     * @param resources DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     */
    public Knowledge newKnowledge(Class<? extends DSLKnowledgeProvider> dsl, Class<?>... resources) throws IOException {
        return getDSL(dsl).create(this, toReaders(resources));
    }

    /**
     * <p>
     * This is a convenience method. The implementation gets URLs of each class and calls {@link #newKnowledge(String, TypeResolver, URL...)}
     * </p>
     *
     * @param dsl          DSL name
     * @param typeResolver TypeResolver to use
     * @param resources    DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     */
    public Knowledge newKnowledge(String dsl, TypeResolver typeResolver, Class<?>... resources) throws IOException {
        return getDSL(dsl).create(this, typeResolver, toReaders(resources));
    }

    /**
     * <p>
     * This is a convenience method. The implementation gets URLs of each class and calls {@link #newKnowledge(String, TypeResolver, URL...)}
     * </p>
     *
     * @param dsl          DSL class
     * @param typeResolver TypeResolver to use
     * @param resources    DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     */
    public Knowledge newKnowledge(Class<? extends DSLKnowledgeProvider> dsl, TypeResolver typeResolver, Class<?>... resources) throws IOException {
        return getDSL(dsl).create(this, typeResolver, toReaders(resources));
    }

    /**
     * <p>
     * This method is a shorthand for {@code newKnowledge().newStatefulSession()} which
     * returns an empty session instance.
     * </p>
     *
     * @return an empty {@link StatefulSession}
     */
    @SuppressWarnings("WeakerAccess")
    public StatefulSession newStatefulSession() {
        return newKnowledge().newStatefulSession();
    }

    /**
     * <p>
     * This method is a shorthand for {@code newKnowledge().newStatelessSession()} which
     * returns an empty session instance.
     * </p>
     *
     * @return an empty {@link StatelessSession}
     */
    @SuppressWarnings("WeakerAccess")
    public StatelessSession newStatelessSession() {
        return newKnowledge().newStatelessSession();
    }

    public Knowledge newKnowledge(String dsl, String... resources) throws IOException {
        return getDSL(dsl).create(this, readers(resources));
    }

    public Knowledge newKnowledge(Class<? extends DSLKnowledgeProvider> dsl, String... resources) throws IOException {
        return getDSL(dsl).create(this, readers(resources));
    }

    public static class Builder {
        private final Configuration conf;
        private Class<? extends MemoryFactoryProvider> memoryFactoryProvider;
        private Class<? extends ExpressionResolverProvider> expressionResolverProvider;
        private Class<? extends TypeResolverProvider> typeResolverProvider;
        private Class<? extends LiteralRhsCompiler> literalRhsCompiler;

        private Builder(Configuration conf) {
            this.conf = conf;
        }

        public Builder withMemoryFactoryProvider(Class<? extends MemoryFactoryProvider> memoryFactoryProvider) {
            this.memoryFactoryProvider = memoryFactoryProvider;
            return this;
        }

        public Builder withExpressionResolverProvider(Class<? extends ExpressionResolverProvider> expressionResolverProvider) {
            this.expressionResolverProvider = expressionResolverProvider;
            return this;
        }

        public Builder withTypeResolverProvider(Class<? extends TypeResolverProvider> typeResolverProvider) {
            this.typeResolverProvider = typeResolverProvider;
            return this;
        }

        public Builder withLiteralRhsCompiler(Class<? extends LiteralRhsCompiler> literalRhsCompiler) {
            this.literalRhsCompiler = literalRhsCompiler;
            return this;
        }

        public KnowledgeService build() {
            return new KnowledgeService(this);
        }

        private MemoryFactoryProvider getMemoryFactoryProvider() {
            return loadCoreSPI(MemoryFactoryProvider.class, Configuration.SPI_MEMORY_FACTORY, memoryFactoryProvider);
        }

        private ExpressionResolverProvider getExpressionResolverProvider() {
            return loadCoreSPI(ExpressionResolverProvider.class, Configuration.SPI_EXPRESSION_RESOLVER, expressionResolverProvider);
        }

        private TypeResolverProvider getTypeResolverProvider() {
            return loadCoreSPI(TypeResolverProvider.class, Configuration.SPI_TYPE_RESOLVER, typeResolverProvider);
        }

        private LiteralRhsCompiler getLiteralRhsCompiler() {
            return loadCoreSPI(LiteralRhsCompiler.class, Configuration.SPI_RHS_COMPILER, literalRhsCompiler);
        }

        @SuppressWarnings("unchecked")
        private <Z extends OrderedServiceProvider, I extends Z> Z loadCoreSPI(Class<Z> clazz, String propertyName, Class<I> implClass) {
            // 1. Check the explicit class parameter first
            if (implClass != null) {
                try {
                    return implClass.getConstructor().newInstance();
                } catch (Exception e) {
                    throw new IllegalStateException("Unable to instantiate implementation instance of " + implClass, e);
                }
            }

            // 2. Check the config's entry
            String className = conf.getProperty(propertyName);
            if (className != null) {
                try {
                    return (Z) Class.forName(className).getConstructor().newInstance();
                } catch (Exception e) {
                    throw new IllegalStateException("Unable to instantiate implementation instance of " + className, e);
                }
            }

            // 3. Fall back to the SPI mechanism
            List<Z> providers = new LinkedList<>();
            ServiceLoader.load(clazz).iterator().forEachRemaining(providers::add);
            Collections.sort(providers);
            if (providers.isEmpty()) {
                // No SPI implementations found on the class path
                throw new IllegalStateException("Implementation missing: " + clazz);
            } else {
                return providers.iterator().next();
            }
        }
    }
}
