package org.evrete.runtime;

import org.evrete.api.RhsContext;
import org.evrete.api.RuntimeFact;
import org.evrete.api.TypeResolver;
import org.evrete.runtime.memory.Action;
import org.evrete.runtime.memory.BetaEndNode;
import org.evrete.runtime.memory.Buffer;
import org.evrete.runtime.structure.AggregateLhsDescriptor;
import org.evrete.runtime.structure.RhsFactGroupDescriptor;
import org.evrete.runtime.structure.RootLhsDescriptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Function;

public class RuntimeRootLhs extends RuntimeLhs implements RhsContext, MemoryChangeListener {
    private final Collection<RuntimeAggregateLhsLoose> aggregateLooseGroups = new ArrayList<>();
    private final Collection<RuntimeAggregateLhsJoined> aggregateConditionedGroups = new ArrayList<>();
    private final BetaEndNode[] allBetaEndNodes;
    private final Function<String, int[]> name2indices;
    private final Buffer buffer;
    private final TypeResolver typeResolver;

    private RuntimeRootLhs(RuntimeRule rule, RootLhsDescriptor descriptor, Buffer buffer) {
        super(rule, descriptor);
        this.name2indices = descriptor.getNameIndices();
        this.buffer = buffer;
        this.typeResolver = rule.getMemory().getTypeResolver();


        //this.allFactTypes = descriptor.getAllFactTypes();
        Collection<BetaEndNode> allBetas = new ArrayList<>(getEndNodes());
        // Create runtime LHS groups

        for (AggregateLhsDescriptor ad : descriptor.getAggregateDescriptors()) {
            RuntimeAggregateLhs aggregate;
            if (ad.isLoose()) {
                RuntimeAggregateLhsLoose loose = new RuntimeAggregateLhsLoose(rule, this, ad);
                aggregateLooseGroups.add(loose);
                aggregate = loose;
            } else {
                RuntimeAggregateLhsJoined conditioned = new RuntimeAggregateLhsJoined(rule, this, ad);
                //this.aggregateNodes.add(conditioned.getAggregateNode());
                aggregateConditionedGroups.add(conditioned);

                // Set this group as a key predicate
                RhsFactGroupDescriptor[] myGroups = ad.getJoinCondition().getLevelData()[0].getKeyGroupSequence();
                addStateKeyPredicate(myGroups[myGroups.length - 1], conditioned.getAggregateKeyPredicate());

                aggregate = conditioned;
            }
            allBetas.addAll(aggregate.getEndNodes());
        }


        this.allBetaEndNodes = allBetas.toArray(BetaEndNode.ZERO_ARRAY);
    }


    static RuntimeRootLhs factory(RuntimeRule rule, RootLhsDescriptor descriptor, Buffer buffer) {
        return new RuntimeRootLhs(rule, descriptor, buffer);
    }


    public Collection<RuntimeAggregateLhsJoined> getAggregateConditionedGroups() {
        return aggregateConditionedGroups;
    }

    private boolean testLooseGroups() {
        for (RuntimeAggregateLhsLoose group : aggregateLooseGroups) {
            if (!group.getAsBoolean()) {
                return false;
            }
        }
        return true;
    }

    public void forEach(Consumer<RhsContext> rhs) {
        forEach(() -> rhs.accept(this));
    }

    private void forEach(Runnable eachFactRunnable) {
        if (testLooseGroups()) {
            if (hasBetaNodes) {
                forEachKey(
                        () -> forEachFact(eachFactRunnable)
                );
            } else {
                forEachFact(eachFactRunnable);
            }
        }
    }

    public BetaEndNode[] getAllBetaEndNodes() {
        return allBetaEndNodes;
    }

    @Override
    public RuntimeFact getFact(String name) {
        int[] arr = name2indices.apply(name);
        if (arr == null) throw new IllegalArgumentException("Unknown type reference: " + name);
        return factState[arr[0]][arr[1]];
    }

    @Override
    public RhsContext update(Object obj) {
        buffer.add(typeResolver, Action.UPDATE, Collections.singleton(obj));
        return this;
    }

    @Override
    public RhsContext delete(Object obj) {
        buffer.add(typeResolver, Action.RETRACT, Collections.singleton(obj));
        return this;
    }

    @Override
    public RhsContext insert(Object obj) {
        buffer.add(typeResolver, Action.INSERT, Collections.singleton(obj));
        return this;
    }
}
