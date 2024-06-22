package org.evrete.runtime;

import org.evrete.api.annotations.NonNull;
import org.evrete.api.spi.MemoryScope;
import org.evrete.runtime.rete.*;
import org.evrete.util.CombinationIterator;
import org.evrete.util.CommonUtils;
import org.evrete.util.FlatMapIterator;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
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
                (conditionNode, sources) -> new ReteSessionConditionNode(runtime, sources, conditionNode),
                entryNode -> new ReteSessionEntryNode(runtime, entryNode)
        );
    }

    @Override
    protected boolean isPlain() {
        return false;
    }

    @Override
    CompletableFuture<Void> processDeleteDeltaActions(Collection<FactHolder> factHolders) {
        Collection<ReteSessionConditionNode> conditionNodes = new LinkedList<>();
        this.graph.forEachConditionNode(conditionNodes::add);
        return CommonUtils.completeAll(
                conditionNodes,
                node -> CompletableFuture.runAsync(
                        () -> node.deleteAll(factHolders),
                        executor
                )
        );


//        Collection<DeletePredicate> predicates = createDeletePredicates(factHolders);
//        Iterator<DeletePredicate> iterator = predicates.iterator();
//        if (iterator.hasNext()) {
//            Predicate<ConditionMemory.MemoryEntry> p = iterator.next();
//            while (iterator.hasNext()) {
//                p = p.or(iterator.next());
//            }
//            final Predicate<ConditionMemory.MemoryEntry> combinedPredicate = p;
//
//            // Perform the deletion on each condition node
//            Collection<ReteSessionConditionNode> conditionNodes = new LinkedList<>();
//            this.graph.forEachConditionNode(conditionNodes::add);
//            return CommonUtils.completeAll(
//                    conditionNodes,
//                    node -> CompletableFuture.runAsync(
//                            () -> node.deleteAll(combinedPredicate),
//                            executor
//                    )
//            );
//        } else {
//            // Nothing to delete in this group
//            return CompletableFuture.completedFuture(null);
//        }
    }

    private Collection<DeletePredicate> createDeletePredicates(Collection<FactHolder> factHolders) {

        MapOfSet<Integer, Long> mapping = new MapOfSet<>();
        for (FactHolder factHolder : factHolders) {
            ActiveType.Idx type = factHolder.getHandle().getType();
            // Get local fact type indices
            Collection<Integer> indicesForType = nodeIndices(type);
            for (Integer index : indicesForType) {
                mapping.add(index, factHolder.getFieldValuesId());
            }
        }

        Collection<DeletePredicate> result = new ArrayList<>(mapping.size());
        for(Map.Entry<Integer, Set<Long>> entry : mapping.entrySet()) {
            result.add(new DeletePredicate(entry.getKey(), entry.getValue()));
        }

        return result;
    }

    public ReteGraph<ReteSessionNode, ReteSessionEntryNode, ReteSessionConditionNode> getGraph() {
        return graph;
    }

    @Override
    CompletableFuture<Void> buildDeltas(DeltaMemoryMode mode) {
        LOGGER.fine(() -> "Starting to build delta memory for group " + FactType.toSimpleDebugString(this.factTypes) + " in mode: " + mode);
        return graph.terminalNode().computeDeltaMemoryAsync(mode);
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

    private Iterator<DefaultFactHandle[]> combinations(ConditionMemory.ScopedValueId[] keys) {
        return new CombinationIterator<>(
                this.currentFactHandles,
                index -> factTypes[index].factIterator(keys[index])
        );
    }

    private static class DeletePredicate implements Predicate<ConditionMemory.MemoryEntry> {
        private final int index;
        private final Set<Long> valuesToDelete;

        DeletePredicate(int index, Set<Long> valuesToDelete) {
            this.index = index;
            this.valuesToDelete = valuesToDelete;
        }

        @Override
        public boolean test(ConditionMemory.MemoryEntry memoryEntry) {
            ConditionMemory.ScopedValueId v = memoryEntry.getScopedValueIds()[index];
            return valuesToDelete.contains(v.getValueId());
        }

        @NonNull
        static Predicate<ConditionMemory.MemoryEntry> ofMultipleOR(@NonNull Collection<DeletePredicate> predicates) {
            Iterator<DeletePredicate> iterator = predicates.iterator();
            if (iterator.hasNext()) {
                Predicate<ConditionMemory.MemoryEntry> predicate = iterator.next();
                while (iterator.hasNext()) {
                    predicate = predicate.or(iterator.next());
                }
                return predicate;
            } else {
                return memoryEntry -> false;
            }
        }
    }
}
