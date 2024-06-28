package org.evrete.runtime;

import org.evrete.api.FactHandle;
import org.evrete.api.MapEntry;
import org.evrete.api.spi.FactStorage;
import org.evrete.api.spi.ValueIndexer;
import org.evrete.util.FactStorageWrapper;

import java.util.stream.Stream;

public final class TypeMemory extends FactStorageWrapper<DefaultFactHandle, FactHolder> {
    private final int fieldCount;
    private final String logicalType;
    private final Class<?> javaType;
    private final ValueIndexer<FactFieldValues> fieldValuesIndexer;
    private final ActiveType type;

    TypeMemory(ActiveType type, FactStorage<DefaultFactHandle, FactHolder> factStorage, ValueIndexer<FactFieldValues> fieldValuesIndexer) {
        super(factStorage);
        // TODO Resolve the mess with class fields
        this.type = type;
        this.fieldCount = type.getFieldCount();
        this.logicalType = type.getValue().getName();
        this.javaType = type.getValue().getJavaClass();
        this.fieldValuesIndexer = fieldValuesIndexer;
    }

    public ActiveType getType() {
        return type;
    }

    TypeMemory(AbstractRuleSession<?> runtime, ActiveType type) {
        this(type, runtime.newTypeFactStorage(), runtime.newFieldValuesIndexer());
    }

    ValueIndexer<FactFieldValues> getFieldValuesIndexer() {
        return fieldValuesIndexer;
    }

    public FactFieldValues readFieldValues(long valueId) {
        return this.fieldValuesIndexer.get(valueId);
    }

    public String getLogicalType() {
        return logicalType;
    }

    public Class<?> getJavaType() {
        return javaType;
    }

    public int getFieldCount() {
        return fieldCount;
    }

    @SuppressWarnings("unchecked")
    public <T> Stream<MapEntry<FactHandle, T>> streamFactEntries() {
        return stream().map(entry -> new MapEntry<>(entry.getKey(), (T) entry.getValue().getFact()));
    }

    public void insert(FactHolder value) {
        insert(value.getHandle(), value);
    }

}
