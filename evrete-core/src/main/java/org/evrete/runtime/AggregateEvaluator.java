package org.evrete.runtime;

import org.evrete.runtime.evaluation.BetaEvaluatorGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AggregateEvaluator extends BetaEvaluatorGroup {
    private final LevelData[] levelData;
    private final FactType[][] grouping;

    public AggregateEvaluator(BetaEvaluatorGroup delegate) {
        super(delegate);

        int maxLevel = Integer.MIN_VALUE;
        for (FactType t : delegate.descriptor()) {
            int level = t.getFactGroup().getLhsDescriptor().getLevel();
            maxLevel = Math.max(maxLevel, level);
        }

        // Fill grouping data
        RhsFactGroupDescriptor[] lastGroupDescriptors = new RhsFactGroupDescriptor[maxLevel + 1];
        for (FactType t : delegate.descriptor()) {
            int level = t.getFactGroup().getLhsDescriptor().getLevel();

            RhsFactGroupDescriptor groupDescriptor = t.getFactGroup();
            RhsFactGroupDescriptor existing;
            if ((existing = lastGroupDescriptors[level]) == null) {
                lastGroupDescriptors[level] = groupDescriptor;
            } else {
                if (existing.getKeyGroupIndex() < groupDescriptor.getKeyGroupIndex()) {
                    lastGroupDescriptors[level] = groupDescriptor;
                }
            }
        }

        this.levelData = new LevelData[lastGroupDescriptors.length];
        for (int i = 0; i < lastGroupDescriptors.length; i++) {
            this.levelData[i] = new LevelData(lastGroupDescriptors[i]);
        }

        this.grouping = new FactType[levelData.length][];
        for (int i = 0; i < levelData.length; i++) {
            this.grouping[i] = levelData[i].factTypeSequence;
        }
    }

    public LevelData[] getLevelData() {
        return levelData;
    }

    public FactType[][] getGrouping() {
        return grouping;
    }

    public static class LevelData {
        private final RhsFactGroupDescriptor[] keyGroupSequence;
        private final FactType[] factTypeSequence;

        private LevelData(RhsFactGroupDescriptor lastDescriptor) {
            List<RhsFactGroupDescriptor> keySeq = new ArrayList<>();
            List<FactType> typeSeq = new ArrayList<>();

            AbstractLhsDescriptor lhs = lastDescriptor.getLhsDescriptor();
            RhsFactGroupDescriptor[] allGroups = lhs.getAllFactGroups();
            for (RhsFactGroupDescriptor g : allGroups) {
                if (!g.isLooseGroup()) {
                    keySeq.add(g);
                    typeSeq.addAll(Arrays.asList(g.getTypes()));
                    if (g == lastDescriptor) {
                        break;
                    }
                }
            }


            this.keyGroupSequence = keySeq.toArray(RhsFactGroupDescriptor.ZERO_ARRAY);
            this.factTypeSequence = typeSeq.toArray(FactType.ZERO_ARRAY);
        }

        public RhsFactGroupDescriptor[] getKeyGroupSequence() {
            return keyGroupSequence;
        }

        public FactType[] getFactTypeSequence() {
            return factTypeSequence;
        }
    }
}
