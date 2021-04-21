package org.evrete.spi.minimal;

import org.evrete.Configuration;
import org.evrete.api.*;
import org.evrete.runtime.FactType;

import java.util.function.BiPredicate;

class DefaultMemoryFactory implements MemoryFactory {
    private final DefaultValueResolver valueResolver = new DefaultValueResolver();
    private static final String CONFIG_BETA_INITIAL_SIZE = "evrete.impl.beta-memory-initial-size";
    private static final String CONFIG_FACT_STORAGE_CAPACITY = "evrete.impl.fact-storage-initial-size";
    private static final int FACT_STORAGE_CAPACITY_DEFAULT = 8192;
    private static final int BETA_INITIAL_SIZE_DEFAULT = 4096;

    private final Configuration configuration;

    DefaultMemoryFactory(RuntimeContext<?> context) {
        this.configuration = context.getConfiguration();
    }

    @Override
    public MemoryKeyCollection newMemoryKeyCollection(FactType[] types) {
        return new DefaultMemoryKeyCollection();
    }

    @Override
    public ValueResolver getValueResolver() {
        return valueResolver;
    }

    @Override
    public <Z> FactStorage<Z> newFactStorage(Type<?> type, Class<Z> storageClass, BiPredicate<Z, Z> identityFunction) {
        int minCapacity = configuration.getAsInteger(CONFIG_FACT_STORAGE_CAPACITY, FACT_STORAGE_CAPACITY_DEFAULT);
        return new DefaultFactStorage<>(type, identityFunction, minCapacity);
    }

    @Override
    public KeyedFactStorage newBetaStorage(ActiveField[] fields) {
        int initialSize = configuration.getAsInteger(CONFIG_BETA_INITIAL_SIZE, BETA_INITIAL_SIZE_DEFAULT);
        if (fields.length == 0) {
            return new SharedAlphaData();
        } else {
            return fields.length == 1 ?
                    new KeyedFactStorageSingle(initialSize)
                    :
                    new KeyedFactStorageMulti(initialSize, fields)
                    ;
        }
    }
}
