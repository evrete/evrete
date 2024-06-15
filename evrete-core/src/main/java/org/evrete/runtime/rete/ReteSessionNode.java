package org.evrete.runtime.rete;

import org.evrete.api.spi.MemoryScope;
import org.evrete.runtime.AbstractRuntime;
import org.evrete.runtime.DeltaMemoryMode;
import org.evrete.runtime.StoredCondition;
import org.evrete.runtime.evaluation.DefaultEvaluatorHandle;

import java.util.Iterator;
import java.util.concurrent.CompletableFuture;

public abstract class ReteSessionNode extends ReteNode<ReteSessionNode> {
    public static final ReteSessionNode[] EMPTY_ARRAY = new ReteSessionNode[0];
    final int totalFactTypes;
    private final AbstractRuntime<?,?> runtime;
    final int[] inGroupIndices;

    public ReteSessionNode(AbstractRuntime<?,?> runtime, ReteKnowledgeNode parent, int totalFactTypes, ReteSessionNode[] sourceNodes) {
        super(sourceNodes);
        this.runtime = runtime;
        this.totalFactTypes = totalFactTypes;
        this.inGroupIndices = parent.inGroupIndices;
    }

//    public SessionMemory getMemory() {
//        return session.getMemory();
//    }

    protected StoredCondition getActiveEvaluator(DefaultEvaluatorHandle handle) {
        return runtime.getEvaluatorsContext().get(handle, false);
    }

    public abstract CompletableFuture<Void> computeDeltasRecursively(DeltaMemoryMode mode);

    abstract Iterator<ConditionMemory.MemoryEntry> iterator(MemoryScope scope);

}
