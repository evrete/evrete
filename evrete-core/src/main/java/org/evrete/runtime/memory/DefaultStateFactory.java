package org.evrete.runtime.memory;

import org.evrete.runtime.evaluation.BetaEvaluatorGroup;

public class DefaultStateFactory implements NodeIterationStateFactory<NodeIterationState, BetaEvaluatorGroup> {

    public DefaultStateFactory() {
/*
        this.node = node;

        int[] nonPlainSourceIndices = node.getNonPlainSourceIndices();
        this.nonPlainSources = nonPlainSourceIndices.length > 0;

        // Destination data
        BetaMemoryNode<?>[] sources = node.getSources();
        FactType[][] primaryFactTypes = new FactType[sources.length][];
        for (BetaMemoryNode<?> source : sources) {
            primaryFactTypes[source.getSourceIndex()] = source.getGrouping()[0];
        }


        FactType[][] secondaryFactTypes = new FactType[nonPlainSourceIndices.length][];
        for (int z = 0; z < nonPlainSourceIndices.length; z++) {
            int nonPlainSourceId = nonPlainSourceIndices[z];
            BetaMemoryNode<?> source = sources[nonPlainSourceId];
            secondaryFactTypes[z] = source.getGrouping()[1];
        }


        FactType[][] grouping = node.getGrouping();
        int totalLevels = grouping.length;
        this.destinationData = new int[totalLevels][][];

        for (int level = 0; level < totalLevels; level++) {
            FactType[] levelTypes = grouping[level];
            int[][] locations = new int[levelTypes.length][];

            for (int typeArrIndex = 0; typeArrIndex < levelTypes.length; typeArrIndex++) {
                FactType t = levelTypes[typeArrIndex];

                int[] addr;
                int[] loc = CollectionUtils.locate2(t, primaryFactTypes, FactType.EQUALITY_BY_INDEX);
                if (loc != null) {
                    // Type belongs to primary level
                    addr = new int[]{0, loc[0], loc[1]};
                } else {
                    loc = CollectionUtils.locate2(t, secondaryFactTypes, FactType.EQUALITY_BY_INDEX);
                    if (loc == null) {
                        //This can happen if one of the sources has grouping size deeper than 2
                        throw new IllegalStateException();
                    }
                    addr = new int[]{1, loc[0], loc[1]};
                }
                locations[typeArrIndex] = addr;
            }

            destinationData[level] = locations;
        }
*/
    }

    @Override
    public NodeIterationState newIterationState(BetaConditionNode node) {
        return new NodeIterationState(node);
    }
}
