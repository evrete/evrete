package org.evrete.runtime;

import org.evrete.api.NamedType;
import org.evrete.runtime.builder.AbstractLhsBuilder;
import org.evrete.runtime.builder.FactTypeBuilder;
import org.evrete.runtime.evaluation.BetaEvaluatorGroup;
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
public abstract class AbstractLhsDescriptor {
    private final MapFunction<String, int[]> nameIndices = new MapFunction<>();
    private final int level;
    private final Set<FactType> groupFactTypes = new HashSet<>();
    private final RhsFactGroupDescriptor[] allFactGroups;

    AbstractLhsDescriptor(AbstractRuntime<?> runtime, AbstractLhsDescriptor parent, AbstractLhsBuilder<?, ?> group, NextIntSupplier factIdGenerator, MapFunction<NamedType, FactType> typeMapping) {
        this.level = parent == null ? 0 : parent.level + 1;

        Set<FactTypeBuilder> declaredTypes = group.getDeclaredFactTypes();
        AbstractLhsBuilder.Compiled compiledConditions = group.getCompiledData();

        Set<FactType> keyedFactTypes = new HashSet<>();
        Collection<FactType> plainFactTypes = new ArrayList<>();
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

            // Scan existing types and identify same fields and same alpha address
            for (FactType existing : groupFactTypes) {
                boolean sameKeys = existing.getFields().equals(factType.getFields());
                boolean sameAlpha = existing.getBucketIndex() == factType.getBucketIndex();
                if (sameKeys && sameAlpha) {
                    existing.markNonUniqueKeyAndAlpha();
                    factType.markNonUniqueKeyAndAlpha();
                }
            }

            groupFactTypes.add(factType);
        }

        ConditionNodeDescriptor[] finalNodes = findBestAllocation(compiledConditions, typeMapping);

        //this.rhsKeyedFactGroups = new ArrayList<>();
        List<RhsFactGroupDescriptor> allFactGroups = new ArrayList<>();
        int factGroupCounter = 0;
        int keyGroupIndex = 0;

        for (ConditionNodeDescriptor finalNode : finalNodes) {
            RhsFactGroupDescriptor descriptor = new RhsFactGroupDescriptor(this, factGroupCounter, keyGroupIndex, finalNode);
            allFactGroups.add(descriptor);
            factGroupCounter++;
            keyGroupIndex++;
            keyedFactTypes.removeAll(Arrays.asList(descriptor.getTypes()));
        }

        for (FactType keyedType : keyedFactTypes) {
            RhsFactGroupDescriptor descriptor = new RhsFactGroupDescriptor(this, factGroupCounter, keyGroupIndex, keyedType);
            allFactGroups.add(descriptor);
            factGroupCounter++;
            keyGroupIndex++;
        }

        if (!plainFactTypes.isEmpty()) {
            allFactGroups.add(new RhsFactGroupDescriptor(this, factGroupCounter, plainFactTypes));
        }

        for (RhsFactGroupDescriptor descriptor : allFactGroups) {
            FactType[] types = descriptor.getTypes();
            int factGroupIndex = descriptor.getFactGroupIndex();
            for (int i = 0; i < types.length; i++) {
                nameIndices.putNew(types[i].getVar(), new int[]{factGroupIndex, i});
            }
        }

        this.allFactGroups = allFactGroups.toArray(RhsFactGroupDescriptor.ZERO_ARRAY);
    }

    private static ConditionNodeDescriptor[] findBestAllocation(AbstractLhsBuilder.Compiled lhsBuilder, MapFunction<NamedType, FactType> mapping) {
        // Compiling conditions
        Set<EvaluatorWrapper> betaConditions = new HashSet<>(lhsBuilder.getBetaConditions());
        if (betaConditions.isEmpty()) return ConditionNodeDescriptor.ZERO_ARRAY;

        final List<BetaEvaluatorGroup> evaluators = new ArrayList<>(EvaluatorFactory.flattenEvaluators(betaConditions, mapping));
        if (evaluators.isEmpty()) throw new IllegalStateException();

        double maxComplexity = Double.MIN_VALUE;
        double minComplexity = Double.MAX_VALUE;
        Set<FactType> betaTypes = new HashSet<>();

        for (BetaEvaluatorGroup g : evaluators) {
            double complexity = g.getComplexity();
            if (complexity <= 0.0) throw new IllegalStateException("Complexity must be a positive value");

            if (complexity > maxComplexity) {
                maxComplexity = complexity;
            }

            if (complexity < minComplexity) {
                minComplexity = complexity;
            }

            betaTypes.addAll(g.descriptor());

        }

        // MinMax complexities
        Map<BetaEvaluatorGroup, Double> minMaxComplexities = new HashMap<>();
        for (BetaEvaluatorGroup g : evaluators) {
            double newComplexity = 1.0 + (g.getComplexity() - minComplexity) / (maxComplexity - minComplexity);
            minMaxComplexities.put(g, newComplexity * g.getEvaluators().length);
        }

        // Sorting
        // Same complexity
        evaluators.sort(Comparator.comparingDouble((ToDoubleFunction<BetaEvaluatorGroup>) minMaxComplexities::get).thenComparing(BetaEvaluatorGroup::toString));

        Collection<ConditionNodeDescriptor> finalNodes = ConditionNodeDescriptor.allocateConditions(betaTypes, evaluators);
        return finalNodes.toArray(ConditionNodeDescriptor.ZERO_ARRAY);


    }

    MapFunction<String, int[]> getNameIndices() {
        return nameIndices;
    }

    RhsFactGroupDescriptor[] getAllFactGroups() {
        return allFactGroups;
    }

    public int getLevel() {
        return level;
    }

    public Set<FactType> getGroupFactTypes() {
        return groupFactTypes;
    }

    @Override
    public String toString() {
        return "{" +
                "factGroups=" + Arrays.toString(allFactGroups) +
                '}';
    }
}


