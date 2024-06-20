package org.evrete.spi.minimal;

import org.evrete.api.FactHandle;
import org.evrete.api.spi.DeltaGroupedFactStorage;
import org.evrete.api.spi.FactStorage;
import org.evrete.api.spi.MemoryFactory;
import org.evrete.api.spi.ValueIndexer;

public class DefaultMemoryFactory<FH extends FactHandle> implements MemoryFactory<FH> {

    @Override
    public <V> FactStorage<FH, V> newFactStorage(Class<V> valueType) {
        return new DefaultFactStorage<>();
    }

    @Override
    public DeltaGroupedFactStorage<FH> newGroupedFactStorage(Class<FH> keyType) {
        return new DefaultDeltaGroupedFactStorage<>();
    }

    @Override
    public <T> ValueIndexer<T> newValueIndexed(Class<T> valueType) {
        return new DefaultValueIndexer<>();
    }
}
