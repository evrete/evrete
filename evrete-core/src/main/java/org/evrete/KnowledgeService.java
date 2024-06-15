package org.evrete;

import org.evrete.api.*;
import org.evrete.api.spi.DSLKnowledgeProvider;
import org.evrete.api.spi.LiteralSourceCompiler;
import org.evrete.api.spi.MemoryFactoryProvider;
import org.evrete.api.spi.TypeResolverProvider;
import org.evrete.runtime.AbstractKnowledgeService;
import org.evrete.runtime.KnowledgeRuntime;
import org.evrete.util.DelegatingExecutorService;

import java.io.*;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <p>
 * KnowledgeService is a root element of every Evrete-based application.
 * It holds the initial {@link Configuration}, security settings, references to all
 * required SPI implementations, and an instance of the Java ExecutorService.
 * </p>
 */
//TODO updatable executor
//TODO create an abstract version in the runtime package
public class KnowledgeService extends AbstractKnowledgeService {
    private final Configuration configuration;
    private final MemoryFactoryProvider collectionsServiceProvider;
    private final TypeResolverProvider typeResolverProvider;
    private final LiteralSourceCompiler literalSourceCompiler;
    private ClassLoader classLoader;

    public KnowledgeService(Configuration conf) {
        this(new Builder(conf));
    }

    private KnowledgeService(Builder builder) {
        super(executorFactory(builder));
        this.configuration = builder.conf;
        this.collectionsServiceProvider = builder.getMemoryFactoryProvider();
        this.typeResolverProvider = builder.getTypeResolverProvider();
        this.literalSourceCompiler = builder.getLiteralSourceCompiler();
        this.classLoader = Thread.currentThread().getContextClassLoader();
    }

    public KnowledgeService() {
        this(new Configuration());
    }


    private static ExecutorService executorFactory(Builder builder) {
        if(builder.executor == null) {
            return new DelegatingExecutorService(null);
        } else {
            return new DelegatingExecutorService(builder.executor);
        }
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

    public static Builder builder(Configuration configuration) {
        return new Builder(configuration);
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
     */
    //TODO mark as deprecated
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
     * @throws IllegalStateException if implementation could not be instantiated
     */
    //TODO mark as deprecated
    public DSLKnowledgeProvider getDSL(Class<? extends DSLKnowledgeProvider> dsl) {
        return DSLKnowledgeProvider.load(dsl);
    }

    /**
     * @param dsl       DSL name
     * @param resources DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     * @throws IOException if an error occurs when reading the data sources.
     */
    public Knowledge newKnowledge(String dsl, URL... resources) throws IOException {
        return getDSL(dsl).create(this, resources);
    }

    /**
     * @param dsl       DSL class
     * @param resources DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     * @throws IOException if an error occurs when reading the data sources.
     */
    public Knowledge newKnowledge(Class<? extends DSLKnowledgeProvider> dsl, URL... resources) throws IOException {
        return getDSL(dsl).create(this, resources);
    }

    /**
     * @param dsl       DSL name
     * @param resources DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     * @throws IOException if an error occurs when reading the data sources.
     */
    public Knowledge newKnowledge(String dsl, File... resources) throws IOException {
        return getDSL(dsl).create(this, resources);
    }

    /**
     * @param dsl       DSL class
     * @param resources DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     * @throws IOException if an error occurs when reading the data sources.
     */
    public Knowledge newKnowledge(Class<? extends DSLKnowledgeProvider> dsl, File... resources) throws IOException {
        return getDSL(dsl).create(this, resources);
    }

    /**
     * @param dsl       DSL class
     * @param resolver  TypeResolver to use
     * @param resources DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     * @throws IOException if an error occurs when reading the data sources.
     * @deprecated use the {@link #newKnowledge(Class, File[])} method instead
     */
    @Deprecated
    @SuppressWarnings("unused")
    public Knowledge newKnowledge(Class<? extends DSLKnowledgeProvider> dsl, TypeResolver resolver, File... resources) throws IOException {
        return getDSL(dsl).create(this, resources);
    }

    /**
     * @param dsl       DSL class
     * @param resolver  TypeResolver to use
     * @param resources DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     * @throws IOException if an error occurs when reading the data sources.
     * @deprecated use the {@link #newKnowledge(String, File[])} method instead
     */
    @Deprecated
    @SuppressWarnings("unused")
    public Knowledge newKnowledge(String dsl, TypeResolver resolver, File... resources) throws IOException {
        return getDSL(dsl).create(this, resources);
    }

    /**
     * @param dsl          DSL name
     * @param typeResolver TypeResolver to use
     * @param resources    DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     * @throws IOException if an error occurs when reading the data sources.
     * @deprecated use the {@link #newKnowledge(String, URL[])} method instead
     */
    @Deprecated
    @SuppressWarnings("unused")
    public Knowledge newKnowledge(String dsl, TypeResolver typeResolver, URL... resources) throws IOException {
        return getDSL(dsl).create(this, resources);
    }

    /**
     * @param dsl          DSL class
     * @param typeResolver TypeResolver to use
     * @param resources    DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     * @throws IOException if an error occurs when reading the data sources.
     * @deprecated use the {@link #newKnowledge(Class, URL[])} method instead
     */
    @Deprecated
    @SuppressWarnings("unused")
    public Knowledge newKnowledge(Class<? extends DSLKnowledgeProvider> dsl, TypeResolver typeResolver, URL... resources) throws IOException {
        return getDSL(dsl).create(this, resources);
    }

    /**
     * @param dsl       DSL name
     * @param resources DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     * @throws IOException if an error occurs when reading the data sources.
     */
    public Knowledge newKnowledge(String dsl, Reader... resources) throws IOException {
        return getDSL(dsl).create(this, resources);
    }

    /**
     * @param dsl       DSL class
     * @param resources DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     * @throws IOException if an error occurs when reading the data sources.
     */
    public Knowledge newKnowledge(Class<? extends DSLKnowledgeProvider> dsl, Reader... resources) throws IOException {
        return getDSL(dsl).create(this, resources);
    }

    /**
     * @param dsl          DSL name
     * @param typeResolver TypeResolver to use
     * @param resources    DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     * @throws IOException if an error occurs when reading the data sources.
     */
    public Knowledge newKnowledge(String dsl, TypeResolver typeResolver, Reader... resources) throws IOException {
        return getDSL(dsl).create(this, typeResolver, resources);
    }

    /**
     * @param dsl          DSL class
     * @param typeResolver TypeResolver to use
     * @param resources    DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     * @throws IOException if an error occurs when reading the data sources.
     */
    public Knowledge newKnowledge(Class<? extends DSLKnowledgeProvider> dsl, TypeResolver typeResolver, Reader... resources) throws IOException {
        return getDSL(dsl).create(this, typeResolver, resources);
    }

    /**
     * @param dsl       DSL name
     * @param resources DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     * @throws IOException if an error occurs when reading the data sources.
     */
    public Knowledge newKnowledge(String dsl, InputStream... resources) throws IOException {
        return getDSL(dsl).create(this, resources);
    }

    /**
     * @param dsl       DSL class
     * @param resources DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     * @throws IOException if an error occurs when reading the data sources.
     */
    public Knowledge newKnowledge(Class<? extends DSLKnowledgeProvider> dsl, InputStream... resources) throws IOException {
        return getDSL(dsl).create(this, resources);
    }

    /**
     * @param dsl          DSL name
     * @param typeResolver TypeResolver to use
     * @param resources    DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     * @throws IOException if an error occurs when reading the data sources.
     * @deprecated use the {@link #newKnowledge(String, InputStream[])} method instead
     */
    @Deprecated
    @SuppressWarnings("unused")
    public Knowledge newKnowledge(String dsl, TypeResolver typeResolver, InputStream... resources) throws IOException {
        return getDSL(dsl).create(this, resources);
    }

    /**
     * @param dsl          DSL class
     * @param typeResolver TypeResolver to use
     * @param resources    DSL resources
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     * @throws IOException if an error occurs when reading the data sources.
     * @deprecated use the {@link #newKnowledge(String, InputStream[])} method instead
     */
    @Deprecated
    @SuppressWarnings("unused")
    public Knowledge newKnowledge(Class<? extends DSLKnowledgeProvider> dsl, TypeResolver typeResolver, InputStream... resources) throws IOException {
        return getDSL(dsl).create(this, resources);
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
     */
    public Knowledge newKnowledge(String dsl, Class<?>... resources) throws IOException {
        return getDSL(dsl).create(this, toReaders(resources));
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public MemoryFactoryProvider getMemoryFactoryProvider() {
        return collectionsServiceProvider;
    }

    public LiteralSourceCompiler getLiteralSourceCompiler() {
        return literalSourceCompiler;
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
     * @throws IOException if an error occurs when reading the class sources.
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
     * @throws IOException if an error occurs when reading the class sources.
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
     * @throws IOException if an error occurs when reading the sources.
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

    /**
     * The Builder class is responsible for constructing instances of the KnowledgeService class.
     */
    public static class Builder {
        private final Configuration conf;
        private Class<? extends MemoryFactoryProvider> memoryFactoryProvider;
        private Class<? extends TypeResolverProvider> typeResolverProvider;
        private Class<? extends LiteralSourceCompiler> literalSourceCompiler;
        private ExecutorService executor;

        private Builder(Configuration conf) {
            this.conf = conf;
        }

        public Builder withMemoryFactoryProvider(Class<? extends MemoryFactoryProvider> memoryFactoryProvider) {
            this.memoryFactoryProvider = memoryFactoryProvider;
            return this;
        }

        public Builder withTypeResolverProvider(Class<? extends TypeResolverProvider> typeResolverProvider) {
            this.typeResolverProvider = typeResolverProvider;
            return this;
        }

        public Builder withLiteralSourceCompiler(Class<? extends LiteralSourceCompiler> literalSourceCompiler) {
            this.literalSourceCompiler = literalSourceCompiler;
            return this;
        }

        public Builder withExecutor(ExecutorService executor) {
            this.executor = executor;
            return this;
        }

        public KnowledgeService build() {
            return new KnowledgeService(this);
        }

        private MemoryFactoryProvider getMemoryFactoryProvider() {
            return loadCoreSPI(MemoryFactoryProvider.class, Configuration.SPI_MEMORY_FACTORY, memoryFactoryProvider);
        }

        private TypeResolverProvider getTypeResolverProvider() {
            return loadCoreSPI(TypeResolverProvider.class, Configuration.SPI_TYPE_RESOLVER, typeResolverProvider);
        }

        private LiteralSourceCompiler getLiteralSourceCompiler() {
            return loadCoreSPI(LiteralSourceCompiler.class, Configuration.SPI_SOURCE_COMPILER, literalSourceCompiler);
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
