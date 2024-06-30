package org.evrete.runtime;

import org.evrete.api.FactHandle;
import org.evrete.api.MapEntry;
import org.evrete.api.MemoryStreaming;
import org.evrete.api.Type;
import org.evrete.api.spi.FactStorage;
import org.evrete.api.spi.GroupingReteMemory;
import org.evrete.api.spi.ValueIndexer;
import org.evrete.collections.ArrayMap;
import org.evrete.runtime.evaluation.AlphaConditionHandle;
import org.evrete.util.CompletionManager;
import org.evrete.util.GroupingReteMemoryWrapper;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SessionMemory implements MemoryStreaming {
    private static final Logger LOGGER = Logger.getLogger(SessionMemory.class.getName());
    private final ArrayMap<ActiveType.Idx, TypeMemory> typedMemories = new ArrayMap<>();
    private final ArrayMap<AlphaAddress, TypeAlphaMemory> alphaMemories = new ArrayMap<>();
    private final CompletionManager<ActiveType.Idx, Void> typeMemoryDeployments = new CompletionManager<>();
    private final AtomicLong allocationCounter = new AtomicLong();
    private final AbstractRuleSession<?> runtime;

    SessionMemory(AbstractRuleSession<?> runtime) {
        this.runtime = runtime;
    }

    void clear() {
        typedMemories.forEach(TypeMemory::clear);
        alphaMemories.forEach(TypeAlphaMemory::clear);
    }

    // Used in unit tests
    ArrayMap<AlphaAddress, TypeAlphaMemory> getAlphaMemories() {
        return alphaMemories;
    }

    // Used in unit tests
    CompletionManager<ActiveType.Idx, Void> getTypeMemoryDeployments() {
        return typeMemoryDeployments;
    }

    private Stream<TypeMemory> memoryStream() {
        return typedMemories.values();
    }

    private Stream<TypeMemory> memoryStream(String logicalType) {
        return memoryStream().filter(typeMemory -> typeMemory.getLogicalType().equals(logicalType));
    }

    private Stream<TypeMemory> memoryStream(Class<?> javaType) {
        return memoryStream()
                .filter(memory -> javaType.isAssignableFrom(memory.getJavaType()));
    }


    @Override
    public Stream<MapEntry<FactHandle, Object>> streamFactEntries() {
        return memoryStream().flatMap(TypeMemory::streamFactEntries);
    }

    @Override
    public <T> Stream<MapEntry<FactHandle, T>> streamFactEntries(String type) {
        return memoryStream(type).flatMap(TypeMemory::streamFactEntries);
    }

    @Override
    public <T> Stream<MapEntry<FactHandle, T>> streamFactEntries(Class<T> type) {
        return memoryStream(type).flatMap(TypeMemory::streamFactEntries);
    }

    TypeMemory getTypeMemory(ActiveType.Idx activeType) {
        return typedMemories.getChecked(activeType);
    }

    public TypeMemory getTypeMemory(FactType factType) {
        return getTypeMemory(factType.typeId());
    }

    TypeMemory getTypeMemory(DefaultFactHandle handle) {
        return getTypeMemory(handle.getType());
    }

    CompletableFuture<Void> allocateMemoryIfNotExists(ActiveType.Idx typeId, Set<AlphaAddress> alphaAddresses) {
        return typeMemoryDeployments.enqueue(
                typeId,
                id -> CompletableFuture
                        .runAsync(
                                () -> allocate(id, alphaAddresses),
                                runtime.getService().getExecutor()
                        )
        );
    }

    // Note: this method is effectively synchronized by the CompletionManager
    private void allocate(ActiveType.Idx typeId, Set<AlphaAddress> alphaAddresses) {
        long allocationId = allocationCounter.getAndIncrement();
        LOGGER.fine(() -> "Memory allocation [" + allocationId + "] START. Type: " + typeId + ", alpha locations:" + alphaAddresses + " ....");
        TypeMemory existing = typedMemories.get(typeId);


        // The type is already stored by this time, retrieving it from the runtime context
        ActiveType newActiveType = runtime.getActiveType(typeId);

        final TypeMemory newTypeMemory;
        if (existing == null) {
            // Fresh allocation
            newTypeMemory = new TypeMemory(runtime, newActiveType);
            for (AlphaAddress alphaAddress : alphaAddresses) {
                GroupingReteMemory<DefaultFactHandle> alphaStorage = runtime.newAlphaMemoryStorage();
                this.alphaMemories.put(alphaAddress, new TypeAlphaMemory(alphaStorage, alphaAddress));
            }
            LOGGER.fine(() -> "Type memory allocation [" + allocationId + "]. Blank instances of type memory and alpha locations have been created");

        } else {
            // Does the exiting type the same field count
            int fieldsExisting = existing.getFieldCount();
            int fieldsOfNew = newActiveType.getFieldCount();


            final Collection<TypeAlphaMemory> newAlphaMemories;

            if (fieldsExisting == fieldsOfNew) {
                // The new type has the same set of fields, fact storage doesn't need to be rebuilt
                newTypeMemory = existing;

                // If the fields are the same, then the allocation is about new alpha conditions.
                // Let's see what's changed
                Set<AlphaAddress> exitingAlphaLocations = this.alphaMemories.values()
                        .map(TypeAlphaMemory::getAlphaAddress)
                        .collect(Collectors.toSet());

                Set<AlphaAddress> newAlphaAddresses = alphaAddresses.stream()
                        .filter(alphaAddress -> !exitingAlphaLocations.contains(alphaAddress))
                        .collect(Collectors.toSet());

                if (newAlphaAddresses.isEmpty()) {
                    LOGGER.fine(() -> "Type memory allocation [" + allocationId + "]. The allocation has the same fields no new alpha memories; no action is required");
                    newAlphaMemories = Collections.emptyList();
                } else {
                    LOGGER.fine(() -> "Type memory allocation [" + allocationId + "]. New alpha locations were found: " + newAlphaAddresses);
                    newAlphaMemories = rebuildAlphas(newTypeMemory, newAlphaAddresses, allocationId);
                }
            } else {
                // A new field has been added, the existing fact storage will be rebuilt
                LOGGER.fine(() -> "Type memory allocation [" + allocationId + "]. Existing fields: " + fieldsExisting + ", new fields count: " + fieldsOfNew + ". Fact storage will be rebuilt.");
                newTypeMemory = this.rebuildStorage(existing, newActiveType, allocationId);
                // And so will ALL the alpha memories
                newAlphaMemories = rebuildAlphas(newTypeMemory, newActiveType.getKnownAlphaLocations(), allocationId);
            }

            // Saving new alpha-memories
            for (TypeAlphaMemory alphaMemory : newAlphaMemories) {
                this.alphaMemories.put(alphaMemory.getAlphaAddress(), alphaMemory);
            }

        }
        // Saving new type memories
        typedMemories.put(typeId, newTypeMemory);
        LOGGER.fine(() -> "Type memory allocation [" + allocationId +  "] END");
    }


    private Collection<TypeAlphaMemory> rebuildAlphas(TypeMemory newTypeMemory, Set<AlphaAddress> alphaLocations, long allocationId) {
        ArrayMap<AlphaAddress, TypeAlphaMemory> resultMap = new ArrayMap<>(alphaLocations.size());
        for (AlphaAddress alphaAddress : alphaLocations) {
            resultMap.put(alphaAddress, new TypeAlphaMemory(runtime.newAlphaMemoryStorage(), alphaAddress));
            LOGGER.fine(() -> "Type memory allocation [" + allocationId +  "]. Created new alpha memory for location " + alphaAddress);
        }

        ActiveType newType = newTypeMemory.getType();
        Type<?> type = runtime.getTypeResolver().getType(newType.getValue().getName());
        // Stream stored values, obtain their bitset of alpha conditions and save to matching alpha memories
        newTypeMemory.stream().parallel().forEach(entry -> {
            FactHolder factHolder = entry.getValue();
            FactFieldValues fieldValues = newType.readFactValue(type, factHolder.getFact());
            // Evaluate alpha conditions
            Mask<AlphaConditionHandle> alphaTests = runtime.alphaConditionResults(newType, fieldValues);
            Collection<AlphaAddress> matchingLocations = AlphaAddress.matchingLocations(alphaTests, alphaLocations);
            //Collection<AlphaAddress> matchingLocations = newType.matchingLocations(runtime, fieldValues, alphaLocations);
            for (AlphaAddress alphaAddress : matchingLocations) {
                resultMap.getChecked(alphaAddress).insert(factHolder.getFieldValuesId(), factHolder.getHandle());
            }
        });

        // Commit and return alpha memories
        return resultMap.values().peek(GroupingReteMemoryWrapper::commit).collect(Collectors.toList());
    }

    TypeMemory rebuildStorage(TypeMemory source, ActiveType newType, long allocationId) {
        // Creating new fact storage
        FactStorage<DefaultFactHandle, FactHolder> newStorage = runtime.newTypeFactStorage();
        ValueIndexer<FactFieldValues> newValueIndexer = runtime.newFieldValuesIndexer();
        //TODO fix the mess with types
        Type<?> type = runtime.getTypeResolver().getType(newType.getValue().getName());
        AtomicLong factCounter = new AtomicLong();
        source.stream().parallel().forEach(entry -> {
            DefaultFactHandle handle = entry.getKey();
            FactHolder factHolder = entry.getValue();
            Object fact = factHolder.getFact();
            // We need to keep the same value id because it is possibly referenced in RETE condition nodes.
            long valueId = factHolder.getFieldValuesId();
            FactFieldValues fieldValues = newType.readFactValue(type, fact);
            FactHolder newFactHolder = new FactHolder(handle, valueId, fact);
            newValueIndexer.assignId(valueId, fieldValues);
            newStorage.insert(handle, newFactHolder);
            factCounter.incrementAndGet();
        });
        LOGGER.fine(() -> "Type memory allocation [" + allocationId + "]. Storage rebuild completed for " + newType + ", total facts processed: [" + factCounter.get() + "]");
        return new TypeMemory(newType, newStorage, newValueIndexer);
    }

    public TypeAlphaMemory getAlphaMemory(AlphaAddress alphaAddress) {
        return alphaMemories.getChecked(alphaAddress);
    }
}
