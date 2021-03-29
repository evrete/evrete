package org.evrete.spi.minimal;

import org.evrete.api.*;
import org.evrete.runtime.FactType;

import java.util.function.BiPredicate;

class DefaultMemoryFactory implements MemoryFactory {
    private final DefaultValueResolver valueResolver = new DefaultValueResolver();
    private static final int MIN_FACT_STORAGE_CAPACITY = 4098;

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
        return new DefaultFactStorage<>(type, identityFunction, MIN_FACT_STORAGE_CAPACITY);
    }

    @Override
    public SharedBetaFactStorage newBetaStorage(ActiveField[] fields) {
        if (fields.length == 0) {
            return new SharedAlphaData();
        } else {
            return fields.length == 1 ?
                    new SharedBetaDataPlain(fields[0])
                    :
                    new SharedBetaData(fields)
                    ;
        }
    }
}
