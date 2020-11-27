package org.evrete.runtime;

import org.evrete.api.RhsContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;

public class RuntimeLhsAggregate extends RuntimeLhs implements RhsContext {
    private final Collection<RuntimeAggregateLhsLoose> aggregateLooseGroups = new ArrayList<>();
    private final Collection<RuntimeAggregateLhsJoined> aggregateConditionedGroups = new ArrayList<>();

    RuntimeLhsAggregate(RuntimeRuleImpl rule, LhsDescriptor descriptor, Set<AggregateLhsDescriptor> aggregates) {
        super(rule, descriptor);
        throw new UnsupportedOperationException();

        // Create runtime LHS groups

/*
        for (AggregateLhsDescriptor ad : aggregates) {
            RuntimeAggregateLhs aggregate;
            if (ad.isLoose()) {
                RuntimeAggregateLhsLoose loose = new RuntimeAggregateLhsLoose(rule, this, ad);
                aggregateLooseGroups.add(loose);
                aggregate = loose;
            } else {
                RuntimeAggregateLhsJoined conditioned = new RuntimeAggregateLhsJoined(rule, this, ad);
                aggregate = conditioned;
                //this.aggregateNodes.add(conditioned.getAggregateNode());
                aggregateConditionedGroups.add(conditioned);

                // Set this group as a key predicate
                RhsFactGroupDescriptor[] myGroups = ad.getJoinCondition().getLevelData()[0].getKeyGroupSequence();
                addStateKeyPredicate(myGroups[myGroups.length - 1], conditioned.getAggregateKeyPredicate());
            }
            addEndNodes(aggregate.getEndNodes());
        }
*/
    }


    @Override
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

    @Override
    protected void forEach(Consumer<RhsContext> rhs) {
        forEach(() -> rhs.accept(this));
    }

    private void forEach(NestedFactRunnable eachFactRunnable) {
        throw new UnsupportedOperationException();
/*
        if (testLooseGroups()) {
            if (hasBetaNodes) {
                forEachKey(
                        () -> forEachFact(eachFactRunnable)
                );
            } else {
                forEachFact(eachFactRunnable);
            }
        }
*/
    }

}
