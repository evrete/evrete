package org.evrete.runtime.rete;

import org.evrete.api.IntToValue;
import org.evrete.api.spi.MemoryScope;
import org.evrete.runtime.*;
import org.evrete.runtime.evaluation.DefaultEvaluatorHandle;
import org.evrete.util.CombinationIterator;
import org.evrete.util.CommonUtils;
import org.evrete.util.FlatMapIterator;
import org.evrete.util.MappingIterator;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.logging.Logger;

public class ReteSessionConditionNode extends ReteSessionNode {
    private static final Logger LOGGER = Logger.getLogger(ReteSessionConditionNode.class.getName());
    private final ConditionMemory.MemoryEntry[] currentMemoryEntries;
    private final ResolvedEvaluator evaluator;
    private final ConditionMemory betaMemory;

    public ReteSessionConditionNode(AbstractRuleSessionBase<?> session, int totalFactTypes, ReteSessionNode[] sourceNodes, ReteKnowledgeConditionNode knowledgeConditionNode) {
        super(session, knowledgeConditionNode, totalFactTypes, sourceNodes);
        int totalSources = sourceNodes.length;
        this.currentMemoryEntries = new ConditionMemory.MemoryEntry[totalSources];
        this.betaMemory = new ConditionMemory();
        this.evaluator = new ResolvedEvaluator(session, knowledgeConditionNode.getEvaluator());
    }

    @Override
    public CompletableFuture<Void> computeDeltasRecursively(DeltaMemoryMode mode) {
        // To compute this node's delta memory, we need the node's source nodes
        // to compute their deltas first.
        return CommonUtils.completeAll(
                        sourceNodes,
                        sourceNode -> sourceNode.computeDeltasRecursively(mode)
                )
                .thenRun(() -> this.computeDeltaLocally(mode));
    }

    public ConditionMemory getBetaMemory() {
        return betaMemory;
    }

    public void deleteAll(Predicate<ConditionMemory.MemoryEntry> predicate) {
        betaMemory.deleteAll(predicate);
    }

    void computeDeltaLocally(DeltaMemoryMode mode) {
        final MemoryScope saveDestination;
        final Iterator<MemoryScope[]> sourceScopes;
        if (mode == DeltaMemoryMode.DEFAULT) {
            // The default behavior: iterating over delta combinations
            // and save to delta memory
            saveDestination = MemoryScope.DELTA;
            sourceScopes = MemoryScope.states(MemoryScope.DELTA, new MemoryScope[sourceNodes.length]);
        } else if (mode == DeltaMemoryMode.HOT_DEPLOYMENT) {
            // Hot deployment means evaluating the session's main memories
            // and saving to the node's main storage
            saveDestination = MemoryScope.MAIN;
            sourceScopes = MemoryScope.states(MemoryScope.MAIN, new MemoryScope[sourceNodes.length]);
        } else {
            throw new IllegalStateException("Unknown memory scope mode: " + mode);
        }
        computeDeltaLocally(saveDestination, sourceScopes);
    }

    /**
     * Computes the delta locally and stores the result in the specified memory.
     *
     * @param saveDestination the memory scope where the result will be saved
     * @param sourceScopes    an iterator over sources' scopes
     */
    void computeDeltaLocally(MemoryScope saveDestination, Iterator<MemoryScope[]> sourceScopes) {
        // 1. Clear this node's delta
        this.betaMemory.clearDeltaMemory();

        // 2. Create iterator over all possible source combinations
        Iterator<ConditionMemory.MemoryEntry[]> sourceCombinations = new FlatMapIterator<>(
                sourceScopes,
                scopes -> new CombinationIterator<>(
                        currentMemoryEntries,
                        index -> sourceNodes()[index].iterator(scopes[index])
                )
        );

        // 3. Evaluate the node's condition and save to the destination storage
        sourceCombinations.forEachRemaining(ignored -> {
            if (evaluator.test()) {
                ConditionMemory.MemoryEntry entry = ConditionMemory.MemoryEntry.fromDeltaState(totalFactTypes, currentMemoryEntries, sourceNodes);
                this.betaMemory.saveNewEntry(saveDestination, entry);
            }
        });

    }

    @Override
    Iterator<ConditionMemory.MemoryEntry> iterator(MemoryScope scope) {
        LOGGER.finer(() -> "Requested " + scope + " key iterator for condition node " + this + ". Node memory state: " + betaMemory);
        return this.betaMemory.iterator(scope);
    }

    /**
     * Returns an iterator over computed memory entries.
     * Each entry is an array of {@link FactFieldValues.Scoped} values.
     * Array indices correspond to the indices defined by the corresponding
     * {@link GroupedFactType#getInGroupIndex()}.
     *
     * @param scope the requested inner memory scope
     * @return an iterator over arrays of {@link FactFieldValues.Scoped} values
     */
    public Iterator<FactFieldValues.Scoped[]> memoryIterator(MemoryScope scope) {
        return new MappingIterator<>(iterator(scope), ConditionMemory.MemoryEntry::scopedValues);
    }

    public void commit() {
        this.betaMemory.commit();
    }

    @Override
    public String toString() {
        return "{" +
                "evaluator=" + evaluator +
                '}';
    }

    private class ResolvedEvaluator {
        private final StoredCondition[] evaluators;
        private final IntToValue[] values;
        private final AbstractRuleSessionBase<?> session;

        // TODO Split the arg into runtime and session memory. And not only here
        ResolvedEvaluator(AbstractRuleSessionBase<?> session, ReteKnowledgeConditionNode.Evaluator evaluator) {
            // Converting evaluator handles to actual evaluators
            this.session = session;
            LhsConditionDH<FactType, ActiveField>[] components = evaluator.getComponents();
            this.evaluators = new StoredCondition[components.length];
            this.values = new IntToValue[components.length];
            for (int i = 0; i < components.length; i++) {
                DefaultEvaluatorHandle handle = components[i].getCondition();
                this.evaluators[i] = getActiveEvaluator(handle);

                final ReteKnowledgeConditionNode.Evaluator.Coordinate[] coordinates = evaluator.coordinates[i];
                this.values[i] = argIndex -> {
                    ReteKnowledgeConditionNode.Evaluator.Coordinate c = coordinates[argIndex];
                    int sourceIndex = c.sourceIdx;
                    int arrayIndex = c.inGroupIdx;
                    int valueIndex = c.fieldIdx;
                    return currentMemoryEntries[sourceIndex].get(arrayIndex).values().valueAt(valueIndex);
                };
            }
        }

        boolean test() {
            for (int i = 0; i < evaluators.length; i++) {
                if (!evaluators[i].test(session, values[i])) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
            return Arrays.toString(evaluators);
        }
    }
}
