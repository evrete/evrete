package org.evrete;

import org.evrete.api.Knowledge;
import org.evrete.api.OrderedServiceProvider;
import org.evrete.api.spi.ExpressionResolverProvider;
import org.evrete.api.spi.LiteralRhsCompiler;
import org.evrete.api.spi.MemoryCollectionsProvider;
import org.evrete.api.spi.TypeResolverProvider;
import org.evrete.runtime.KnowledgeImpl;
import org.evrete.runtime.async.ForkJoinExecutor;

import java.util.*;

public class KnowledgeService {
    private final Configuration configuration;
    private final ForkJoinExecutor executor = new ForkJoinExecutor();
    private final MemoryCollectionsProvider collectionsServiceProvider;
    private final ExpressionResolverProvider expressionResolverProvider;
    private final TypeResolverProvider typeResolverProvider;
    private final LiteralRhsCompiler literalRhsProvider;
    private ClassLoader classLoader;


    public KnowledgeService(Configuration configuration) {
        this.configuration = configuration;
        this.collectionsServiceProvider = loadService(MemoryCollectionsProvider.class);
        this.expressionResolverProvider = loadService(ExpressionResolverProvider.class);
        this.typeResolverProvider = loadService(TypeResolverProvider.class);
        this.literalRhsProvider = loadService(LiteralRhsCompiler.class);
        this.classLoader = Thread.currentThread().getContextClassLoader();
    }

    public KnowledgeService() {
        this(new Configuration());
    }

    private static <Z extends OrderedServiceProvider> Z loadService(Class<Z> clazz) {
        List<Z> providers = new LinkedList<>();
        Iterator<Z> sl = ServiceLoader.load(clazz).iterator();
        sl.forEachRemaining(providers::add);
        Collections.sort(providers);
        if (providers.isEmpty()) {
            throw new IllegalStateException("Implementation missing: " + clazz);
        } else {
            return providers.iterator().next();
        }
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    @SuppressWarnings("unused")
    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public Knowledge newKnowledge() {
        return new KnowledgeImpl(this);
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

    public MemoryCollectionsProvider getCollectionsServiceProvider() {
        return collectionsServiceProvider;
    }

    public ExpressionResolverProvider getExpressionResolverProvider() {
        return expressionResolverProvider;
    }

    public LiteralRhsCompiler getLiteralRhsProvider() {
        return literalRhsProvider;
    }

    public TypeResolverProvider getTypeResolverProvider() {
        return typeResolverProvider;
    }

}
