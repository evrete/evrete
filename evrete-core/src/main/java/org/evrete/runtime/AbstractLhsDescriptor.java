package org.evrete.runtime;

import org.evrete.api.Evaluator;
import org.evrete.api.NamedType;
import org.evrete.runtime.builder.AbstractLhsBuilder;
import org.evrete.runtime.builder.FactTypeBuilder;
import org.evrete.runtime.evaluation.EvaluatorFactory;
import org.evrete.runtime.evaluation.EvaluatorGroup;
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
        Collection<FactType> alphaFactTypes = new ArrayList<>();
        for (FactTypeBuilder builder : declaredTypes) {
            Set<Evaluator> alphaConditions = compiledConditions.getAlphaConditions(builder);
            FactType factType = FactType.factory(runtime, builder, alphaConditions, factIdGenerator);
            if (factType.getFields().size() == 0) {
                alphaFactTypes.add(factType);
            } else {
                keyedFactTypes.add(factType);
            }
            typeMapping.putNew(builder, factType);

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

        if (!alphaFactTypes.isEmpty()) {
            allFactGroups.add(new RhsFactGroupDescriptor(this, factGroupCounter, alphaFactTypes));
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
        Set<Evaluator> betaConditions = new HashSet<>(lhsBuilder.getBetaConditions());
        if (betaConditions.isEmpty()) return ConditionNodeDescriptor.ZERO_ARRAY;

        final List<EvaluatorGroup> evaluators = new ArrayList<>(EvaluatorFactory.flattenEvaluators(betaConditions, mapping));
        if (evaluators.isEmpty()) throw new IllegalStateException();

        double maxComplexity = Double.MIN_VALUE;
        double minComplexity = Double.MAX_VALUE;
        Set<FactType> betaTypes = new HashSet<>();

        for (EvaluatorGroup g : evaluators) {
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
        Map<EvaluatorGroup, Double> minMaxComplexities = new HashMap<>();
        for (EvaluatorGroup g : evaluators) {
            double newComplexity = 1.0 + (g.getComplexity() - minComplexity) / (maxComplexity - minComplexity);
            minMaxComplexities.put(g, newComplexity * g.getEvaluators().length);
        }

        // Sorting
        // Same complexity
        evaluators.sort(Comparator.comparingDouble((ToDoubleFunction<EvaluatorGroup>) minMaxComplexities::get).thenComparing(EvaluatorGroup::toString));

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


