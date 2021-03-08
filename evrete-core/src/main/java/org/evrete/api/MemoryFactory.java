package org.evrete.api;

import java.util.function.BiPredicate;

public interface MemoryFactory {

    SharedBetaFactStorage newBetaStorage(ActiveField[] fields);

    ValueResolver getValueResolver();

    @SuppressWarnings("unused")
    <Z> FactStorage<Z> newFactStorage(Type<?> type, Class<Z> storageClass, BiPredicate<Z, Z> identityFunction);
}
