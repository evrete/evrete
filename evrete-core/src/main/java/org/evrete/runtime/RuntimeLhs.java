package org.evrete.runtime;

import java.util.ArrayList;
import java.util.Collection;

public class RuntimeLhs extends AbstractRuntimeLhs {
    private final Collection<BetaEndNode> allBetaEndNodes = new ArrayList<>();

    RuntimeLhs(RuntimeRuleImpl rule, LhsDescriptor descriptor) {
        super(rule, descriptor);
        this.allBetaEndNodes.addAll(getEndNodes());
    }

    public final Collection<BetaEndNode> getAllBetaEndNodes() {
        return allBetaEndNodes;
    }
}
