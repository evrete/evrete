package org.evrete.spi.minimal;

import org.evrete.Configuration;
import org.evrete.api.*;
import org.evrete.runtime.FactType;

import java.util.function.BiPredicate;

class DefaultMemoryFactory implements MemoryFactory {
    private static final String CONFIG_BETA_INITIAL_SIZE = "evrete.impl.beta-memory-initial-size";
    //TODO drop the property and update docs
    private static final String CONFIG_FACT_STORAGE_CAPACITY = "evrete.impl.fact-storage-initial-size";
    private static final int FACT_STORAGE_CAPACITY_DEFAULT = 8192;
    private static final int BETA_INITIAL_SIZE_DEFAULT = 4096;
    private final DefaultValueResolver valueResolver = new DefaultValueResolver();
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
        return new DefaultFactStorage<>(type, identityFunction);
    }

    @Override
    public KeyedFactStorage newBetaStorage(int fieldCount) {
        if (fieldCount == 0) {
            return new SharedAlphaData();
        } else {
            return fieldCount == 1 ?
                    new KeyedFactStorageSingle()
                    :
                    new KeyedFactStorageMulti(fieldCount)
                    ;
        }
    }
}
