package org.evrete.runtime;

import org.evrete.api.Action;
import org.evrete.api.RhsContext;
import org.evrete.api.RuntimeFact;
import org.evrete.runtime.memory.ActionQueue;
import org.evrete.runtime.memory.BetaEndNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class RuntimeLhs extends AbstractRuntimeLhs implements RhsContext {
    //private final Collection<RuntimeAggregateLhsLoose> aggregateLooseGroups = new ArrayList<>();
    //private final Collection<RuntimeAggregateLhsJoined> aggregateConditionedGroups = new ArrayList<>();
    private final Collection<BetaEndNode> allBetaEndNodes = new ArrayList<>();
    private final Function<String, int[]> name2indices;
    private ActionQueue<Object> buffer;
    private final RuntimeRuleImpl rule;

    protected RuntimeLhs(RuntimeRuleImpl rule, LhsDescriptor descriptor) {
        super(rule, descriptor);
        this.name2indices = descriptor.getNameIndices();
        this.allBetaEndNodes.addAll(getEndNodes());
        this.rule = rule;
    }

    static RuntimeLhs factory(RuntimeRuleImpl rule, LhsDescriptor descriptor) {
        Set<AggregateLhsDescriptor> aggregates = descriptor.getAggregateDescriptors();
        if (aggregates.isEmpty()) {
            return new RuntimeLhsDefault(rule, descriptor);
        } else {
            return new RuntimeLhsAggregate(rule, descriptor, aggregates);
        }
    }

    @Override
    public RuntimeRuleImpl getRule() {
        return rule;
    }

/*
    protected void addEndNodes(Collection<BetaEndNode> endNodes) {
        this.allBetaEndNodes.addAll(endNodes);
    }
*/

    public abstract Collection<RuntimeAggregateLhsJoined> getAggregateConditionedGroups();

    abstract void forEach(Consumer<RhsContext> rhs);

    public void setBuffer(ActionQueue<Object> buffer) {
        this.buffer = buffer;
    }

    public final Collection<BetaEndNode> getAllBetaEndNodes() {
        return allBetaEndNodes;
    }

    @Override
    public final RuntimeFact getFact(String name) {
        int[] arr = name2indices.apply(name);
        if (arr == null) throw new IllegalArgumentException("Unknown type reference: " + name);
        return factState[arr[0]][arr[1]];
    }

    @Override
    //TODO check if field values have _really_ changed
    public final RhsContext update(Object obj) {
        buffer.add(Action.UPDATE, obj);
        return this;
    }

    @Override
    public final RhsContext delete(Object obj) {
        buffer.add(Action.RETRACT, obj);
        return this;
    }

    @Override
    public final RhsContext insert(Object obj) {
        buffer.add(Action.INSERT, obj);
        return this;
    }
}
