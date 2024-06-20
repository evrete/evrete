package org.evrete.runtime.rete;

import org.evrete.api.spi.MemoryScope;
import org.evrete.runtime.*;
import org.evrete.util.MappingIterator;

import java.util.Iterator;
import java.util.concurrent.CompletableFuture;

public class ReteSessionEntryNode extends ReteSessionNode {
    private final SessionMemory memory;
    private final AlphaAddress alphaAddress;

    public ReteSessionEntryNode(AbstractRuleSessionBase<?> session, ReteKnowledgeEntryNode knowledgeEntryNode) {
        super(session, knowledgeEntryNode, ReteSessionNode.EMPTY_ARRAY);
        this.memory = session.getMemory();
        this.alphaAddress = knowledgeEntryNode.factType.getAlphaAddress();
    }

    @Override
    String debugName() {
        return "{fact='" + getNodeFactTypes()[0].getVarName() + "', memory=" + alphaAddress + "}";
    }

    /**
     * Returns alpha-memory for this node. We're not making it final in the constructor because alpha-memories
     * can be rebuilt when a new rule is appended to a {@link org.evrete.api.RuleSession}
     * @return alpha-memory
     */
    private TypeAlphaMemory alphaMemory() {
        return memory.getAlphaMemory(this.alphaAddress);
    }

    @Override
    public CompletableFuture<Void> computeDeltaMemoryAsync(DeltaMemoryMode mode) {
        // Entry nodes do not contain inner memories, they're attached directly to alpha memories
        // (which are computed at a different stage).
        return CompletableFuture.completedFuture(null);
    }

    @Override
    Iterator<ConditionMemory.MemoryEntry> iterator(MemoryScope scope) {
        return new MappingIterator<>(
                alphaMemory().keyIterator(scope),
                fieldValues -> ConditionMemory.MemoryEntry.fromEntryNode(
                        fieldValues,
                        scope
                )
        );
    }

    @Override
    public String toString() {
        return "{" +
                "factType='" + getNodeFactTypes()[0].getVarName() +
                "'}";
    }
}
