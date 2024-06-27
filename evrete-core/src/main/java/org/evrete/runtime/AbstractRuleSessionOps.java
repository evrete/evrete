package org.evrete.runtime;

import org.evrete.api.FactHandle;
import org.evrete.api.RuleSession;
import org.evrete.api.Type;
import org.evrete.api.TypeResolver;
import org.evrete.api.annotations.NonNull;
import org.evrete.api.annotations.Nullable;
import org.evrete.api.spi.ValueIndexer;
import org.evrete.util.CommonUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
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
        return bufferInsertMultiple(fact, true, resolveCollections, this.actionBuffer);
    }

    @Override
    @Nullable
    public FactHandle insert0(String type, Object fact, boolean resolveCollections) {
        return bufferInsertMultiple(type, fact, true, resolveCollections, this.actionBuffer);
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
        } else {
            throw new IllegalArgumentException("Unknown type of fact handle: ");
        }
    }

    @Override
    public final void delete(FactHandle handle) {
        _assertActive();
        bufferDelete(handle, true, this.actionBuffer);
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


    void bufferDelete(DefaultFactHandle handle, boolean applyToStorage, @NonNull FactHolder existing, WorkMemoryActionBuffer destination) {
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
        destination.addDelete(new DeltaMemoryAction.Delete(type, deleteSubject, !applyToStorage));
        LOGGER.finer(() -> "New memory delete action buffered for handle " + handle);
    }

    void bufferDelete(FactHandle handle, boolean applyToStorage, WorkMemoryActionBuffer destination) {
        DefaultFactHandle h = unwrapFactHandle(handle);
        ActiveType type = getActiveType(h);
        FactHolder existing = getMemory().getTypeMemory(type.getId()).get(h);
        if (existing != null) {
            bufferDelete(h, applyToStorage, existing, destination);
        }
    }

    void bufferUpdate(boolean applyToStorage, DefaultFactHandle handle, Object newValue, WorkMemoryActionBuffer destination) {
        Objects.requireNonNull(newValue, "Null facts aren't supported");

        Type<?> factType = getTypeResolver().resolve(newValue);
        if(factType == null) {
            LOGGER.warning(() -> "Unknown fact type, UPDATE operation skipped for: " + newValue);
            return;
        }

        ActiveType activeType = getCreateIndexedType(factType);
        TypeMemory typeMemory = getMemory().getTypeMemory(activeType.getId());

        FactHolder existing = typeMemory.get(handle);

        if (existing == null) {
            LOGGER.warning(() -> "Fact not found, UPDATE operation skipped for: " + newValue);
        } else {
            // Check if the fact is of the right type
            //TODO something is wrong here. Rewrite. The checks below will never fail
            Class<?> expectedFactClass = activeType.getValue().getJavaClass();
            Class<?> argClass = newValue.getClass();
            if (expectedFactClass.isAssignableFrom(argClass)) {
                // Buffer deletion (must be first!!!)
                this.bufferDelete(handle, applyToStorage, existing, destination);
                // Buffer new insert operation
                this.bufferInsertSingle(handle, factType, activeType, applyToStorage,newValue, destination);
            } else {
                throw new IllegalArgumentException("Argument type mismatch. Actual '" + argClass + "' vs expected '" + expectedFactClass + "'");
            }
        }
    }

    /**
     * The inner implementation of the {@link RuleSession#insert0(Object, boolean)} method.
     *
     * @param fact               the fact (or facts) to inserts
     * @param resolveCollections collection/array inspection flag
     * @param destination        the buffer where the insert operations will be stored.
     * @return a newly generated fact handle, or null if more than one fact is supplied.
     */
    DefaultFactHandle bufferInsertMultiple(Object fact, boolean applyToStorage, boolean resolveCollections, WorkMemoryActionBuffer destination) {
        Collection<?> rawFacts = factToCollection(fact, resolveCollections);
        return bufferInsertMultiple(rawFacts, applyToStorage, destination);
    }

    /**
     * The inner implementation of the {@link RuleSession#insert0(String, Object, boolean)} method.
     *
     * @param fact               the fact (or facts) to inserts
     * @param applyToStorage     indicates whether to apply the insert operations to the fact storage.
     * @param resolveCollections collection/array inspection flag
     * @param destination        the buffer where the insert operations will be stored.
     * @return a newly generated fact handle, or null if more than one fact is supplied.
     */
    DefaultFactHandle bufferInsertMultiple(String type, Object fact, boolean applyToStorage, boolean resolveCollections, WorkMemoryActionBuffer destination) {
        Collection<?> rawFacts = factToCollection(fact, resolveCollections);
        return bufferInsertMultiple(rawFacts, applyToStorage, type, destination);
    }

    /**
     * Generates new {@link DeltaMemoryAction.Insert} operations and stores them in the provided buffer.
     *
     * @param facts          the facts to convert into insert operations.
     * @param applyToStorage indicates whether to apply the insert operations to the fact storage.
     * @param logicalType    the explicit logical type of the facts being inserted
     * @param destination    the buffer where the insert operations will be stored.
     * @return a newly generated fact handle, or null if more than one fact is supplied.
     */
    private DefaultFactHandle bufferInsertMultiple(Collection<?> facts, boolean applyToStorage, String logicalType, WorkMemoryActionBuffer destination) {
        TypeResolver typeResolver = getTypeResolver();
        Type<?> factType = typeResolver.getType(Objects.requireNonNull(logicalType, "Null fact type is not allowed"));
        if (factType == null) {
            if (warnUnknownTypes) {
                LOGGER.warning(() -> "Unknown type '" + logicalType + "', insert operation skipped.");
            }
            return null;
        } else {
            ActiveType type = getCreateIndexedType(factType);
            DefaultFactHandle last = null;
            for (Object fact : facts) {
                last = bufferInsertSingle(factType, type, applyToStorage, fact, destination);
            }
            return facts.size() == 1 ? last : null;
        }
    }

    /**
     * Generates new {@link DeltaMemoryAction.Insert} operations and stores them in the provided buffer.
     *
     * @param applyToStorage indicates whether to apply the insert operations to the fact storage.
     * @param facts          the facts to convert into insert operations.
     * @param destination    the buffer where the insert operations will be stored.
     * @return a newly generated fact handle, or null if more than one fact is supplied.
     */
    private DefaultFactHandle bufferInsertMultiple(Collection<?> facts, boolean applyToStorage, WorkMemoryActionBuffer destination) {
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
                last = bufferInsertSingle(factType, type, applyToStorage, fact, destination);
            }
        }
        return facts.size() == 1 ? last : null;
    }

    /**
     * Generates a new {@link DeltaMemoryAction.Insert} operation and stores it in the provided buffer.
     *
     * @param applyToStorage indicates whether to apply the insert operations to the fact storage.
     * @param activeType           resolved active type, see {@link ActiveType}.
     * @param fact           the fact to insert.
     * @param destination    the buffer where the insert operation will be stored.
     */
    DefaultFactHandle bufferInsertSingle(Type<?> type, ActiveType activeType, boolean applyToStorage, Object fact, WorkMemoryActionBuffer destination) {
        final DefaultFactHandle factHandle = new DefaultFactHandle(activeType.getId());
        this.bufferInsertSingle(factHandle, type, activeType, applyToStorage, fact, destination);
        return factHandle;
    }

    /**
     * Generates a new {@link DeltaMemoryAction.Insert} operation and stores it in the provided buffer.
     *
     * @param applyToStorage indicates whether to apply the insert operations to the fact storage.
     * @param activeType           resolved active type, see {@link ActiveType}.
     * @param fact           the fact to insert.
     * @param destination    the buffer where the insert operation will be stored.
     */
    private void bufferInsertSingle(DefaultFactHandle factHandle, Type<?> type, ActiveType activeType, boolean applyToStorage, Object fact, WorkMemoryActionBuffer destination) {

        // 1. Read field values
        TypeMemory memory = getMemory().getTypeMemory(activeType.getId());
        ValueIndexer<FactFieldValues> valueIndexer = memory.getFieldValuesIndexer();
        FactFieldValues fieldValues = activeType.readFactValue(type, fact);

        // 2. Index field values
        long valuesId = valueIndexer.getOrCreateId(fieldValues);
        FactHolder factHolder = new FactHolder(factHandle, valuesId, fact);

        // 3. Save the fact in the type memory
        if (applyToStorage) {
            memory.insert(factHolder);
        }

        destination.addInsert(new DeltaMemoryAction.Insert(activeType, factHolder, fieldValues, !applyToStorage));
        LOGGER.finer(() -> "New insert action buffered for fact: " + fact);
    }


    private static Collection<?> factToCollection(Object fact, boolean resolveCollections) {
        Object f = Objects.requireNonNull(fact, "Null facts are not allowed");
        return resolveCollections ?
                CommonUtils.toCollection(f)
                :
                Collections.singleton(f);
    }

}
