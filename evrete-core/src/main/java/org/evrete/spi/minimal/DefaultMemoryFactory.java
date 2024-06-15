package org.evrete.spi.minimal;

import org.evrete.api.FactHandle;
import org.evrete.api.spi.DeltaGroupedFactStorage;
import org.evrete.api.spi.FactStorage;
import org.evrete.api.spi.MemoryFactory;

public class DefaultMemoryFactory<FH extends FactHandle> implements MemoryFactory<FH> {

    @Override
    public <V> FactStorage<FH, V> newFactStorage(Class<V> valueType) {
        return new DefaultFactStorage<>();
    }

    @Override
    public <K> DeltaGroupedFactStorage<K, FH> newGroupedFactStorage(Class<K> keyType) {
        return new DefaultDeltaGroupedFactStorage<>();
    }
}
