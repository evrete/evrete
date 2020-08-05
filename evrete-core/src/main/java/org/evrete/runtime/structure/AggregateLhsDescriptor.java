package org.evrete.runtime.structure;

import org.evrete.api.Evaluator;
import org.evrete.api.NamedType;
import org.evrete.runtime.AbstractRuntime;
import org.evrete.runtime.aggregate.AggregateEvaluatorFactory;
import org.evrete.runtime.builder.AggregateLhsBuilder;
import org.evrete.runtime.builder.LhsBuilder;
import org.evrete.util.MapFunction;
import org.evrete.util.NextIntSupplier;

import java.util.Set;
import java.util.function.Function;

public class AggregateLhsDescriptor extends LhsDescriptor {
    private final AggregateEvaluatorFactory aggregateEvaluatorFactory;
    private final AggregateEvaluator joinCondition;


    public AggregateLhsDescriptor(AbstractRuntime<?, ?> runtime, RootLhsDescriptor parent, AggregateLhsBuilder<?> group, NextIntSupplier factIdGenerator, MapFunction<NamedType, FactType> typeMapping) {
        super(runtime, parent, group, factIdGenerator, typeMapping);
        this.aggregateEvaluatorFactory = group.getAggregateEvaluatorFactory();

        LhsBuilder.Compiled compiled = group.getCompiledData();
        Set<Evaluator> conditions = compiled.getAggregateConditions();
        if (conditions.isEmpty()) {
            //this.aggregateConditionNode = null;
            this.joinCondition = null;
        } else {
            Function<NamedType, FactType> unionMapping = MapFunction.union(typeMapping, parent.getRootMapping());
            this.joinCondition = new AggregateEvaluator(EvaluatorFactory.unionEvaluators(runtime, conditions, unionMapping));
        }
    }

    public AggregateEvaluatorFactory getAggregateEvaluatorFactory() {
        return aggregateEvaluatorFactory;
    }


    public boolean isLoose() {
        return this.joinCondition == null;
    }

    public AggregateEvaluator getJoinCondition() {
        if (joinCondition == null) {
            throw new UnsupportedOperationException();
        } else {
            return joinCondition;
        }
    }
}


