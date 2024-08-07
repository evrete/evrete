package org.evrete;

import org.evrete.api.*;
import org.evrete.api.spi.DSLKnowledgeProvider;
import org.evrete.api.spi.MemoryFactoryProvider;
import org.evrete.api.spi.SourceCompilerProvider;
import org.evrete.api.spi.TypeResolverProvider;
import org.evrete.runtime.AbstractKnowledgeService;
import org.evrete.runtime.KnowledgeRuntime;
import org.evrete.util.DelegatingExecutorService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;

/**
 * <p>
 * KnowledgeService is a root element of every Evrete-based application.
 * It holds the initial {@link Configuration}, references to all
 * required SPI implementations, and an instance of the Java ExecutorService.
 * </p>
 */
public class KnowledgeService extends AbstractKnowledgeService {
    private final Configuration configuration;
    private final MemoryFactoryProvider collectionsServiceProvider;
    private final TypeResolverProvider typeResolverProvider;
    private final SourceCompilerProvider sourceCompilerProvider;
    private ClassLoader classLoader;

    public KnowledgeService(Configuration conf) {
        this(new Builder(conf));
    }

    private KnowledgeService(Builder builder) {
        super(executorFactory(builder));
        this.configuration = builder.conf;
        this.collectionsServiceProvider = builder.getMemoryFactoryProvider();
        this.typeResolverProvider = builder.getTypeResolverProvider();
        this.classLoader = Thread.currentThread().getContextClassLoader();
        this.sourceCompilerProvider = builder.getSourceCompilerProvider();
    }

    public KnowledgeService() {
        this(new Configuration());
    }

    static ExecutorService executorFactory(Builder builder) {
        if(builder.executor == null) {
            int parallelism = builder.conf.getAsInteger(Configuration.PARALLELISM,Runtime.getRuntime().availableProcessors());
            boolean daemonThreads = builder.conf.getAsBoolean(Configuration.DAEMON_INNER_THREADS, Configuration.DAEMON_INNER_THREADS_DEFAULT);
            return new DelegatingExecutorService(parallelism, daemonThreads);
        } else {
            return new DelegatingExecutorService(builder.executor);
        }
    }

    public static Builder builder() {
        return new Builder(new Configuration());
    }

    public static Builder builder(Configuration configuration) {
        return new Builder(configuration);
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Shuts down the service.
     */
    public void shutdown() {
        super.shutdownInner();
    }

    @SuppressWarnings("unused")
    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * @return an empty {@link Knowledge} instance
     */
    public Knowledge newKnowledge() {
        return newKnowledge((String) null);
    }

    /**
     * @return an empty {@link Knowledge} instance
     */
    public Knowledge newKnowledge(String name) {
        return new KnowledgeRuntime(this, name);
    }

    /**
     * @param typeResolver the type resolver to use in the newly created Knowledge instance
     * @return an empty {@link Knowledge} instance
     * @deprecated use the default {@link #newKnowledge()} method
     */
    @Deprecated
    @SuppressWarnings("unused")
    public Knowledge newKnowledge(TypeResolver typeResolver) {
        return newKnowledge();
    }

    /**
     * @param typeResolver the type resolver to use in the newly created Knowledge instance
     * @return an empty {@link Knowledge} instance
     * @deprecated use the default {@link #newKnowledge(String)} method
     */
    @Deprecated
    @SuppressWarnings("unused")
    public Knowledge newKnowledge(TypeResolver typeResolver, String name) {
        return new KnowledgeRuntime(this, name);
    }

    /**
     * <p>
     * A convenience method to load specific DSL implementation;
     * </p>
     *
     * @param dsl DSL name
     * @return new instance of DSL provider
     * @throws IllegalStateException if no implementation found by the given name
     * @deprecated this method is deprecated and scheduled for removal.
     * Use the new {@link org.evrete.api.RuleSetContext#importRules(String, Object)} approach or the
     * {@link DSLKnowledgeProvider#load(String)} utility method instead.
     */
    @Deprecated
    public DSLKnowledgeProvider getDSL(String dsl) {
        return DSLKnowledgeProvider.load(dsl);
    }

    /**
     * <p>
     * A convenience method to load specific DSL implementation;
     * </p>
     *
     * @param dsl DSL implementation class
     * @return new instance of DSL provider
     * @deprecated this method is deprecated and scheduled for removal.
     * Use the new {@link org.evrete.api.RuleSetContext#importRules(String, Object)} approach or the
     * {@link DSLKnowledgeProvider#load(String)} utility method instead.
     */
    @Deprecated
    public DSLKnowledgeProvider getDSL(Class<? extends DSLKnowledgeProvider> dsl) {
        return DSLKnowledgeProvider.load(dsl);
    }

    /**
     * @param dsl       DSL name
     * @param resources DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     * @throws IOException if an error occurs when reading the data sources.
     * @deprecated this method is deprecated and scheduled for removal.
     * Use the new {@link org.evrete.api.RuleSetContext#importRules(String, Object)} approach or the
     */
    @Deprecated
    public Knowledge newKnowledge(String dsl, URL... resources) throws IOException {
        return deprecatedStub(dsl, resources);
    }

    /**
     * @param dsl       DSL class
     * @param resources DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     * @throws IOException if an error occurs when reading the data sources.
     * @deprecated this method is deprecated and scheduled for removal.
     * Use the new {@link org.evrete.api.RuleSetContext#importRules(String, Object)} approach or the
     */
    @Deprecated
    public Knowledge newKnowledge(Class<? extends DSLKnowledgeProvider> dsl, URL... resources) throws IOException {
        return deprecatedStub(dsl, resources);
    }

    /**
     * @param dsl       DSL name
     * @param resources DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     * @throws IOException if an error occurs when reading the data sources.
     * @deprecated this method is deprecated and scheduled for removal.
     * Use the new {@link org.evrete.api.RuleSetContext#importRules(String, Object)} approach or the
     */
    @Deprecated
    public Knowledge newKnowledge(String dsl, File... resources) throws IOException {
        return deprecatedStub(dsl, resources);
    }

    /**
     * @param dsl       DSL class
     * @param resources DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     * @throws IOException if an error occurs when reading the data sources.
     * @deprecated this method is deprecated and scheduled for removal.
     * Use the new {@link org.evrete.api.RuleSetContext#importRules(String, Object)} approach or the
     */
    @Deprecated
    public Knowledge newKnowledge(Class<? extends DSLKnowledgeProvider> dsl, File... resources) throws IOException {
        return deprecatedStub(dsl, resources);
    }

    /**
     * @param dsl       DSL class
     * @param resolver  TypeResolver to use
     * @param resources DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     * @throws IOException if an error occurs when reading the data sources.
     * @deprecated this method is deprecated and scheduled for removal.
     * Use the new {@link org.evrete.api.RuleSetContext#importRules(String, Object)} approach or the
     */
    @Deprecated
    @SuppressWarnings("unused")
    public Knowledge newKnowledge(Class<? extends DSLKnowledgeProvider> dsl, TypeResolver resolver, File... resources) throws IOException {
        return deprecatedStub(dsl, resources);
    }

    /**
     * @param dsl       DSL class
     * @param resolver  TypeResolver to use
     * @param resources DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     * @throws IOException if an error occurs when reading the data sources.
     * @deprecated this method is deprecated and scheduled for removal.
     * Use the new {@link org.evrete.api.RuleSetContext#importRules(String, Object)} approach or the
     */
    @Deprecated
    @SuppressWarnings("unused")
    public Knowledge newKnowledge(String dsl, TypeResolver resolver, File... resources) throws IOException {
        return deprecatedStub(dsl, resources);
    }

    /**
     * @param dsl          DSL name
     * @param typeResolver TypeResolver to use
     * @param resources    DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     * @throws IOException if an error occurs when reading the data sources.
     * @deprecated this method is deprecated and scheduled for removal.
     * Use the new {@link org.evrete.api.RuleSetContext#importRules(String, Object)} approach or the
     */
    @Deprecated
    @SuppressWarnings("unused")
    public Knowledge newKnowledge(String dsl, TypeResolver typeResolver, URL... resources) throws IOException {
        return deprecatedStub(dsl, resources);
    }

    /**
     * @param dsl          DSL class
     * @param typeResolver TypeResolver to use
     * @param resources    DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     * @throws IOException if an error occurs when reading the data sources.
     * @deprecated this method is deprecated and scheduled for removal.
     * Use the new {@link org.evrete.api.RuleSetContext#importRules(String, Object)} approach or the
     */
    @Deprecated
    @SuppressWarnings("unused")
    public Knowledge newKnowledge(Class<? extends DSLKnowledgeProvider> dsl, TypeResolver typeResolver, URL... resources) throws IOException {
        return deprecatedStub(dsl, resources);
    }

    /**
     * @param dsl       DSL name
     * @param resources DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     * @throws IOException if an error occurs when reading the data sources.
     * @deprecated this method is deprecated and scheduled for removal.
     * Use the new {@link org.evrete.api.RuleSetContext#importRules(String, Object)} approach or the
     */
    @Deprecated
    public Knowledge newKnowledge(String dsl, Reader... resources) throws IOException {
        return deprecatedStub(dsl, resources);
    }

    /**
     * @param dsl       DSL class
     * @param resources DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     * @throws IOException if an error occurs when reading the data sources.
     * @deprecated this method is deprecated and scheduled for removal.
     * Use the new {@link org.evrete.api.RuleSetContext#importRules(String, Object)} approach or the
     */
    @Deprecated
    public Knowledge newKnowledge(Class<? extends DSLKnowledgeProvider> dsl, Reader... resources) throws IOException {
        return deprecatedStub(dsl, resources);
    }

    /**
     * @param dsl          DSL name
     * @param typeResolver TypeResolver to use
     * @param resources    DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     * @throws IOException if an error occurs when reading the data sources.
     * @deprecated this method is deprecated and scheduled for removal.
     * Use the new {@link org.evrete.api.RuleSetContext#importRules(String, Object)} approach or the
     */
    @Deprecated
    @SuppressWarnings("unused")
    public Knowledge newKnowledge(String dsl, TypeResolver typeResolver, Reader... resources) throws IOException {
        return deprecatedStub(dsl, resources);
    }

    /**
     * @param dsl          DSL class
     * @param typeResolver TypeResolver to use
     * @param resources    DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     * @throws IOException if an error occurs when reading the data sources.
     * @deprecated this method is deprecated and scheduled for removal.
     * Use the new {@link org.evrete.api.RuleSetContext#importRules(String, Object)} approach or the
     */
    @Deprecated
    @SuppressWarnings("unused")
    public Knowledge newKnowledge(Class<? extends DSLKnowledgeProvider> dsl, TypeResolver typeResolver, Reader... resources) throws IOException {
        return deprecatedStub(dsl, resources);
    }

    /**
     * @param dsl       DSL name
     * @param resources DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     * @throws IOException if an error occurs when reading the data sources.
     * @deprecated this method is deprecated and scheduled for removal.
     * Use the new {@link org.evrete.api.RuleSetContext#importRules(String, Object)} approach or the
     */
    @Deprecated
    public Knowledge newKnowledge(String dsl, InputStream... resources) throws IOException {
        return deprecatedStub(dsl, resources);
    }

    /**
     * @param dsl       DSL class
     * @param resources DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     * @throws IOException if an error occurs when reading the data sources.
     * @deprecated this method is deprecated and scheduled for removal.
     * Use the new {@link org.evrete.api.RuleSetContext#importRules(String, Object)} approach or the
     */
    @Deprecated
    public Knowledge newKnowledge(Class<? extends DSLKnowledgeProvider> dsl, InputStream... resources) throws IOException {
        return deprecatedStub(dsl, resources);
    }

    /**
     * @param dsl          DSL name
     * @param typeResolver TypeResolver to use
     * @param resources    DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     * @throws IOException if an error occurs when reading the data sources.
     * @deprecated this method is deprecated and scheduled for removal.
     * Use the new {@link org.evrete.api.RuleSetContext#importRules(String, Object)} approach or the
     */
    @Deprecated
    @SuppressWarnings("unused")
    public Knowledge newKnowledge(String dsl, TypeResolver typeResolver, InputStream... resources) throws IOException {
        return deprecatedStub(dsl, resources);
    }

    /**
     * @param dsl          DSL class
     * @param typeResolver TypeResolver to use
     * @param resources    DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     * @throws IOException if an error occurs when reading the data sources.
     * @deprecated this method is deprecated and scheduled for removal.
     * Use the new {@link org.evrete.api.RuleSetContext#importRules(String, Object)} approach or the
     */
    @Deprecated
    @SuppressWarnings("unused")
    public Knowledge newKnowledge(Class<? extends DSLKnowledgeProvider> dsl, TypeResolver typeResolver, InputStream... resources) throws IOException {
        return deprecatedStub(dsl, resources);
    }

    /**
     * <p>
     * This is a convenience method. The implementation gets URLs of each class and calls {@link #newKnowledge(String, URL...)}
     * </p>
     *
     * @param dsl       DSL name
     * @param resources DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     * @throws IOException if an error occurs when reading the class sources.
     * @deprecated this method is deprecated and scheduled for removal.
     * Use the new {@link org.evrete.api.RuleSetContext#importRules(String, Object)} approach or the
     */
    @Deprecated
    public Knowledge newKnowledge(String dsl, Class<?>... resources) throws IOException {
        return deprecatedStub(dsl, resources);
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public MemoryFactoryProvider getMemoryFactoryProvider() {
        return collectionsServiceProvider;
    }

    public SourceCompilerProvider getSourceCompilerProvider() {
        return sourceCompilerProvider;
    }

    @SuppressWarnings("unused")
    public TypeResolverProvider getTypeResolverProvider() {
        return typeResolverProvider;
    }

    private Knowledge deprecatedStub(String provider, Object[] resources) throws IOException {
        Knowledge knowledge = newKnowledge();
        knowledge.importRules(provider, resources);
        return knowledge;
    }

    private Knowledge deprecatedStub(Class<? extends DSLKnowledgeProvider> type, Object[] resources) throws IOException {
        Knowledge knowledge = newKnowledge();
        DSLKnowledgeProvider provider = DSLKnowledgeProvider.load(type);
        knowledge.importRules(provider, resources);
        return knowledge;
    }

    /**
     * <p>
     * This is a convenience method. The implementation gets URLs of each class and calls {@link #newKnowledge(String, URL...)}
     * </p>
     *
     * @param dsl       DSL class
     * @param resources DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     * @throws IOException if an error occurs when reading the class sources.
     * @deprecated this method is deprecated and scheduled for removal.
     * Use the new {@link org.evrete.api.RuleSetContext#importRules(String, Object)} approach or the
     */
    @Deprecated
    public Knowledge newKnowledge(Class<? extends DSLKnowledgeProvider> dsl, Class<?>... resources) throws IOException {
        return deprecatedStub(dsl, resources);
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
     * @throws IOException if an error occurs when reading the class sources.
     * @deprecated this method is deprecated and scheduled for removal.
     * Use the new {@link org.evrete.api.RuleSetContext#importRules(String, Object)} approach or the
     */
    @Deprecated
    @SuppressWarnings("unused")
    public Knowledge newKnowledge(String dsl, TypeResolver typeResolver, Class<?>... resources) throws IOException {
        return deprecatedStub(dsl, resources);
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
     * @throws IOException if an error occurs when reading the sources.
     * @deprecated this method is deprecated and scheduled for removal.
     * Use the new {@link org.evrete.api.RuleSetContext#importRules(String, Object)} approach or the
     */
    @Deprecated
    @SuppressWarnings("unused")
    public Knowledge newKnowledge(Class<? extends DSLKnowledgeProvider> dsl, TypeResolver typeResolver, Class<?>... resources) throws IOException {
        return deprecatedStub(dsl, resources);
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

    /**
     * Creates a new {@link Knowledge} instance from provided resources.
     * @deprecated this method is deprecated and scheduled for removal.
     * Use the new {@link org.evrete.api.RuleSetContext#importRules(String, Object)} approach or the
     */
    @Deprecated
    public Knowledge newKnowledge(String dsl, String... resources) throws IOException {
        return deprecatedStub(dsl, resources);
    }

    /**
     * Creates a new {@link Knowledge} instance from provided resources.
     * @deprecated this method is deprecated and scheduled for removal.
     * Use the new {@link org.evrete.api.RuleSetContext#importRules(DSLKnowledgeProvider, Object)} approach or the
     */
    @Deprecated
    public Knowledge newKnowledge(Class<? extends DSLKnowledgeProvider> dsl, String... resources) throws IOException {
        return deprecatedStub(dsl, resources);
    }

    /**
     * The Builder class is responsible for constructing instances of the KnowledgeService class.
     */
    public static class Builder {
        private final Configuration conf;
        private Class<? extends MemoryFactoryProvider> memoryFactoryProvider;
        private Class<? extends TypeResolverProvider> typeResolverProvider;
        private Class<? extends SourceCompilerProvider> sourceCompilerProvider;
        private ExecutorService executor;

        /**
         * Constructs a new Builder with the provided Configuration.
         *
         * @param conf The Configuration to be used by the Builder.
         */
        private Builder(Configuration conf) {
            this.conf = conf;
        }

        /**
         * Sets the MemoryFactoryProvider class for this builder.
         *
         * @param memoryFactoryProvider The MemoryFactoryProvider class to set.
         * @return The current Builder instance for method chaining.
         */
        public Builder withMemoryFactoryProvider(Class<? extends MemoryFactoryProvider> memoryFactoryProvider) {
            this.memoryFactoryProvider = memoryFactoryProvider;
            return this;
        }

        /**
         * Sets the TypeResolverProvider class for this builder.
         *
         * @param typeResolverProvider The TypeResolverProvider class to set.
         * @return The current Builder instance for method chaining.
         */
        public Builder withTypeResolverProvider(Class<? extends TypeResolverProvider> typeResolverProvider) {
            this.typeResolverProvider = typeResolverProvider;
            return this;
        }

        /**
         * Sets the SourceCompilerProvider class for this builder.
         *
         * @param sourceCompilerProvider The SourceCompilerProvider class to set.
         * @return The current Builder instance for method chaining.
         */
        public Builder withSourceCompilerProvider(Class<? extends SourceCompilerProvider> sourceCompilerProvider) {
            this.sourceCompilerProvider = sourceCompilerProvider;
            return this;
        }

        /**
         * Sets the external {@link ExecutorService} for this builder. Please note that externally supplied executors
         * won't be automatically shut down when the {@link ExecutorService#shutdown()} method is invoked
         *
         * @param executor The ExecutorService to set.
         * @return The current Builder instance for method chaining.
         */
        public Builder withExecutor(ExecutorService executor) {
            this.executor = executor;
            return this;
        }

        /**
         * Constructs a new instance of the {@link KnowledgeService} with the current configuration of the builder.
         *
         * @return A new instance of the KnowledgeService.
         */
        public KnowledgeService build() {
            return new KnowledgeService(this);
        }

        private MemoryFactoryProvider getMemoryFactoryProvider() {
            return loadCoreSPI(MemoryFactoryProvider.class, Configuration.SPI_MEMORY_FACTORY, memoryFactoryProvider);
        }

        private TypeResolverProvider getTypeResolverProvider() {
            return loadCoreSPI(TypeResolverProvider.class, Configuration.SPI_TYPE_RESOLVER, typeResolverProvider);
        }

        private SourceCompilerProvider getSourceCompilerProvider() {
            return loadCoreSPI(SourceCompilerProvider.class, Configuration.SPI_SOURCE_COMPILER, sourceCompilerProvider);
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
