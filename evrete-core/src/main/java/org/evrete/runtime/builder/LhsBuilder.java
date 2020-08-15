package org.evrete.runtime.builder;

import org.evrete.api.RhsContext;
import org.evrete.api.RuntimeContext;

import java.util.Collection;
import java.util.HashSet;
import java.util.function.Consumer;

public class LhsBuilder<C extends RuntimeContext<C>> extends AbstractLhsBuilder<C, LhsBuilder<C>> {
    private final Collection<AggregateLhsBuilder<C>> aggregateGroups = new HashSet<>();

    LhsBuilder(RuleBuilderImpl<C> ruleBuilder) {
        super(ruleBuilder);
    }

    void saveAggregate(AggregateLhsBuilder<C> group) {
        aggregateGroups.add(group);
    }

    public Collection<AggregateLhsBuilder<C>> getAggregateGroups() {
        return aggregateGroups;
    }

    @Override
    protected LhsBuilder<C> self() {
        return this;
    }

    public C execute(Consumer<RhsContext> consumer) {
        return getRuleBuilder().build(consumer);

    }

    public C execute() {
        return getRuleBuilder().build(null);
    }

    public AggregateLhsBuilder<C> having() {
        return new AggregateLhsBuilder<>(this);
    }

}
