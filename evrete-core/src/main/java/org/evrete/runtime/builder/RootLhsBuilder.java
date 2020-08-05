package org.evrete.runtime.builder;

import org.evrete.api.RhsContext;
import org.evrete.api.RuntimeContext;

import java.util.Collection;
import java.util.HashSet;
import java.util.function.Consumer;

public class RootLhsBuilder<C extends RuntimeContext<C, ?>> extends LhsBuilder<C, RootLhsBuilder<C>> {
    private final Collection<AggregateLhsBuilder<C>> aggregateGroups = new HashSet<>();

    RootLhsBuilder(RuleBuilderImpl<C> ruleBuilder) {
        super(ruleBuilder);
    }

    void saveAggregate(AggregateLhsBuilder<C> group) {
        aggregateGroups.add(group);
    }

    public Collection<AggregateLhsBuilder<C>> getAggregateGroups() {
        return aggregateGroups;
    }

    @Override
    protected RootLhsBuilder<C> self() {
        return this;
    }

    public C execute(Consumer<RhsContext> consumer) {
        return getRuleBuilder().execute(consumer);
    }

    public C execute() {
        return getRuleBuilder().execute(null);
    }

    public AggregateLhsBuilder<C> having() {
        return new AggregateLhsBuilder<>(this);
    }

}
