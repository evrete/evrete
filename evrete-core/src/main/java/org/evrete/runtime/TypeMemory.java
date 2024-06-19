package org.evrete.runtime;

import org.evrete.api.FactHandle;
import org.evrete.api.MapEntry;
import org.evrete.api.spi.FactStorage;
import org.evrete.api.spi.ValueIndexer;
import org.evrete.util.FactStorageWrapper;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Stream;

public final class TypeMemory extends FactStorageWrapper<DefaultFactHandle, FactHolder> {
    private static final Logger LOGGER = Logger.getLogger(TypeMemory.class.getName());
    private final int fieldCount;
    private final String logicalType;
    private final Class<?> javaType;
    private final ValueIndexer<FactFieldValues> fieldValuesIndexer;
    private final AbstractRuleSession<?> runtime;
    //private final ActiveType type;

    TypeMemory(AbstractRuleSession<?> runtime, ActiveType type, FactStorage<DefaultFactHandle, FactHolder> factStorage, ValueIndexer<FactFieldValues> fieldValuesIndexer) {
        super(factStorage);
        // TODO Resolve the mess with class fields
        //this.type = type;
        this.runtime = runtime;
        this.fieldCount = type.getFieldCount();
        this.logicalType = type.getValue().getName();
        this.javaType = type.getValue().getJavaClass();
        this.fieldValuesIndexer = fieldValuesIndexer;
    }

    TypeMemory(AbstractRuleSession<?> runtime, ActiveType type) {
        this(runtime, type, runtime.newTypeFactStorage(), runtime.newFieldValuesIndexer());
    }

    ValueIndexer<FactFieldValues> getFieldValuesIndexer() {
        return fieldValuesIndexer;
    }

//    CompletableFuture<FactHolder> factToFactHolder(DefaultFactHandle handle, Object fact) {
//        return CompletableFuture.supplyAsync(() -> factToFactHolderSync(handle, fact), runtime.getService().getExecutor());
//    }

//    DefaultFactHandle newFactHandle() {
//        return new DefaultFactHandle(type.getId());
//    }

//    private FactHolder factToFactHolderSync(DefaultFactHandle handle, Object fact) {
//        // 1. Reading fact values
//        FactFieldValues values = this.type.readFactValue(fact);
//
//        // 2. Obtain the values' ID
//        long valuesId = this.fieldValuesIndexer.getOrCreateId(values);
//
//        LOGGER.finer(() -> "Fact holder created for " + fact + ".  Handle: " + handle + ", values ID: " + valuesId);
//
//        return new FactHolder(handle, valuesId, values, fact);
//    }


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

    @Override
    public void clear() {
        super.clear();
        this.fieldValuesIndexer.clear();
    }
}
