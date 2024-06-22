package org.evrete.runtime.rete;

import org.evrete.api.IntToValue;
import org.evrete.api.spi.MemoryScope;
import org.evrete.runtime.*;
import org.evrete.util.CombinationIterator;
import org.evrete.util.CommonUtils;
import org.evrete.util.FlatMapIterator;
import org.evrete.util.MappingIterator;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntFunction;
import java.util.function.ObjIntConsumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ReteSessionConditionNode extends ReteSessionNode {
    private static final Logger LOGGER = Logger.getLogger(ReteSessionConditionNode.class.getName());
    private final ConditionMemory.MemoryEntry[] currentMemoryEntries;
    /**
     * This is a "flattened" state of current memory entries coming from source nodes
     */
    private final FieldValuesMeta[] currentFieldValues;
    private final ResolvedEvaluator evaluator;
    private final ConditionMemory betaMemory;
    private final TypeMemory[] nodeTypeMemories;


    public ReteSessionConditionNode(AbstractRuleSessionBase<?> session, ReteSessionNode[] sourceNodes, ReteKnowledgeConditionNode knowledgeConditionNode) {
        super(session, knowledgeConditionNode, sourceNodes);
        int totalSources = sourceNodes.length;
        this.currentMemoryEntries = new ConditionMemory.MemoryEntry[totalSources];


        FactType[] nodeFactTypes = getNodeFactTypes();
        this.currentFieldValues = new FieldValuesMeta[nodeFactTypes.length];

        this.nodeTypeMemories = new TypeMemory[nodeFactTypes.length];
        for (int i = 0; i < nodeFactTypes.length; i++) {
            this.nodeTypeMemories[i] = session.getMemory().getTypeMemory(nodeFactTypes[i]);
        }

        this.betaMemory = new ConditionMemory();
        this.evaluator = new ResolvedEvaluator(session, knowledgeConditionNode.getEvaluator());

    }

    @Override
    String debugName() {
        return FactType.toSimpleDebugString(this.getNodeFactTypes());
    }

    @Override
    public CompletableFuture<Void> computeDeltaMemoryAsync(DeltaMemoryMode mode) {
        LOGGER.fine(()->"Node " + this.debugName() + " is requesting delta memories from sources: " + debugName(sourceNodes));

        // To compute this node's delta memory, we need the node's source nodes
        // to compute their deltas first.
        return CommonUtils.completeAll(
                        sourceNodes,
                        sourceNode -> sourceNode.computeDeltaMemoryAsync(mode)
                )
                .thenRunAsync(
                        () -> this.computeDeltaLocally(mode),
                        getExecutor()
                );
    }

    public ConditionMemory getBetaMemory() {
        return betaMemory;
    }

    public void deleteAll(Collection<FactHolder> factHolders) {
        betaMemory.deleteAll(ConditionMemory.DeletePredicate.ofMultipleOR(createDeletePredicates(factHolders)));
    }

    private Collection<ConditionMemory.DeletePredicate> createDeletePredicates(Collection<FactHolder> factHolders) {
        MapOfSet<Integer, Long> mapping = new MapOfSet<>();
        for (FactHolder factHolder : factHolders) {
            ActiveType.Idx type = factHolder.getHandle().getType();
            // Get local fact type indices
            Collection<Integer> indicesForType = nodeIndices(type);
            for (Integer index : indicesForType) {
                mapping.add(index, factHolder.getFieldValuesId());
            }
        }

        Collection<ConditionMemory.DeletePredicate> result = new ArrayList<>(mapping.size());
        for(Map.Entry<Integer, Set<Long>> entry : mapping.entrySet()) {
            result.add(new ConditionMemory.DeletePredicate(entry.getKey(), entry.getValue()));
        }

        return result;
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
        LOGGER.fine(()->"Node " + this.debugName() + " starting to compute local delta from sources: " + debugName(sourceNodes));
        // 1. Clear this node's delta memory
        this.betaMemory.clearDeltaMemory();

        // 2. Create iterator over all possible source combinations

        ReteSessionNode[] sourceNodes = sourceNodes();
        Iterator<ConditionMemory.MemoryEntry[]> sourceCombinations = new FlatMapIterator<>(
                sourceScopes,
                scopes -> new ListeningCombinationIterator(
                        currentMemoryEntries,
                        index -> sourceNodes[index].iterator(scopes[index]),
                        this::sourceValueChanged
                )
        );

        // 3. Evaluate the node's condition and save to the destination storage
        sourceCombinations.forEachRemaining(ignored -> evaluateAndSave(saveDestination));
        LOGGER.fine(()->"Node " + this.debugName() + " has finished computing its delta memory. New delta memory size: " + this.betaMemory.size(MemoryScope.DELTA) + ", main memory size: " + this.betaMemory.size(MemoryScope.MAIN));

    }

    private void evaluateAndSave(MemoryScope saveDestination) {
        if (evaluator.test()) {
            ConditionMemory.ScopedValueId[] ids = new ConditionMemory.ScopedValueId[currentFieldValues.length];
            for (int i = 0; i < ids.length; i++) {
                ids[i] = new ConditionMemory.ScopedValueId(currentFieldValues[i].valuesId, currentFieldValues[i].scope) ;
            }

            ConditionMemory.MemoryEntry entry = new ConditionMemory.MemoryEntry(ids);
            this.betaMemory.saveNewEntry(saveDestination, entry);
            LOGGER.finer(()->"Node " + this.debugName() + ", new delta entry: " + Arrays.toString(this.currentFieldValues) + ". Delta memory size: " + this.betaMemory.size(MemoryScope.DELTA));
        }
    }

    /**
     * To evaluate conditions, we need to turn unique <code>long</code> value indices into real objects as
     * described in the {@link org.evrete.api.spi.ValueIndexer} docs.
     *
     * @param memoryEntry the next memory entry from a source node
     * @param sourceIndex the index of the source node
     */
    private void sourceValueChanged(ConditionMemory.MemoryEntry memoryEntry, int sourceIndex) {
        ConditionMemory.ScopedValueId[] valueIds = memoryEntry.getScopedValueIds();
        for (int i = 0; i < valueIds.length; i++) {
            int pos = location(sourceIndex, i);
            long newValuesId = valueIds[i].getValueId();
            MemoryScope newScope = valueIds[i].getScope();

            FieldValuesMeta localVal = currentFieldValues[pos];
            if(localVal == null || localVal.valuesId != newValuesId || localVal.scope != newScope) {
                // We need to read or update cached values
                FactFieldValues fieldValues = nodeTypeMemories[pos].readFieldValues(newValuesId);
                currentFieldValues[pos] = new FieldValuesMeta(fieldValues, newValuesId, newScope);
            }
        }
    }


    @Override
    Iterator<ConditionMemory.MemoryEntry> iterator(MemoryScope scope) {
        return this.betaMemory.iterator(scope);
    }

    /**
     * Returns an iterator over computed memory entries.
     *
     * @param scope the requested inner memory scope
     * @return an iterator over arrays of {@link ConditionMemory.ScopedValueId} values
     */
    public Iterator<ConditionMemory.ScopedValueId[]> memoryIterator(MemoryScope scope) {
        return new MappingIterator<>(iterator(scope), ConditionMemory.MemoryEntry::scopedValues);
    }

    public void commit() {
        this.betaMemory.commit();
    }

    @Override
    public String toString() {

        return "{" +
                "evaluator=" + evaluator +
                ", sourceNodes=" + Arrays.stream(sourceNodes).map(Object::toString).collect(Collectors.joining(", ")) +
                '}';
    }

    private static class ListeningCombinationIterator extends CombinationIterator<ConditionMemory.MemoryEntry> {
        private final ObjIntConsumer<ConditionMemory.MemoryEntry> listener;

        public ListeningCombinationIterator(ConditionMemory.MemoryEntry[] sharedResultArray, IntFunction<Iterator<ConditionMemory.MemoryEntry>> iteratorFunction, ObjIntConsumer<ConditionMemory.MemoryEntry> listener) {
            super(sharedResultArray, iteratorFunction);
            this.listener = listener;
        }

        @Override
        protected ConditionMemory.MemoryEntry advanceIterator(int sourceIndex) {
            ConditionMemory.MemoryEntry entry = super.advanceIterator(sourceIndex);
            listener.accept(entry, sourceIndex);
            return entry;
        }
    }

    private class ResolvedEvaluator {
        private final ResolvedEvaluatorComponent[] components;

        ResolvedEvaluator(AbstractRuleSessionBase<?> session, ReteKnowledgeEvaluator evaluator) {
            // Converting evaluator handles to actual evaluators
            ReteKnowledgeEvaluator.Component[] componentDescriptors = evaluator.getComponents();
            this.components = new ResolvedEvaluatorComponent[componentDescriptors.length];
            for (int i = 0; i < componentDescriptors.length; i++) {
                this.components[i] = new ResolvedEvaluatorComponent(session, componentDescriptors[i]);
            }
        }

        boolean test() {
            for(ResolvedEvaluatorComponent component : components) {
                if(!component.test()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
            return Arrays.toString(components);
        }
    }

    class ResolvedEvaluatorComponent {
        final IntToValue values;
        final StoredCondition condition;
        final AbstractRuleSessionBase<?> session;

        ResolvedEvaluatorComponent(AbstractRuleSessionBase<?> session, ReteKnowledgeEvaluator.Component component) {
            this.session = session;
            this.condition = getActiveEvaluator(component.getDelegate().getCondition());

            final ReteKnowledgeEvaluator.Coordinate[] coordinates = component.getCoordinates();

            this.values = argIndex -> {
                ReteKnowledgeEvaluator.Coordinate c = coordinates[argIndex];
                int inNodeIdx = c.inNodeIdx;
                int valueIndex = c.fieldIdx;
                FactFieldValues fieldValues = currentFieldValues[inNodeIdx].values;
                return fieldValues.valueAt(valueIndex);
            };
        }

        boolean test() {
            return condition.test(session, values);
        }

        @Override
        public String toString() {
            return condition.toString();
        }
    }

    static class FieldValuesMeta {
        private final FactFieldValues values;
        private final long valuesId;
        private final MemoryScope scope;

        public FieldValuesMeta(FactFieldValues values, long valuesId, MemoryScope scope) {
            this.values = values;
            this.valuesId = valuesId;
            this.scope = scope;
        }

        @Override
        public String toString() {
            return "{" +
                    scope +
                    ", " + valuesId + "=" + values +
                    '}';
        }
    }
}
