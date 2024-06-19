package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.api.annotations.NonNull;
import org.evrete.api.annotations.Nullable;
import org.evrete.api.spi.FactStorage;
import org.evrete.api.spi.ValueIndexer;
import org.evrete.runtime.evaluation.AlphaConditionHandle;
import org.evrete.util.CommonUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * <p>
 * Base session class with all the methods related to operations on facts
 * </p>
 *
 * @param <S> session type parameter
 */
public abstract class AbstractRuleSessionOps<S extends RuleSession<S>> extends AbstractRuleSessionBase<S> {
    private static final Logger LOGGER = Logger.getLogger(AbstractRuleSessionOps.class.getName());
    private final WorkMemoryActionBuffer actionBuffer;

    AbstractRuleSessionOps(KnowledgeRuntime knowledge) {
        super(knowledge);
        this.actionBuffer = new WorkMemoryActionBuffer();
    }

    @Override
    @SuppressWarnings("unchecked")
    public final <T> T getFact(FactHandle handle) {
        _assertActive();
        DefaultFactHandle h = unwrapFactHandle(handle);
        FactHolder factHolder = getFactWrapper(h);
        return factHolder == null ? null : (T) factHolder.getFact();
    }

    @Override
    @Nullable
    public FactHandle insert0(Object fact, boolean resolveCollections) {
        return bufferInsertActions(true, fact, resolveCollections, this.actionBuffer);
    }

    @Override
    @Nullable
    public FactHandle insert0(String type, Object fact, boolean resolveCollections) {
        return bufferInsertActions(true, type, fact, resolveCollections, this.actionBuffer);
    }

    @Override
    public final void update(FactHandle handle, Object newValue) {
        _assertActive();
        DefaultFactHandle h = unwrapFactHandle(handle);
        bufferUpdate(true, h, newValue, this.actionBuffer);
    }


    DefaultFactHandle unwrapFactHandle(FactHandle handle) {
        if (handle instanceof DefaultFactHandle) {
            return (DefaultFactHandle) handle;
        } else if (handle instanceof FutureFactHandle) {
            return ((FutureFactHandle) handle).get();
        } else {
            throw new IllegalArgumentException("Unknown type of fact handle: ");
        }
    }

    @Override
    //TODO add tests
    public final boolean delete(FactHandle handle) {
        _assertActive();
        return bufferDelete(true, handle, this.actionBuffer);
    }


    WorkMemoryActionBuffer getActionBuffer() {
        return actionBuffer;
    }

    /**
     * Implementation of the external {@link RuleSession#getFact(FactHandle)} method.
     *
     * @param handle fact handle
     * @return stored fact wrapper ot null if nor found
     */
    FactHolder getFactWrapper(DefaultFactHandle handle) {
        return getMemory().getTypeMemory(handle).get(handle);
    }


//    boolean deleteAndBufferDeleteOp(DefaultFactHandle h) {
//        // 1. Delete from the type memory immediately
//        FactWrapper stored = getMemory().get(h).remove(h);
//        if (stored != null) {
//            // 2. Register
//            this.actionBuffer.add(new DeltaMemoryAction.Delete(h, stored));
//            return true;
//        } else {
//            return false;
//        }
//    }


    void bufferDelete(boolean applyToStorage, DefaultFactHandle handle, @NonNull FactHolder existing, WorkMemoryActionBuffer destination) {
        final FactHolder deleteSubject;
        ActiveType type = getActiveType(handle);

        if (applyToStorage) {
            deleteSubject = getMemory().getTypeMemory(handle).remove(handle);
            if (deleteSubject == null) {
                throw new IllegalArgumentException("Unknown fact handle: " + handle);
            }
        } else {
            deleteSubject = existing;
        }
        destination.addDelete(type, applyToStorage, deleteSubject);
        LOGGER.finer(() -> "New memory delete action buffered for handle " + handle);
    }

    boolean bufferDelete(boolean applyToStorage, FactHandle handle, WorkMemoryActionBuffer destination) {
        DefaultFactHandle h = unwrapFactHandle(handle);
        ActiveType type = getActiveType(h);
        FactHolder existing = getMemory().getTypeMemory(type.getId()).get(h);
        if (existing == null) {
            return false;
        } else {
            bufferDelete(applyToStorage, h, existing, destination);
            return true;
        }
    }

    void bufferUpdate(boolean applyToStorage, DefaultFactHandle handle, Object newValue, WorkMemoryActionBuffer destination) {
        Objects.requireNonNull(newValue, "Null facts aren't supported");
        ActiveType type = getActiveType(handle);
        TypeMemory typeMemory = getMemory().getTypeMemory(type.getId());

        FactHolder existing = typeMemory.get(handle);

        if (existing == null) {
            LOGGER.warning(() -> "Fact not found, UPDATE operation skipped for: " + newValue);
        } else {
            // Check if the fact is of the right type
            Class<?> expectedFactClass = type.getValue().getJavaClass();
            Class<?> argClass = newValue.getClass();
            if (expectedFactClass.isAssignableFrom(argClass)) {
                // Buffer deletion (must be first!!!)
                this.bufferDelete(applyToStorage, handle, existing, destination);
                // Buffer new insert operation
                FutureFactHandle insertFuture = this.bufferInsertSingle(handle, applyToStorage, type, newValue, destination);
                if (applyToStorage) {
                    // Wait for the op to complete
                    insertFuture.get();
                }
            } else {
                throw new IllegalArgumentException("Argument type mismatch. Actual '" + argClass + "' vs expected '" + expectedFactClass + "'");
            }
        }
    }

    /**
     * The inner implementation of the {@link RuleSession#insert0(Object, boolean)} method.
     *
     * @param applyToStorage     indicates whether to apply the insert operations to the fact storage.
     * @param fact               the fact (or facts) to inserts
     * @param resolveCollections collection/array inspection flag
     * @param destination        the buffer where the insert operations will be stored.
     * @return a newly generated fact handle, or null if more than one fact is supplied.
     */
    FutureFactHandle bufferInsertActions(boolean applyToStorage, Object fact, boolean resolveCollections, WorkMemoryActionBuffer destination) {
        Collection<?> rawFacts = factToCollection(fact, resolveCollections);
        return bufferInsertActions(applyToStorage, rawFacts, destination);
    }

    /**
     * The inner implementation of the {@link RuleSession#insert0(String, Object, boolean)} method.
     *
     * @param applyToStorage     indicates whether to apply the insert operations to the fact storage.
     * @param fact               the fact (or facts) to inserts
     * @param resolveCollections collection/array inspection flag
     * @param destination        the buffer where the insert operations will be stored.
     * @return a newly generated fact handle, or null if more than one fact is supplied.
     */
    FutureFactHandle bufferInsertActions(boolean applyToStorage, String type, Object fact, boolean resolveCollections, WorkMemoryActionBuffer destination) {
        Collection<?> rawFacts = factToCollection(fact, resolveCollections);
        return bufferInsertActions(applyToStorage, rawFacts, type, destination);
    }

    /**
     * Generates new {@link DeltaMemoryAction.Insert} operations and stores them in the provided buffer.
     * The method distinguishes between external inserts (those caused by the {@link RuleSession#insert(Object)}
     * method and its overloads) and inserts caused by RHS (Right-Hand Side) actions. In the latter case,
     * the newly generated fact wrappers are not immediately stored in the respective {@link FactStorage}
     * but are rather delayed until other RHS actions are completed.
     *
     * @param applyToStorage indicates whether to apply the insert operations to the fact storage.
     * @param facts          the facts to convert into insert operations.
     * @param logicalType    the explicit logical type of the facts being inserted
     * @param destination    the buffer where the insert operations will be stored.
     * @return a newly generated fact handle, or null if more than one fact is supplied.
     */
    private FutureFactHandle bufferInsertActions(boolean applyToStorage, Collection<?> facts, String logicalType, WorkMemoryActionBuffer destination) {
        TypeResolver typeResolver = getTypeResolver();
        Type<?> factType = typeResolver.getType(Objects.requireNonNull(logicalType, "Null fact type is not allowed"));
        if (factType == null) {
            if (warnUnknownTypes) {
                LOGGER.warning(() -> "Unknown type '" + logicalType + "', insert operation skipped.");
            }
            return null;
        } else {
            ActiveType type = getCreateIndexedType(factType);
            FutureFactHandle last = null;
            for (Object fact : facts) {
                last = bufferInsertSingle(null, applyToStorage, type, fact, destination);
            }
            return facts.size() == 1 ? last : null;
        }
    }

    /**
     * Generates new {@link DeltaMemoryAction.Insert} operations and stores them in the provided buffer.
     * The method distinguishes between external inserts (those caused by the {@link RuleSession#insert(Object)}
     * method and its overloads) and inserts caused by RHS (Right-Hand Side) actions. In the latter case,
     * the newly generated fact wrappers are not immediately stored in the respective {@link FactStorage}
     * but are rather delayed until other RHS actions are completed.
     *
     * @param applyToStorage indicates whether to apply the insert operations to the fact storage.
     * @param facts          the facts to convert into insert operations.
     * @param destination    the buffer where the insert operations will be stored.
     * @return a newly generated fact handle, or null if more than one fact is supplied.
     */
    private FutureFactHandle bufferInsertActions(boolean applyToStorage, Collection<?> facts, WorkMemoryActionBuffer destination) {
        TypeResolver typeResolver = getTypeResolver();
        FutureFactHandle last = null;
        for (Object fact : facts) {
            Type<?> factType = typeResolver.resolve(fact);
            if (factType == null) {
                if (warnUnknownTypes) {
                    LOGGER.warning(() -> "Can not map type for '" + fact.getClass().getName() + "', insert operation skipped.");
                }
            } else {
                ActiveType type = getCreateIndexedType(factType);
                last = bufferInsertSingle(null, applyToStorage, type, fact, destination);
                ;
            }
        }
        return facts.size() == 1 ? last : null;
    }


    /**
     * Generates a new {@link DeltaMemoryAction.Insert} operation and stores it in the provided buffer.
     * The method distinguishes between external inserts (those caused by the {@link RuleSession#insert(Object)}
     * method and its overloads) and inserts caused by RHS (Right-Hand Side) actions. In the latter case,
     * the newly generated fact wrapper is not immediately stored in the respective {@link FactStorage}
     * but rather delayed until other RHS actions are completed.
     *
     * @param handle         fact handle to assign to the fact or null if a new handle needs to be generated
     * @param applyToStorage indicates whether to apply the insert operation to the fact storage.
     * @param type           resolved active type, see {@link ActiveType}.
     * @param fact           the fact to insert.
     * @param destination    the buffer where the insert operation will be stored.
     */
    FutureFactHandle bufferInsertSingle(@Nullable DefaultFactHandle handle, boolean applyToStorage, ActiveType type, Object fact, WorkMemoryActionBuffer destination) {

        TypeMemory memory = getMemory().getTypeMemory(type.getId());
        ValueIndexer<FactFieldValues> valueIndexer = memory.getFieldValuesIndexer();

        // 1. Generate fact holder
        CompletableFuture<FactHolder> factHolder = this.generateFactHolder(handle, type, fact, valueIndexer)
                .thenApply(new Function<FactHolder, FactHolder>() {
                    @Override
                    public FactHolder apply(FactHolder factHolder) {
                        if (applyToStorage) {
                            // 2. Save the fact in the type memory if needed
                            memory.insert(factHolder);
                        }
                        return factHolder;
                    }
                });


        // 3. Evaluate alpha conditions and assign destination alpha memories
        final CompletableFuture<RoutedFactHolder> routedFactHolder = factHolder.thenApply(holder -> generateRoutedFactHolder(holder, type));

        // 5. Submitting new action
        //DeltaMemoryAction.Insert op = new DeltaMemoryAction.Insert(type, handle, applyToStorage, routedFactHolder);
        destination.addInsert(type, applyToStorage, routedFactHolder);

        LOGGER.finer(() -> "New insert action buffered for fact: " + fact);
        return new FutureFactHandle(routedFactHolder.thenApply(holder -> holder.getFactHolder().getHandle()));
    }

    private Mask<AlphaConditionHandle> alphaConditionStatus(FactHolder factHolder) {
        Mask<AlphaConditionHandle> alphaConditionResults = Mask.alphaConditionsMask();

        forEachAlphaConditionHandle(factHolder.getHandle().getType(), indexedHandle -> {
            StoredCondition evaluator = getEvaluatorsContext().get(indexedHandle.getHandle(), false);
            // Alpha conditions have only one field
            ActiveField activeField = evaluator.getDescriptor().get(0).field();
            IntToValue args = index -> factHolder.getValues().valueAt(activeField.valueIndex());
            alphaConditionResults.set(indexedHandle, evaluator.test(AbstractRuleSessionOps.this, args));
        });

        return alphaConditionResults;
    }

    CompletableFuture<FactHolder> generateFactHolder(@Nullable DefaultFactHandle handle, ActiveType type, Object fact, ValueIndexer<FactFieldValues> valueIndexer) {
        return CompletableFuture.supplyAsync(() -> generateFactHolderSync(handle, type, fact, valueIndexer), getService().getExecutor());
    }

    private FactHolder generateFactHolderSync(@Nullable DefaultFactHandle handle, ActiveType type, Object fact, ValueIndexer<FactFieldValues> valueIndexer) {
        // 1. Reading fact values
        FactFieldValues values = type.readFactValue(fact);
        // 2. Index the values
        long valueId = valueIndexer.getOrCreateId(values);
        // 3. Generate new handle if needed
        DefaultFactHandle h = handle == null ? new DefaultFactHandle(type.getId()) : handle;
        return new FactHolder(h, valueId, values, fact);
    }

    FactHolder generateFactHolderSync(@Nullable DefaultFactHandle handle, ActiveType type, Object fact, long valueId) {
        // 1. Reading fact values
        FactFieldValues values = type.readFactValue(fact);
        // 2. Generate new handle if needed
        DefaultFactHandle h = handle == null ? new DefaultFactHandle(type.getId()) : handle;
        return new FactHolder(h, valueId, values, fact);
    }


    private RoutedFactHolder generateRoutedFactHolder(FactHolder factHolder, ActiveType type) {
        // 1. Evaluate alpha conditions and filter matching sets of alpha conditions
        List<AlphaAddress> matching = matchingLocations(factHolder, type.getKnownAlphaLocations());

        // 3. Returning the result
        return new RoutedFactHolder(factHolder, matching);
    }


    List<AlphaAddress> matchingLocations(FactHolder factHolder, Set<AlphaAddress> scope) {
        // 1. Evaluate alpha conditions
        Mask<AlphaConditionHandle> alphaConditionResults = alphaConditionStatus(factHolder);

        // 2. Collecting matching sets of alpha conditions (each set means a separate alpha memory)
        List<AlphaAddress> matching = new ArrayList<>(scope.size());
        for (AlphaAddress alphaAddress : scope) {
            if (alphaConditionResults.containsAll(alphaAddress.getMask())) {
                matching.add(alphaAddress);
                LOGGER.finer(() -> "Location " + alphaAddress + " is selected for " + factHolder);
            } else {
                LOGGER.finer(() -> "Location " + alphaAddress + " is NOT selected for " + factHolder);
            }
        }

        // 3. Returning the result
        return matching;
    }

//    private DefaultFactHandle generateFactHandle(ActiveType type, Object fact) {
//        DefaultFactHandle handle = new DefaultFactHandle(type.getId());
//        LOGGER.finer(() -> "Fact handle created for " + fact + " -> " + handle);
//        return handle;
//    }

    private static Collection<?> factToCollection(Object fact, boolean resolveCollections) {
        Object f = Objects.requireNonNull(fact, "Null facts are not allowed");
        return resolveCollections ?
                CommonUtils.toCollection(f)
                :
                Collections.singleton(f);
    }

}
