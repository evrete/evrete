package org.evrete.runtime;

import org.evrete.api.Evaluator;
import org.evrete.api.NamedType;
import org.evrete.runtime.builder.AbstractLhsBuilder;
import org.evrete.runtime.builder.FactTypeBuilder;
import org.evrete.runtime.evaluation.EvaluatorFactory;
import org.evrete.runtime.evaluation.EvaluatorGroup;
import org.evrete.util.CollectionUtils;
import org.evrete.util.MapFunction;
import org.evrete.util.NextIntSupplier;

import java.util.*;

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
    private final int betaFactGroupCount;

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

        this.betaFactGroupCount = keyGroupIndex;
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

    int getBetaFactGroupCount() {
        return betaFactGroupCount;
    }

    MapFunction<String, int[]> getNameIndices() {
        return nameIndices;
    }

    RhsFactGroupDescriptor[] getAllFactGroups() {
        return allFactGroups;
    }

    private static ConditionNodeDescriptor[] findBestAllocation(AbstractLhsBuilder.Compiled lhsBuilder, MapFunction<NamedType, FactType> mapping) {
        // Compiling conditions
        Set<Evaluator> betaConditions = new HashSet<>(lhsBuilder.getBetaConditions());
        if (betaConditions.isEmpty()) return ConditionNodeDescriptor.ZERO_ARRAY;

        final List<EvaluatorGroup> evaluators = EvaluatorFactory.flattenEvaluators(betaConditions, mapping);

        Set<FactType> betaTypes = new HashSet<>();

        // Group conditions by var count
        TreeMap<Integer, List<EvaluatorGroup>> grouped = new TreeMap<>();
        for (EvaluatorGroup e : evaluators) {
            int count = e.descriptor().size();
            grouped.computeIfAbsent(count, k -> new ArrayList<>()).add(e);
            betaTypes.addAll(e.descriptor());
        }

        // Create permutation for each level
        TreeMap<Integer, List<List<EvaluatorGroup>>> groupedPermutations = new TreeMap<>();
        for (Map.Entry<Integer, List<EvaluatorGroup>> entry : grouped.entrySet()) {
            groupedPermutations.put(entry.getKey(), CollectionUtils.permutation(entry.getValue()));
        }

        List<Map<Integer, List<EvaluatorGroup>>> allCombinations = CollectionUtils.combinations(groupedPermutations, TreeMap::new);


        List<List<EvaluatorGroup>> flatPermutations = new LinkedList<>();
        for (Map<Integer, List<EvaluatorGroup>> map : allCombinations) {
            //Flatten the map
            List<EvaluatorGroup> l = new LinkedList<>();
            for (Integer count : map.keySet()) {
                l.addAll(map.get(count));
            }
            flatPermutations.add(l);
        }


        Collection<ConditionNodeDescriptor> best = null;
        double min = Double.MAX_VALUE;

        for (List<EvaluatorGroup> list : flatPermutations) {
            Collection<ConditionNodeDescriptor> finalNodes = ConditionNodeDescriptor.allocateConditions(betaTypes, list);

            double complexity = 0.0;
            for (ConditionNodeDescriptor cnd : finalNodes) {
                complexity += complexity(cnd);
            }

            if (complexity < min) {
                min = complexity;
                best = finalNodes;
            }
        }
        assert best != null;
        return best.toArray(ConditionNodeDescriptor.ZERO_ARRAY);
    }

    //TODO take into account condition complexity
    private static double complexity(ConditionNodeDescriptor node) {
        NodeDescriptor[] sources = node.getSources();
        double[] distances = new double[sources.length];
        double sum = 0.0;
        for (int i = 0; i < sources.length; i++) {
            double d = distanceToEntryNode(sources[i]);
            distances[i] = d;
            sum += d;
        }
        double avg = sum / sources.length;

        double deviation = 0.0;
        for (double distance : distances) {
            deviation += Math.pow(distance - avg, 2);
        }
        return avg * (1.0 + deviation);
    }

    private static double distanceToEntryNode(NodeDescriptor node) {
        double distance = 0.0;
        if (node.isConditionNode()) {
            for (NodeDescriptor source : node.getSources()) {
                if (source.isConditionNode()) {
                    distance += 1.0 + distanceToEntryNode(source);
                } else {
                    distance += 1.0;
                }
            }
            distance = distance / node.getSources().length;
        }
        return distance;
    }


    public int getLevel() {
        return level;
    }

    Set<FactType> getGroupFactTypes() {
        return groupFactTypes;
    }

    @Override
    public String toString() {
        return "{" +
                "factGroups=" + Arrays.toString(allFactGroups) +
                '}';
    }
}


