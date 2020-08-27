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
    private final Buffer ruleBuffer;
    private final Buffer memoryBuffer;
    private boolean inActiveState = false;

    public RuntimeRuleImpl(RuleDescriptor rd, SessionMemory memory) {
        super(rd, memory);
        this.ruleBuffer = new Buffer();
        this.memoryBuffer = memory.getBuffer();
        this.lhs = RuntimeLhs.factory(this, rd.getLhs(), ruleBuffer);
    }

    public final void executeRhs() {
        assert this.inActiveState;
        this.lhs.forEach(rhs);
        // Merge memory changes
        memoryBuffer.takeAllFrom(ruleBuffer);
        resetState();
    }

    boolean readActiveState() {
        //TODO !!!!! do deletes mean an active state??????
        this.inActiveState = isDeleteDeltaAvailable() || isInsertDeltaAvailable();
        return inActiveState;
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
    public void onAfterChange() {
        throw new UnsupportedOperationException();
        //resetState();
    }

    public boolean isInActiveState() {
        return inActiveState;
    }

    public void setInActiveState(boolean inActiveState) {
        this.inActiveState = inActiveState;
    }

    private void resetState() {
        this.inActiveState = false;
        // Merge deltas if available
        for (BetaEndNode endNode : lhs.getAllBetaEndNodes()) {
            endNode.mergeDelta();
        }
    }

    public RuntimeLhs getLhs() {
        return lhs;
    }

    public BetaEndNode[] getAllBetaEndNodes() {
        return lhs.getAllBetaEndNodes();
    }

    public Collection<RuntimeAggregateLhsJoined> getAggregateLhsGroups() {
        return lhs.getAggregateConditionedGroups();
    }
}
