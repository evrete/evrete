package org.evrete.runtime;

import java.util.ArrayList;
import java.util.Collection;

public abstract class RuntimeLhs extends AbstractRuntimeLhs {
    private final Collection<BetaEndNode> allBetaEndNodes = new ArrayList<>();

    RuntimeLhs(RuntimeRuleImpl rule, LhsDescriptor descriptor) {
        super(rule, descriptor);
        this.allBetaEndNodes.addAll(getEndNodes());
    }

    static RuntimeLhs factory(RuntimeRuleImpl rule, LhsDescriptor descriptor) {
        return new RuntimeLhsDefault(rule, descriptor);
    }


    public final Collection<BetaEndNode> getAllBetaEndNodes() {
        return allBetaEndNodes;
    }
}
