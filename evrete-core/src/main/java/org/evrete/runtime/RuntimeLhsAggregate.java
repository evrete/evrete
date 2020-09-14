package org.evrete.runtime;

import org.evrete.api.RhsContext;
import org.evrete.runtime.memory.Buffer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;

public class RuntimeLhsAggregate extends RuntimeLhs implements RhsContext {
    private final Collection<RuntimeAggregateLhsLoose> aggregateLooseGroups = new ArrayList<>();
    private final Collection<RuntimeAggregateLhsJoined> aggregateConditionedGroups = new ArrayList<>();

    RuntimeLhsAggregate(RuntimeRuleImpl rule, LhsDescriptor descriptor, Buffer buffer, Set<AggregateLhsDescriptor> aggregates) {
        super(rule, descriptor, buffer);
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

    @Override
    public boolean isInActiveState() {
        for (AbstractRuntimeLhs g : aggregateLooseGroups) {
            g.isInActiveState();
        }
        for (AbstractRuntimeLhs g : aggregateConditionedGroups) {
            g.isInActiveState();
        }
        return super.isInActiveState();
    }

    @Override
    public void resetState() {
        for (AbstractRuntimeLhs g : aggregateLooseGroups) {
            g.resetState();
        }
        for (AbstractRuntimeLhs g : aggregateConditionedGroups) {
            g.resetState();
        }
        super.resetState();
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
    public void forEach(Consumer<RhsContext> rhs) {
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
