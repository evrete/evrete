package org.evrete.runtime.rete;

import org.evrete.runtime.GroupedFactType;

public class ReteKnowledgeEntryNode extends ReteKnowledgeNode {
    final GroupedFactType factType;

    public ReteKnowledgeEntryNode(GroupedFactType factType) {
        super(factType);
        this.factType = factType;
    }

    public GroupedFactType getFactType() {
        return factType;
    }

}
