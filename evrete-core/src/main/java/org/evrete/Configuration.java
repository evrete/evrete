package org.evrete;

import org.evrete.api.spi.*;

import java.util.*;

public class Configuration {
    private static final boolean DEFAULT_WARN_UNKNOWN_TYPES = true;
    private final ClassLoader classLoader;// = Thread.currentThread().getContextClassLoader();
    private final CollectionsService collectionsService;
    private final ExpressionResolver expressionResolver;
    private final ResolverService resolverService;
    private boolean warnUnknownTypes = DEFAULT_WARN_UNKNOWN_TYPES;

    private Configuration(ClassLoader classLoader, Properties properties) {
        this.classLoader = classLoader;
        this.collectionsService = loadService(CollectionsServiceProvider.class).instance(properties);
        this.expressionResolver = loadService(ExpressionResolverProvider.class).instance(properties, classLoader);
        this.resolverService = loadService(ResolverServiceProvider.class).instance();
    }

    public Configuration() {
        this(Thread.currentThread().getContextClassLoader(), new Properties());
    }

    private static <Z extends Comparable<Z>> Z loadService(Class<Z> clazz) {
        List<Z> providers = new LinkedList<>();
        Iterator<Z> sl = ServiceLoader.load(clazz).iterator();
        sl.forEachRemaining(providers::add);
        Collections.sort(providers);
        if (providers.isEmpty()) {
            throw new IllegalStateException();
        } else {
            return providers.iterator().next();
        }
    }

    public boolean isWarnUnknownTypes() {
        return warnUnknownTypes;
    }

    public Configuration setWarnUnknownTypes(boolean warnUnknownTypes) {
        this.warnUnknownTypes = warnUnknownTypes;
        return this;
    }

    public ResolverService getResolverService() {
        return resolverService;
    }

    public CollectionsService getCollectionsService() {
        return collectionsService;
    }

    public ExpressionResolver getExpressionsService() {
        return expressionResolver;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }
}
