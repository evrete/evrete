package org.evrete.api;

import org.evrete.runtime.FactType;

import java.util.function.BiPredicate;

public interface MemoryFactory {

    KeyedFactStorage newBetaStorage(ActiveField[] fields);

    MemoryKeyCollection newMemoryKeyCollection(FactType[] types);

    ValueResolver getValueResolver();

    @SuppressWarnings("unused")
    <Z> FactStorage<Z> newFactStorage(Type<?> type, Class<Z> storageClass, BiPredicate<Z, Z> identityFunction);
}
