package org.evrete;

import org.evrete.api.Knowledge;
import org.evrete.api.OrderedServiceProvider;
import org.evrete.api.StatefulSession;
import org.evrete.api.StatelessSession;
import org.evrete.api.spi.*;
import org.evrete.runtime.KnowledgeRuntime;
import org.evrete.runtime.async.ForkJoinExecutor;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
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
    private final SourceSecurity security = new SourceSecurity();

    public KnowledgeService(Configuration conf) {
        this.configuration = conf;
        this.executor = new ForkJoinExecutor(conf.getAsInteger(Configuration.PARALLELISM, Runtime.getRuntime().availableProcessors()));
        this.collectionsServiceProvider = loadService(conf, MemoryFactoryProvider.class, Configuration.SPI_MEMORY_FACTORY);
        this.expressionResolverProvider = loadService(conf, ExpressionResolverProvider.class, Configuration.SPI_EXPRESSION_RESOLVER);
        this.typeResolverProvider = loadService(conf, TypeResolverProvider.class, Configuration.SPI_TYPE_RESOLVER);
        this.literalRhsProvider = loadService(conf, LiteralRhsCompiler.class, Configuration.SPI_RHS_COMPILER);
        this.classLoader = Thread.currentThread().getContextClassLoader();
    }

    public KnowledgeService() {
        this(new Configuration());
    }

    private static <Z extends OrderedServiceProvider> Z loadService(Configuration conf, Class<Z> clazz, String propertyName) {
        List<Z> providers = new LinkedList<>();
        Iterator<Z> sl = ServiceLoader.load(clazz).iterator();
        sl.forEachRemaining(providers::add);
        Collections.sort(providers);
        if (providers.isEmpty()) {
            throw new IllegalStateException("Implementation missing: " + clazz);
        } else {
            String className = conf.getProperty(propertyName);
            if (className == null) {
                return providers.iterator().next();
            } else {
                for (Z provider : providers) {
                    if (provider.getClass().getName().equals(className)) {
                        return provider;
                    }
                }
                throw new IllegalArgumentException("No such service implementation found: '" + className + "'");
            }
        }
    }

    public SourceSecurity getSecurity() {
        return security;
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

    private static URL classToURL(Class<?> cl) {
        String resource = cl.getName().replaceAll("\\.", "/") + ".class";
        return cl.getClassLoader().getResource(resource);
    }

    /**
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     */
    public Knowledge newKnowledge(String dsl, URL... resources) throws IOException {
        return getDslProvider(dsl).create(this, resources);
    }

    /**
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     */
    public Knowledge newKnowledge(String dsl, Reader... resources) throws IOException {
        return getDslProvider(dsl).create(this, resources);
    }

    /**
     * @return a {@link Knowledge} instance built by DSL provider from given resources.
     */
    public Knowledge newKnowledge(String dsl, InputStream... resources) throws IOException {
        return getDslProvider(dsl).create(this, resources);
    }

    public Knowledge newKnowledge(String dsl, Class<?>... resources) throws IOException {
        if (resources == null || resources.length == 0) throw new IOException("Empty resources");
        URL[] urls = new URL[resources.length];
        for (int i = 0; i < resources.length; i++) {
            urls[i] = classToURL(resources[i]);
        }
        return getDslProvider(dsl).create(this, urls);
    }

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

    public TypeResolverProvider getTypeResolverProvider() {
        return typeResolverProvider;
    }

    public Knowledge newKnowledge(String dsl, String... resources) throws IOException {
        if (resources == null || resources.length == 0) throw new IOException("Empty resources");
        Reader[] urls = new Reader[resources.length];
        for (int i = 0; i < resources.length; i++) {
            urls[i] = new StringReader(resources[i]);
        }
        return getDslProvider(dsl).create(this, urls);
    }

    /**
     * <p>
     * Deprecated method, use {@link #newStatefulSession()} instead.
     * </p>
     *
     * @return an empty {@link StatefulSession}
     */
    @SuppressWarnings("WeakerAccess")
    @Deprecated
    public StatefulSession newSession() {
        return newStatefulSession();
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


}
