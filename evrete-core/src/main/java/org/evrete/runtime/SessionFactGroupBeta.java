package org.evrete.runtime;

import org.evrete.api.spi.MemoryScope;
import org.evrete.runtime.rete.*;
import org.evrete.util.CombinationIterator;
import org.evrete.util.CommonUtils;
import org.evrete.util.FlatMapIterator;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.logging.Logger;

class SessionFactGroupBeta extends SessionFactGroup {
    private static final Logger LOGGER = Logger.getLogger(SessionFactGroupBeta.class.getName());
    private final ReteGraph<ReteSessionNode, ReteSessionEntryNode, ReteSessionConditionNode> graph;
    private final DefaultFactHandle[] currentFactHandles;

    SessionFactGroupBeta(AbstractRuleSessionBase<?> runtime, Beta factGroup) {
        super(runtime, factGroup);

        int totalFactTypes = getEntryNodes().length;
        this.currentFactHandles = new DefaultFactHandle[totalFactTypes];
        // Transform the condition graph and allocate the nodes' memory structures.
        this.graph = factGroup.getGraph().transform(
                ReteSessionNode.class,
                (conditionNode, sources) -> new ReteSessionConditionNode(runtime, totalFactTypes, sources, conditionNode),
                entryNode -> new ReteSessionEntryNode(runtime, totalFactTypes, entryNode)
        );
    }

    @Override
    protected boolean isPlain() {
        return false;
    }

    @Override
    CompletableFuture<Void> processDeleteDeltaActions(Collection<FactHolder> factHolders) {
        Collection<DeletePredicate> predicates = createDeletePredicates(factHolders);
        Iterator<DeletePredicate> iterator = predicates.iterator();
        if (iterator.hasNext()) {
            Predicate<ConditionMemory.MemoryEntry> p = iterator.next();
            while (iterator.hasNext()) {
                p = p.or(iterator.next());
            }
            final Predicate<ConditionMemory.MemoryEntry> combinedPredicate = p;

            // Perform the deletion on each condition node
            Collection<ReteSessionConditionNode> conditionNodes = new LinkedList<>();
            this.graph.forEachConditionNode(conditionNodes::add);
            return CommonUtils.completeAll(
                    conditionNodes,
                    node -> CompletableFuture.runAsync(
                            () -> node.deleteAll(combinedPredicate),
                            executor
                    )
            );
        } else {
            // Nothing to delete in this group
            return CompletableFuture.completedFuture(null);
        }
    }

    private Collection<DeletePredicate> createDeletePredicates(Collection<FactHolder> factHolders) {
        // Splitting actions by active fact types
        MapOfSet<ActiveType.Idx, FactFieldValues> deletesByType = new MapOfSet<>(
                factHolders,
                delete -> delete.getHandle().getType(),
                FactHolder::getValues
        );

        // In a fact group, several fact declarations can be of the same type
        // we'll create delete predicates for each type
        Collection<DeletePredicate> deletePredicates = new LinkedList<>();
        deletesByType.forEach(
                (activeTypeId, factFieldValues) -> {
                    int[] inGroupIndices = inGroupIndicesOfType(activeTypeId);
                    if (inGroupIndices != null) {
                        deletePredicates.add(new DeletePredicate(inGroupIndices, factFieldValues));
                    }
                }
        );

        // Create a combined delete predicate (logical OR)
        return deletePredicates;
    }

    public ReteGraph<ReteSessionNode, ReteSessionEntryNode, ReteSessionConditionNode> getGraph() {
        return graph;
    }

    @Override
    CompletableFuture<Void> buildDeltas(DeltaMemoryMode mode) {
        LOGGER.fine(() -> "Starting to build delta memory for group " + this + " in mode: " + mode);
        return graph.terminalNode().computeDeltasRecursively(mode);
    }

    @Override
    public CompletableFuture<Void> commitDeltas() {
        List<CompletableFuture<Void>> futures = new LinkedList<>();
        this.graph.forEachConditionNode(node -> futures.add(CompletableFuture.runAsync(node::commit, executor)));
        return CommonUtils.completeAll(futures);
    }

    @Override
    Iterator<DefaultFactHandle[]> factHandles(MemoryScope scope) {
        // Fact keys are taken from the terminal node's delta memory
        return new FlatMapIterator<>(
                this.graph.terminalNode().memoryIterator(scope),
                this::combinations
        );
    }

    //    @Override
//    public Iterator<DefaultFactHandle[]> deltaIterator() {
//        // Fact keys are taken from the terminal node's delta memory
//        return new FlatMapIterator<>(
//                this.graph.terminalNode().deltaMemoryIterator(),
//                this::combinations
//        );
//    }

    private Iterator<DefaultFactHandle[]> combinations(FactFieldValues.Scoped[] keys) {
        return new CombinationIterator<>(
                this.currentFactHandles,
                index -> factTypes[index].factIterator(keys[index])
        );
    }

    private static class DeletePredicate implements Predicate<ConditionMemory.MemoryEntry> {
        private final int[] inGroupIndices;
        private final Set<FactFieldValues> valuesToDelete;

        DeletePredicate(int[] inGroupIndices, Set<FactFieldValues> valuesToDelete) {
            this.inGroupIndices = inGroupIndices;
            this.valuesToDelete = valuesToDelete;
        }

        @Override
        public boolean test(ConditionMemory.MemoryEntry memoryEntry) {
            for (int inGroupIndex : inGroupIndices) {
                FactFieldValues.Scoped v = memoryEntry.get(inGroupIndex);
                if (v != null) {
                    if (valuesToDelete.contains(v.values())) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
