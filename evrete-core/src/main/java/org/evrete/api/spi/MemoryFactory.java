package org.evrete.api.spi;

import org.evrete.api.FactHandle;
import org.evrete.api.ValuesPredicate;

/**
 * The MemoryFactory interface defines methods for creating different types of memories used in the engine.
 * @param <FH> the type of the {@link FactHandle} implementation to be used
 * @see org.evrete.api.spi.MemoryFactoryProvider
 */
public interface MemoryFactory<FH extends FactHandle> {

    <V> FactStorage<FH, V> newFactStorage(Class<V> valueType);

    DeltaGroupedFactStorage<FH> newGroupedFactStorage(Class<FH> valueType);

    <T> ValueIndexer<T> newValueIndexed(Class<T> valueType);
}
