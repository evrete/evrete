package org.evrete.runtime;

import org.evrete.api.RhsContext;
import org.evrete.api.RuntimeRule;
import org.evrete.runtime.memory.BetaEndNode;
import org.evrete.runtime.memory.Buffer;
import org.evrete.runtime.memory.SessionMemory;

import java.util.Collection;
import java.util.function.Consumer;


public class RuntimeRuleImpl extends AbstractRuntimeRule implements MemoryChangeListener, RuntimeRule {
    private final RuntimeLhs lhs;
    private final Buffer buffer;

    public RuntimeRuleImpl(RuleDescriptor rd, SessionMemory memory) {
        super(rd, memory);
        this.buffer = new Buffer();
        this.lhs = RuntimeLhs.factory(this, rd.getLhs(), buffer);
    }

    @Override
    public final void fire() {
        this.lhs.forEach(rhs);
        // Merge memory changes
        getMemory().getBuffer().takeAllFrom(buffer);
    }

    public final RuntimeRuleImpl setRhs(Consumer<RhsContext> consumer) {
        super.setRhs(consumer);
        return this;
    }

    @Override
    public void onAfterChange() {
        resetState();
    }

    private void resetState() {
        // Merge deltas if available
        for (BetaEndNode endNode : lhs.getAllBetaEndNodes()) {
            endNode.mergeDelta();
        }
    }

    public BetaEndNode[] getAllBetaEndNodes() {
        return lhs.getAllBetaEndNodes();
    }

    public Collection<RuntimeAggregateLhsJoined> getAggregateLhsGroups() {
        return lhs.getAggregateConditionedGroups();
    }
}
