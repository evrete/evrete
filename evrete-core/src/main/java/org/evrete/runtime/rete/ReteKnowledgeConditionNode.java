package org.evrete.runtime.rete;


import org.evrete.api.IntToValue;
import org.evrete.api.LhsField;
import org.evrete.runtime.ActiveField;
import org.evrete.runtime.FactType;
import org.evrete.runtime.GroupedFactType;
import org.evrete.runtime.LhsConditionDH;
import org.evrete.runtime.evaluation.BetaEvaluator;

public class ReteKnowledgeConditionNode extends ReteKnowledgeNode {
    final Evaluator evaluator;

    public ReteKnowledgeConditionNode(BetaEvaluator evaluator, ReteKnowledgeNode[] sourceNodes, GroupedFactType[] allGroupFactTypes) {
        super(sourceNodes);
        this.evaluator = new Evaluator(evaluator, sourceNodes, allGroupFactTypes);
    }

    public Evaluator getEvaluator() {
        return evaluator;
    }

    /**
     * The context-aware version of the {@link BetaEvaluator}. For each component of the parent class,
     * this class contains information on how to obtain values for their
     * {@link org.evrete.api.Evaluator#test(IntToValue)} method arguments.
     */
    public static class Evaluator {
        public final Coordinate[][] coordinates;
        private LhsConditionDH<FactType, ActiveField>[] components;

        public Evaluator(BetaEvaluator parent, ReteKnowledgeNode[] sourceNodes, GroupedFactType[] allGroupFactTypes) {
            this.components = parent.getComponents();
            this.coordinates = new Coordinate[components.length][];

            for (int idx = 0; idx < components.length; idx++) {
                LhsConditionDH<FactType, ActiveField> condition = components[idx];
                this.coordinates[idx] = Coordinate.factory(condition, sourceNodes, allGroupFactTypes);
            }
        }

        public LhsConditionDH<FactType, ActiveField>[] getComponents() {
            return components;
        }

        public static class Coordinate {
            public final int sourceIdx;
            public final int inGroupIdx;
            public final int fieldIdx;

            private Coordinate(int sourceIdx, int inGroupIdx, int fieldIdx) {
                if(fieldIdx < 0 || inGroupIdx < 0 || sourceIdx < 0) {
                    throw new IllegalStateException("Coordinates must have non-negative coordinates: " + sourceIdx + ", " + inGroupIdx + ", " + fieldIdx);
                } else {
                    this.sourceIdx = sourceIdx;
                    this.inGroupIdx = inGroupIdx;
                    this.fieldIdx = fieldIdx;
                }
            }

            static Coordinate[] factory(LhsConditionDH<FactType, ActiveField> condition, ReteKnowledgeNode[] sourceNodes, GroupedFactType[] allGroupFactTypes) {
                LhsField.Array<FactType, ActiveField> descriptor = condition.getDescriptor();
                Coordinate[] coordinates = new Coordinate[descriptor.length()];
                for (int i = 0; i < coordinates.length; i++) {
                    LhsField<FactType, ActiveField> lhsField = descriptor.get(i);
                    coordinates[i] = instance(lhsField, sourceNodes, allGroupFactTypes);
                }
                return coordinates;
            }

            static Coordinate instance(LhsField<FactType, ActiveField> lhsField, ReteKnowledgeNode[] sourceNodes, GroupedFactType[] allGroupFactTypes) {
                int sourceIdx = -1;
                int inGroupIdx = -1;
                int fieldIdx = lhsField.field().valueIndex();

                // Finding source index
                for (int s = 0; s < sourceNodes.length; s++) {
                    ReteKnowledgeNode source = sourceNodes[s];
                    if(source.getFactTypeMask().get(lhsField.fact())) {
                        sourceIdx = s;
                        break;
                    }
                }

                // Finding grouped fact type and its  position in the group
                for (int f = 0; f < allGroupFactTypes.length; f++) {
                    GroupedFactType groupedFactType = allGroupFactTypes[f];
                    if(groupedFactType.getInRuleIndex() == lhsField.fact().getInRuleIndex()) {
                        inGroupIdx = f;
                        break;
                    }
                }

                return new Coordinate(sourceIdx, inGroupIdx, fieldIdx);
            }
        }
    }
}
