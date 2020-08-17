package org.evrete.runtime;

import org.evrete.api.Named;
import org.evrete.api.RhsContext;
import org.evrete.runtime.memory.BetaEndNode;
import org.evrete.runtime.memory.Buffer;
import org.evrete.runtime.memory.SessionMemory;
import org.evrete.runtime.structure.RuleDescriptor;

import java.util.Collection;
import java.util.function.Consumer;


//TODO pre/post row listeners
public class RuntimeRule extends RuntimeRuleBase implements Named, MemoryChangeListener {
    private final String ruleName;
    private final RuntimeRootLhs root;
    private Consumer<RhsContext> rhs;
    private final Buffer buffer;

    public RuntimeRule(RuleDescriptor rd, SessionMemory memory) {
        super(rd.getRootLhsDescriptor().getAllFactTypes(), memory);
        this.rhs = rd.getRhs();
        this.ruleName = rd.getName();
        this.buffer = new Buffer();
        this.root = RuntimeRootLhs.factory(this, rd.getRootLhsDescriptor(), buffer);

        for (RuntimeFactType rt : getAllFactTypes()) {
            rt.setRule(this);
        }
    }

    @Override
    public String getName() {
        return ruleName;
    }

    public final void fire() {
        this.root.forEach(rhs);
        // Merge memory changes
        getMemory().getBuffer().takeAllFrom(buffer);
    }

    public final RuntimeRule setRhs(Consumer<RhsContext> consumer) {
        this.rhs = consumer;
        return this;
    }

/*
    public boolean hasChanges() {
        return isDeleteDeltaAvailable() || isInsertDeltaAvailable();
    }
*/

    @Override
    public void onAfterChange() {
        // Merge deltas if available
        resetState();

        // Prepare nodes for RHS call
        for (RuntimeFactType type : getAllFactTypes()) {
            type.onAfterChange();
        }
    }

    //TODO optimize, this method is currently called unconditionally
    private void resetState() {
        this.resetDeltaState();
        for (BetaEndNode endNode : root.getAllBetaEndNodes()) {
            endNode.mergeDelta();
        }
    }

    public BetaEndNode[] getAllBetaEndNodes() {
        return root.getAllBetaEndNodes();
    }

    public Collection<RuntimeAggregateLhsJoined> getAggregateLhsGroups() {
        return root.getAggregateConditionedGroups();
    }

}
