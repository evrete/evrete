package org.evrete.runtime;

import org.evrete.runtime.evaluation.BetaEvaluator;
import org.evrete.util.MapFunction;

import java.util.Collection;

class KnowledgeLhs extends ActiveLhs<KnowledgeFactGroup> {
    private final MapFunction<String, FactPosition> factPositionMapping;


    public KnowledgeLhs(KnowledgeFactGroup[] factGroups) {
        super(factGroups);

        // Create fact mapping. This mapping will be used as-is by session rule instances.
        MapFunction<String, FactPosition> mapFunction = new MapFunction<>();
        KnowledgeFactGroup[] groups = getFactGroups();

        for (int groupIndex = 0; groupIndex < groups.length; groupIndex++) {
            KnowledgeFactGroup group = groups[groupIndex];
            FactType[] factTypes = group.getEntryNodes();
            for (int inGroupIndex = 0; inGroupIndex < factTypes.length; inGroupIndex++) {
                FactType factType = factTypes[inGroupIndex];
                String varName = factType.getVarName();
                FactPosition factPosition = new FactPosition(groupIndex, inGroupIndex, factType.getInRuleIndex());
                mapFunction.putNew(varName, factPosition);
            }
        }
        this.factPositionMapping = mapFunction;
    }

    MapFunction<String, FactPosition> getFactPositionMapping() {
        return factPositionMapping;
    }

    public static KnowledgeLhs factory(Collection<FactType> factTypes, RuleBuilderActiveConditions lhsConditions) {
        // 1. Flatten (combine) and sort beta conditions
        Collection<BetaEvaluator> flattenedBetaConditions = lhsConditions.flattenBetaConditions(factTypes);

        KnowledgeFactGroup[] factGroups =  KnowledgeFactGroupBuilder.build(factTypes, flattenedBetaConditions);
        return new KnowledgeLhs(factGroups);
    }



    /**
     * {@link org.evrete.api.RhsContext} methods allow to reference facts by their declared name. Internally,
     * the engine operates on groups of facts. This utility class provides a local "address" of a fact.
     */
    static class FactPosition {
        final int groupIndex;
        final int inGroupIndex;
        final int inRuleIndex;

        FactPosition(int groupIndex, int inGroupIndex, int inRuleIndex) {
            this.groupIndex = groupIndex;
            this.inGroupIndex = inGroupIndex;
            this.inRuleIndex = inRuleIndex;
        }
    }

}


