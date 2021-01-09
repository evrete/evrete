package org.evrete.runtime.builder;

import org.evrete.api.RhsContext;
import org.evrete.api.RuleBuilder;
import org.evrete.api.RuntimeContext;

import java.util.function.Consumer;

public class LhsBuilder<C extends RuntimeContext<C>> extends AbstractLhsBuilder<C, LhsBuilder<C>> {
    //private final Collection<AggregateLhsBuilder<C>> aggregateGroups = new HashSet<>();

    LhsBuilder(RuleBuilderImpl<C> ruleBuilder) {
        super(ruleBuilder);
    }

/*
    void saveAggregate(AggregateLhsBuilder<C> group) {
        aggregateGroups.add(group);
    }

    public Collection<AggregateLhsBuilder<C>> getAggregateGroups() {
        return aggregateGroups;
    }
*/

    @Override
    protected LhsBuilder<C> self() {
        return this;
    }

    public C execute(Consumer<RhsContext> consumer) {
        return getRuleBuilder().build(consumer);
    }

    public C execute(String literalRhs) {
        return getRuleBuilder().build(literalRhs);
    }

    public RuleBuilder<C> setRhs(Consumer<RhsContext> consumer) {
        RuleBuilderImpl<C> builder = getRuleBuilder();
        builder.setRhs(consumer);
        return builder;
    }

    public RuleBuilder<C> setRhs(String literalConsumer) {
        RuleBuilderImpl<C> builder = getRuleBuilder();
        builder.setRhs(literalConsumer);
        return builder;
    }

    public RuleBuilder<C> create() {
        return getRuleBuilder();
    }


    public C execute() {
        return getRuleBuilder().build();
    }

/*
    public AggregateLhsBuilder<C> having(FactBuilder... facts) {
        return new AggregateLhsBuilder<>(this, facts);
    }
*/

}
