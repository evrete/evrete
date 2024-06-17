package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.api.annotations.NonNull;
import org.evrete.api.annotations.Nullable;
import org.evrete.api.spi.FactStorage;
import org.evrete.runtime.evaluation.AlphaConditionHandle;
import org.evrete.util.CommonUtils;

import java.util.*;
import java.util.function.Consumer;
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
        try {
            FactHolder factHolder = getFactWrapper((DefaultFactHandle) handle);
            return factHolder == null ? null : (T) factHolder.getFact();
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Unknown fact handle", e);
        }
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
        try {
            DefaultFactHandle h = (DefaultFactHandle) handle;
            bufferUpdate(true, h, newValue, this.actionBuffer);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Unknown fact handle", e);
        }
    }


    @Override
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
        DeltaMemoryAction.Delete op = new DeltaMemoryAction.Delete(type, handle, applyToStorage, deleteSubject);
        destination.add(op);
        LOGGER.finer(() -> "New memory action buffered: " + op);
    }

    boolean bufferDelete(boolean applyToStorage, FactHandle handle, WorkMemoryActionBuffer destination) {
        try {
            DefaultFactHandle h = (DefaultFactHandle) handle;

            ActiveType type = getActiveType(h);
            FactHolder existing = getMemory().getTypeMemory(type.getId()).get(h);
            if (existing == null) {
                return false;
            } else {
                bufferDelete(applyToStorage, h, existing, destination);
                return true;
            }
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Unknown fact handle", e);
        }
    }

    void bufferUpdate(boolean applyToStorage, DefaultFactHandle handle, Object newValue, WorkMemoryActionBuffer destination) {
        Objects.requireNonNull(newValue, "Null facts aren't supported");
        ActiveType type = getActiveType(handle);
        FactHolder existing = getMemory().getTypeMemory(type.getId()).get(handle);

        if (existing == null) {
            LOGGER.warning(() -> "Fact not found, UPDATE operation skipped for: " + newValue);
        } else {
            // Check if the fact is of the right type
            Class<?> expectedFactClass = type.getValue().getJavaClass();
            Class<?> argClass = newValue.getClass();
            if (expectedFactClass.isAssignableFrom(argClass)) {
                // Buffer deletion
                this.bufferDelete(applyToStorage, handle, existing, destination);
                // Buffer new insert operation
                this.bufferInsertSingle(applyToStorage, handle, type, newValue, destination);
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
    DefaultFactHandle bufferInsertActions(boolean applyToStorage, Object fact, boolean resolveCollections, WorkMemoryActionBuffer destination) {
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
    DefaultFactHandle bufferInsertActions(boolean applyToStorage, String type, Object fact, boolean resolveCollections, WorkMemoryActionBuffer destination) {
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
    private DefaultFactHandle bufferInsertActions(boolean applyToStorage, Collection<?> facts, String logicalType, WorkMemoryActionBuffer destination) {
        TypeResolver typeResolver = getTypeResolver();
        Type<?> factType = typeResolver.getType(Objects.requireNonNull(logicalType, "Null fact type is not allowed"));
        if (warnUnknownTypes) {
            LOGGER.warning(() -> "Unknown type '" + logicalType + "', insert operation skipped.");
            return null;
        } else {
            ActiveType type = getCreateIndexedType(factType);
            DefaultFactHandle last = null;
            for (Object fact : facts) {
                DefaultFactHandle handle = generateFactHandle(type, fact);
                bufferInsertSingle(applyToStorage, handle, type, fact, destination);
                last = handle;
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
    private DefaultFactHandle bufferInsertActions(boolean applyToStorage, Collection<?> facts, WorkMemoryActionBuffer destination) {
        TypeResolver typeResolver = getTypeResolver();
        DefaultFactHandle last = null;
        for (Object fact : facts) {
            Type<?> factType = typeResolver.resolve(fact);
            if (factType == null) {
                if (warnUnknownTypes) {
                    LOGGER.warning(() -> "Can not map type for '" + fact.getClass().getName() + "', insert operation skipped.");
                }
            } else {
                ActiveType type = getCreateIndexedType(factType);
                DefaultFactHandle handle = generateFactHandle(type, fact);
                last = handle;
                bufferInsertSingle(applyToStorage, handle, type, fact, destination);
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
     * @param applyToStorage indicates whether to apply the insert operation to the fact storage.
     * @param handle         the assigned fact handle.
     * @param type           resolved active type, see {@link ActiveType}.
     * @param fact           the fact to insert.
     * @param destination    the buffer where the insert operation will be stored.
     */
    void bufferInsertSingle(boolean applyToStorage, DefaultFactHandle handle, ActiveType type, Object fact, WorkMemoryActionBuffer destination) {

        // 1. Generate field values
        final FactHolder factHolder = generateFactHolder(handle, type, fact);

        // 2. Save the fact in the type memory
        if (applyToStorage) {
            getMemory().getTypeMemory(type.getId()).insert(factHolder);
        }

        // 3. Evaluate alpha conditions and assign destination alpha memories
        final RoutedFactHolder routedFactHolder = generateRoutedFactHolder(factHolder, type);

        // 5. Submitting new action
        DeltaMemoryAction.Insert op = new DeltaMemoryAction.Insert(type, handle, applyToStorage, routedFactHolder);
        destination.add(op);

        LOGGER.finer(() -> "New memory action buffered: " + op);
    }

    private Mask<AlphaConditionHandle> alphaConditionStatus(FactHolder factHolder) {
        Mask<AlphaConditionHandle> alphaConditionResults = Mask.alphaConditionsMask();

        //Stream<AlphaConditionHandle> alphaConditionHandleStream = alphaConditionHandles(factHolder.getHandle().getType());

        forEachAlphaConditionHandle(factHolder.getHandle().getType(), new Consumer<AlphaConditionHandle>() {
            @Override
            public void accept(AlphaConditionHandle indexedHandle) {
                StoredCondition evaluator = getEvaluatorsContext().get(indexedHandle.getHandle(), false);
                // Alpha conditions have only one field
                ActiveField activeField = evaluator.getDescriptor().get(0).field();
                IntToValue args = index -> factHolder.getValues().valueAt(activeField.valueIndex());
                alphaConditionResults.set(indexedHandle, evaluator.test(AbstractRuleSessionOps.this, args));
            }
        });

//        alphaConditionHandleStream.forEach(indexedHandle -> {
//            StoredCondition evaluator = getEvaluatorsContext().get(indexedHandle.getHandle(), false);
//            // Alpha conditions have only one field
//            ActiveField activeField = evaluator.getDescriptor().get(0).field();
//            IntToValue args = index -> factHolder.getValues().valueAt(activeField.valueIndex());
//            alphaConditionResults.set(indexedHandle, evaluator.test(AbstractRuleSessionOps.this, args));
//        });

        return alphaConditionResults;
    }

    FactHolder generateFactHolder(DefaultFactHandle handle, ActiveType type, Object fact) {
        // 1. Reading fact values
        FactFieldValues values = type.readFactValue(fact);
        // 2. Returning the result
        return new FactHolder(handle, values, fact);
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

    private DefaultFactHandle generateFactHandle(ActiveType type, Object fact) {
        DefaultFactHandle handle = new DefaultFactHandle(type.getId());
        LOGGER.finer(() -> "Fact handle created for " + fact + " -> " + handle);
        return handle;
    }

    private static Collection<?> factToCollection(Object fact, boolean resolveCollections) {
        Object f = Objects.requireNonNull(fact, "Null facts are not allowed");
        return resolveCollections ?
                CommonUtils.toCollection(f)
                :
                Collections.singleton(f);
    }

}
