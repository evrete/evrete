package org.evrete.runtime;

import org.evrete.api.FactHandle;
import org.evrete.api.RhsContext;
import org.evrete.api.RuntimeRule;
import org.evrete.api.annotations.NonNull;
import org.evrete.api.annotations.Nullable;
import org.evrete.util.MapFunction;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

class RhsContextImpl implements RhsContext {
    private static final Logger LOGGER = Logger.getLogger(RhsContextImpl.class.getName());
    /**
     * The rule's RHS action requires access to facts by name using {@link RhsContext#get(String)}
     * and by value using {@link RhsContext#update(Object)}. This array serves as a cache to avoid
     * repeated {@link org.evrete.api.spi.FactStorage#get(FactHandle)} calls. The array is cleared for
     * each RHS fact combination.
     */
    private final CachedFactEntry[] cache;
    private final DefaultFactHandle[][] currentState;
    private final MapFunction<String, KnowledgeLhs.FactPosition> factPositions;
    private final SessionFactGroup[] factGroups;
    private final SessionRule rule;
    private final WorkMemoryActionBuffer destinationForRuleActions;
    final AtomicLong activationCount = new AtomicLong();

    /**
     * Default constructor
     *
     * @param rule                the rule for which this context is created
     * @param currentGroupedFacts a shared array representing currently processed fact handles
     * @param factPositions       a helper mapping from fact names to their positions inside the current state
     * @param factGroups          the rule's LHS fact groups, having the same dimension as the current state
     */
    public RhsContextImpl(SessionRule rule, DefaultFactHandle[][] currentGroupedFacts, MapFunction<String, KnowledgeLhs.FactPosition> factPositions, SessionFactGroup[] factGroups, WorkMemoryActionBuffer destinationForRuleActions) {
        this.cache = new CachedFactEntry[factPositions.size()];
        this.currentState = currentGroupedFacts;
        this.factPositions = factPositions;
        this.factGroups = factGroups;
        this.rule = rule;
        this.destinationForRuleActions = destinationForRuleActions;
    }

    /**
     * This method is called for each new RHS fact combination i.e, the {@link #currentState} variable is updated.
     */
    RhsContext next() {
        LOGGER.finer(() -> "Rule '" + rule.getName() +  "', current RHS handles: " + Arrays.deepToString(currentState));

        // Clearing the cache for each new fact combination
        Arrays.fill(cache, null);
        this.activationCount.incrementAndGet();
        return this;
    }

    private Object currentFactByFactName(String varName) {
        KnowledgeLhs.FactPosition pos = this.factPositions.apply(varName);
        int inRuleIndex = pos.inRuleIndex;
        CachedFactEntry cachedFactEntry = this.cache[inRuleIndex];
        if (cachedFactEntry == null) {
            DefaultFactHandle handle = this.currentState[pos.groupIndex][pos.inGroupIndex];
            FactHolder wrapper = factGroups[pos.groupIndex].factTypes[pos.inGroupIndex].getFact(handle);
            this.cache[inRuleIndex] = new CachedFactEntry(wrapper, handle);
            return wrapper.getFact();
        } else {
            return cachedFactEntry.wrapper.getFact();
        }
    }

    /**
     * Searches for a fact in the cache and returns its corresponding FactWrapper.
     *
     * <p>Note: This method assumes that the provided fact was previously retrieved via a `get`
     * method and is thus present in the cache. If the fact is not found, an
     * IllegalArgumentException is thrown.
     *
     * @param fact the fact to search for in the cache; it must have been previously retrieved via a `get` method
     * @return the FactWrapper associated with the provided fact or null if not found
     */
    @Nullable
    private CachedFactEntry findByFact(Object fact) {
        // Searching through the cache for the corresponding FactWrapper
        for (CachedFactEntry entry : this.cache) {
            if (entry != null && entry.wrapper.getFact() == fact) {
                return entry;
            }
        }
        return null;
    }

    @Override
    public FactHandle insert0(Object fact, boolean resolveCollections) {
        return rule.getRuntime().bufferInsertMultiple(fact, false, resolveCollections, this.destinationForRuleActions);
    }

    @Override
    public FactHandle insert0(String type, Object fact, boolean resolveCollections) {
        return rule.getRuntime().bufferInsertMultiple(fact, false, resolveCollections, this.destinationForRuleActions);
    }

    @Override
    public void delete(FactHandle handle) {
        rule.getRuntime().bufferDelete(handle, false, this.destinationForRuleActions);
    }

    @Override
    public void update(FactHandle handle, Object newValue) {
        rule.getRuntime().bufferUpdate(false, (DefaultFactHandle) handle, newValue, this.destinationForRuleActions);
    }

    @Override
    public final RhsContext update(Object obj) {
        CachedFactEntry entry = findByFact(Objects.requireNonNull(obj, "Facts not allowed to be null"));
        if (entry == null) {
            LOGGER.warning(()->"Rule '" + rule.getName() + "'. Fact " + obj + " is not known to the context. This operation is only possible for facts previously retrieved via a get(...) method. The UPDATE operation skipped.");
        } else {
            rule.getRuntime().bufferUpdate(false, entry.handle, obj, this.destinationForRuleActions);
        }
        return this;
    }

    @Override
    public final RhsContext delete(Object obj) {
        Objects.requireNonNull(obj, "Facts not allowed to be null");

        if (obj instanceof FactHandle) {
            delete((FactHandle) obj);
        } else {
            deleteCurrentFact(obj);
        }
        return this;
    }

    private void deleteCurrentFact(Object obj) {
        CachedFactEntry entry = findByFact(obj);
        if (entry == null) {
            LOGGER.warning(()->"Rule '" + rule.getName() +  "'. Fact " + obj + " is not known to the context. This operation is only possible for facts previously retrieved via a get(...) method. The DELETE operation skipped.");
        } else {
            rule.getRuntime().bufferDelete(entry.handle, false, entry.wrapper, this.destinationForRuleActions);
        }
    }


    @Override
    public RuntimeRule getRule() {
        return this.rule;
    }

    @Override
    public Object getObject(String name) {
        return currentFactByFactName(name);
    }

    private static class CachedFactEntry {
        final FactHolder wrapper;
        final DefaultFactHandle handle;

        CachedFactEntry(@NonNull FactHolder wrapper, @NonNull DefaultFactHandle handle) {
            this.wrapper = wrapper;
            this.handle = handle;
        }
    }
}
