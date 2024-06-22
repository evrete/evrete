package org.evrete.runtime.rete;

import org.evrete.runtime.FactType;

public class ReteKnowledgeEntryNode extends ReteKnowledgeNode {
    final FactType factType;

    public ReteKnowledgeEntryNode(FactType factType) {
        super(factType);
        this.factType = factType;
    }

}
