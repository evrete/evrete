package org.evrete.api;

import org.evrete.api.annotations.Unstable;

import java.util.function.BiPredicate;

/**
 * The MemoryFactory interface defines methods for creating different types of memories used in the engine.
 * @see org.evrete.api.spi.MemoryFactoryProvider
 */
@Unstable
public interface MemoryFactory {

    /**
     * Creates a new instance of {@link KeyedFactStorage} (RETE alpha and beta memories) with the specified field count.
     *
     * @param fieldCount the number of fields in the KeyedFactStorage
     * @return a new instance of {@link KeyedFactStorage}
     */
    KeyedFactStorage newBetaStorage(int fieldCount);

    MemoryKeyCollection newMemoryKeyCollection();

    ValueResolver getValueResolver();

    /**
     * Creates a new instance of {@link FactStorage} for the specified type of facts, storage class, and identity function.
     *
     * @param type             the type of facts that the storage will handle
     * @param storageClass     the class of the storage implementation
     * @param identityFunction the identity function to determine if two facts are equal
     * @param <Z>              the type parameter representing the facts
     * @return a new instance of {@link FactStorage} for the specified type
     */
    <Z> FactStorage<Z> newFactStorage(Type<?> type, Class<Z> storageClass, BiPredicate<Z, Z> identityFunction);
}
