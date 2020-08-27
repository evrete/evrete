package org.evrete;

import org.evrete.api.spi.*;

import java.util.*;
import java.util.logging.Logger;

public class Configuration {
    private static final Logger LOGGER = Logger.getLogger(Configuration.class.getName());
    private static final boolean DEFAULT_USE_REFLECTION = true;
    private static final int DEFAULT_EXPECTED_OBJECT_COUNT = 256 * 1024;
    private static final int DEFAULT_SESSION_POOL_SIZE = 16;
    private static final int DEFAULT_MEMORY_BUFFER_CAPACITY = 256;
    private static final int DEFAULT_CYCLE_LIMIT = 1024;
    private static final boolean DEFAULT_WARN_UNKNOWN_TYPES = true;
    private final ClassLoader classLoader;// = Thread.currentThread().getContextClassLoader();
    private final CollectionsService collectionsService;
    private final ExpressionResolver expressionResolver;
    private final ResolverService resolverService;
    private boolean useReflection = DEFAULT_USE_REFLECTION;
    private int expectedObjectCount = DEFAULT_EXPECTED_OBJECT_COUNT;
    private int memoryBufferCapacity = DEFAULT_MEMORY_BUFFER_CAPACITY;
    private long cycleLimit = DEFAULT_CYCLE_LIMIT;
    //private final KnowledgeServicesProvider servicesProvider;
    private boolean warnUnknownTypes = DEFAULT_WARN_UNKNOWN_TYPES;
    private boolean entryNodeCachingEnabled = true;

    private Configuration(ClassLoader classLoader, Properties properties) {
        this.classLoader = classLoader;
        this.collectionsService = loadService(CollectionsServiceProvider.class).instance(properties);
        this.expressionResolver = loadService(ExpressionResolverProvider.class).instance(properties, classLoader);
        this.resolverService = loadService(ResolverServiceProvider.class).instance();
    }


    private Configuration(Properties properties) {
        this(Thread.currentThread().getContextClassLoader(), properties);
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

    public boolean isEntryNodeCachingEnabled() {
        return entryNodeCachingEnabled;
    }

    public void setEntryNodeCachingEnabled(boolean entryNodeCachingEnabled) {
        this.entryNodeCachingEnabled = entryNodeCachingEnabled;
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

    public boolean isUseReflection() {
        return useReflection;
    }

    public void setUseReflection(boolean useReflection) {
        this.useReflection = useReflection;
    }

    int getMemoryBufferCapacity() {
        return memoryBufferCapacity;
    }

    public void setMemoryBufferCapacity(int memoryBufferCapacity) {
        this.memoryBufferCapacity = memoryBufferCapacity;
    }

    public int getExpectedObjectCount() {
        return expectedObjectCount;
    }

    public void setExpectedObjectCount(int expectedObjectCount) {
        this.expectedObjectCount = expectedObjectCount;
    }

    public long getCycleLimit() {
        return cycleLimit;
    }

    public Configuration setCycleLimit(long cycleLimit) {
        if (cycleLimit < 2) {
            LOGGER.warning("Cycle limit too low, rules might not work as expected");
        }
        this.cycleLimit = cycleLimit;
        return this;
    }

}
