package org.evrete.runtime;

import org.evrete.api.RhsContext;
import org.evrete.api.RuntimeFact;
import org.evrete.api.TypeResolver;
import org.evrete.runtime.memory.Action;
import org.evrete.runtime.memory.BetaEndNode;
import org.evrete.runtime.memory.Buffer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class RuntimeLhs extends AbstractRuntimeLhs implements RhsContext, MemoryChangeListener, ActivationSubject {
    //private final Collection<RuntimeAggregateLhsLoose> aggregateLooseGroups = new ArrayList<>();
    //private final Collection<RuntimeAggregateLhsJoined> aggregateConditionedGroups = new ArrayList<>();
    private final Collection<BetaEndNode> allBetaEndNodes = new ArrayList<>();
    private final Function<String, int[]> name2indices;
    private final Buffer buffer;
    private final TypeResolver typeResolver;
    private final RuntimeRuleImpl rule;

    protected RuntimeLhs(RuntimeRuleImpl rule, LhsDescriptor descriptor, Buffer buffer) {
        super(rule, descriptor);
        this.name2indices = descriptor.getNameIndices();
        this.buffer = buffer;
        this.typeResolver = rule.getMemory().getTypeResolver();
        this.allBetaEndNodes.addAll(getEndNodes());
        this.rule = rule;
    }

    static RuntimeLhs factory(RuntimeRuleImpl rule, LhsDescriptor descriptor, Buffer buffer) {
        Set<AggregateLhsDescriptor> aggregates = descriptor.getAggregateDescriptors();
        if(aggregates.isEmpty()) {
            return new RuntimeLhsDefault(rule, descriptor, buffer);
        } else {
            return new RuntimeLhsAggregate(rule, descriptor, buffer, aggregates);
        }
    }

    public RuntimeRuleImpl getRule() {
        return rule;
    }

    protected void addEndNodes(Collection<BetaEndNode> endNodes) {
        this.allBetaEndNodes.addAll(endNodes);
    }

    public abstract Collection<RuntimeAggregateLhsJoined> getAggregateConditionedGroups();

    public abstract void forEach(Consumer<RhsContext> rhs);

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
    public final RhsContext update(Object obj) {
        buffer.add(typeResolver, Action.UPDATE, Collections.singleton(obj));
        return this;
    }

    @Override
    public final RhsContext delete(Object obj) {
        buffer.add(typeResolver, Action.RETRACT, Collections.singleton(obj));
        return this;
    }

    @Override
    public final RhsContext insert(Object obj) {
        buffer.add(typeResolver, Action.INSERT, Collections.singleton(obj));
        return this;
    }
}
