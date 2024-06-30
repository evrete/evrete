package org.evrete.spi.minimal;

import org.evrete.api.FactHandle;
import org.evrete.api.spi.FactStorage;
import org.evrete.api.spi.GroupingReteMemory;
import org.evrete.api.spi.MemoryFactory;
import org.evrete.api.spi.ValueIndexer;

public class DefaultMemoryFactory<FH extends FactHandle> implements MemoryFactory<FH> {

    @Override
    public <V> FactStorage<FH, V> newFactStorage(Class<V> valueType) {
        return new DefaultFactStorage<>();
    }

    @Override
    public GroupingReteMemory<FH> newGroupedFactStorage(Class<FH> keyType) {
        return new DefaultGroupingReteMemory<>();
    }

    @Override
    public <T> ValueIndexer<T> newValueIndexed(Class<T> valueType) {
        return new DefaultValueIndexer<>();
    }
}
