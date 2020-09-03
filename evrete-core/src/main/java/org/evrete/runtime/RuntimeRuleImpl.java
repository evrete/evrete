package org.evrete.runtime;

import org.evrete.api.RhsContext;
import org.evrete.api.RuntimeRule;
import org.evrete.runtime.memory.BetaEndNode;
import org.evrete.runtime.memory.Buffer;
import org.evrete.runtime.memory.SessionMemory;

import java.util.function.Consumer;


public class RuntimeRuleImpl extends AbstractRuntimeRule implements RuntimeRule, ActivationSubject {
    private final RuntimeLhs lhs;
    private final Buffer ruleBuffer;
    private final Buffer memoryBuffer;

    public RuntimeRuleImpl(RuleDescriptor rd, SessionMemory memory) {
        super(rd, memory);
        this.ruleBuffer = new Buffer();
        this.memoryBuffer = memory.getBuffer();
        this.lhs = RuntimeLhs.factory(this, rd.getLhs(), ruleBuffer);
    }

    public final void executeRhs() {
        assert isInActiveState();
        this.lhs.forEach(rhs);
        memoryBuffer.takeAllFrom(ruleBuffer);
    }

    public final RuntimeRuleImpl setRhs(Consumer<RhsContext> consumer) {
        super.setRhs(consumer);
        return this;
    }

    public final RuntimeRuleImpl chainRhs(Consumer<RhsContext> consumer) {
        super.chainRhs(consumer);
        return this;
    }

    @Override
    public boolean isInActiveState() {
        return lhs.isInActiveState();
    }

    @Override
    public void resetState() {
        lhs.resetState();
        // Merge deltas if available
        for (BetaEndNode endNode : lhs.getAllBetaEndNodes()) {
            endNode.mergeDelta();
        }
    }

    @Override
    public RuntimeLhs getLhs() {
        return lhs;
    }

    @Override
    public String toString() {
        return "RuntimeRule{" +
                "name=" + getName() +
                '}';
    }

    /*
    public Collection<BetaEndNode> getAllBetaEndNodes() {
        return lhs.getAllBetaEndNodes();
    }

    public Collection<RuntimeAggregateLhsJoined> getAggregateLhsGroups() {
        return lhs.getAggregateConditionedGroups();
    }
*/
}
