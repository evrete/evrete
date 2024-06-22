package org.evrete.runtime;

import org.evrete.api.spi.MemoryScope;
import org.evrete.runtime.rete.*;
import org.evrete.util.CombinationIterator;
import org.evrete.util.CommonUtils;
import org.evrete.util.FlatMapIterator;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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

    private Iterator<DefaultFactHandle[]> combinations(ConditionMemory.ScopedValueId[] keys) {
        return new CombinationIterator<>(
                this.currentFactHandles,
                index -> factTypes[index].factIterator(keys[index])
        );
    }
}
