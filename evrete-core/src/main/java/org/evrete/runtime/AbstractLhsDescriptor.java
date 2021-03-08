package org.evrete.runtime;

import org.evrete.api.NamedType;
import org.evrete.runtime.builder.AbstractLhsBuilder;
import org.evrete.runtime.builder.FactTypeBuilder;
import org.evrete.runtime.evaluation.BetaEvaluator;
import org.evrete.runtime.evaluation.EvaluatorFactory;
import org.evrete.runtime.evaluation.EvaluatorWrapper;
import org.evrete.util.MapFunction;
import org.evrete.util.NextIntSupplier;

import java.util.*;
import java.util.function.ToDoubleFunction;

/**
 * Descriptor for LHS of a rule. Each LHS consists of beta-graphs
 * (fact types and conditions they participate in) and alpha fact types,
 * not involved in any join conditions.
 */
abstract class AbstractLhsDescriptor {
    private final FactType[] factTypes;
    private final RhsFactGroupDescriptor[] allFactGroups;

    AbstractLhsDescriptor(AbstractRuntime<?> runtime, AbstractLhsBuilder<?, ?> group, NextIntSupplier factIdGenerator, MapFunction<NamedType, FactType> typeMapping) {
        Set<FactTypeBuilder> declaredTypes = group.getDeclaredFactTypes();
        AbstractLhsBuilder.Compiled compiledConditions = group.getCompiledData();

        Set<FactType> keyedFactTypes = new HashSet<>();
        Collection<FactType> plainFactTypes = new ArrayList<>();
        List<FactType> allFactTypes = new LinkedList<>();
        for (FactTypeBuilder builder : declaredTypes) {
            // Building FactType
            FactType factType = runtime.buildFactType(
                    builder,
                    compiledConditions.getAlphaConditions(builder),
                    factIdGenerator.next()
            );
            typeMapping.putNew(builder, factType);


            if (factType.getFields().size() == 0) {
                plainFactTypes.add(factType);
            } else {
                keyedFactTypes.add(factType);
            }
            allFactTypes.add(factType);
        }

        this.factTypes = allFactTypes.toArray(FactType.ZERO_ARRAY);

        ConditionNodeDescriptor[] finalNodes = findBestAllocation(compiledConditions, typeMapping);

        List<RhsFactGroupDescriptor> allFactGroups = new ArrayList<>();

        for (ConditionNodeDescriptor finalNode : finalNodes) {
            RhsFactGroupDescriptor descriptor = new RhsFactGroupDescriptor(finalNode);
            allFactGroups.add(descriptor);
            keyedFactTypes.removeAll(Arrays.asList(descriptor.getTypes()));
        }

        assert keyedFactTypes.isEmpty();

        if (!plainFactTypes.isEmpty()) {
            allFactGroups.add(new RhsFactGroupDescriptor(plainFactTypes));
        }

        this.allFactGroups = allFactGroups.toArray(RhsFactGroupDescriptor.ZERO_ARRAY);
    }

    private static ConditionNodeDescriptor[] findBestAllocation(AbstractLhsBuilder.Compiled lhsBuilder, MapFunction<NamedType, FactType> mapping) {
        // Compiling conditions
        Set<EvaluatorWrapper> betaConditions = new HashSet<>(lhsBuilder.getBetaConditions());
        if (betaConditions.isEmpty()) return ConditionNodeDescriptor.ZERO_ARRAY;

        final List<BetaEvaluator> evaluators = new ArrayList<>(EvaluatorFactory.flattenEvaluators(betaConditions, mapping));
        if (evaluators.isEmpty()) throw new IllegalStateException();

        double maxComplexity = Double.MIN_VALUE;
        double minComplexity = Double.MAX_VALUE;
        Set<FactType> betaTypes = new HashSet<>();

        for (BetaEvaluator g : evaluators) {
            double complexity = g.getComplexity();
            if (complexity <= 0.0) throw new IllegalStateException("Complexity must be a positive value");

            if (complexity > maxComplexity) {
                maxComplexity = complexity;
            }

            if (complexity < minComplexity) {
                minComplexity = complexity;
            }

            betaTypes.addAll(g.factTypes());

        }

        // MinMax complexities (times
        Map<BetaEvaluator, Double> minMaxComplexities = new HashMap<>();
        for (BetaEvaluator g : evaluators) {
            double newComplexity = 1.0 + (g.getComplexity() - minComplexity) / (maxComplexity - minComplexity);
            minMaxComplexities.put(g, newComplexity * g.getTotalTypesInvolved());
        }

        // Sorting
        // Same complexity
        evaluators.sort(Comparator.comparingDouble((ToDoubleFunction<BetaEvaluator>) minMaxComplexities::get).thenComparing(BetaEvaluator::toString));

        Collection<ConditionNodeDescriptor> finalNodes = ConditionNodeDescriptor.allocateConditions(betaTypes, evaluators);
        return finalNodes.toArray(ConditionNodeDescriptor.ZERO_ARRAY);


    }

    RhsFactGroupDescriptor[] getAllFactGroups() {
        return allFactGroups;
    }

    public FactType[] getFactTypes() {
        return factTypes;
    }

    @Override
    public String toString() {
        return "{" +
                "factGroups=" + Arrays.toString(allFactGroups) +
                '}';
    }
}


