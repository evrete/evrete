package org.evrete.spi.minimal;

import org.evrete.api.*;
import org.evrete.runtime.FactType;

import java.util.function.BiPredicate;

class DefaultMemoryFactory implements MemoryFactory {
    private final DefaultValueResolver valueResolver = new DefaultValueResolver();


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
