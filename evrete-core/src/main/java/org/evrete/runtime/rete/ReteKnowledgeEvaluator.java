package org.evrete.runtime.rete;

import org.evrete.api.IntToValue;
import org.evrete.api.LhsField;
import org.evrete.runtime.ActiveField;
import org.evrete.runtime.FactType;
import org.evrete.runtime.LhsConditionDH;
import org.evrete.runtime.evaluation.BetaEvaluator;

/**
 * The context-aware version of the {@link BetaEvaluator}. For each component of the parent class,
 * this class contains information on how to obtain values for their
 * {@link org.evrete.api.ValuesPredicate#test(IntToValue)} method arguments.
 */
public class ReteKnowledgeEvaluator {
    private final Component[] components;

    public ReteKnowledgeEvaluator(BetaEvaluator parent, ReteKnowledgeNode node) {
        LhsConditionDH<FactType, ActiveField>[] conditions = parent.getComponents();
        this.components = new Component[conditions.length];
        for (int idx = 0; idx < conditions.length; idx++) {
            LhsConditionDH<FactType, ActiveField> condition = conditions[idx];
            this.components[idx] = new Component(condition, Coordinate.factory(condition, node));
        }
    }

    public Component[] getComponents() {
        return components;
    }

    //TODO use a subclass of LhsConditionDH
    public static class Component {
        private final LhsConditionDH<FactType, ActiveField> delegate;
        private final Coordinate[] coordinates;

        Component(LhsConditionDH<FactType, ActiveField> delegate, Coordinate[] coordinates) {
            this.delegate = delegate;
            this.coordinates = coordinates;
        }

        public LhsConditionDH<FactType, ActiveField> getDelegate() {
            return delegate;
        }

        public Coordinate[] getCoordinates() {
            return coordinates;
        }
    }

    public static class Coordinate {
        public final int inNodeIdx;
        public final int fieldIdx;

        private Coordinate(int inNodeIdx, int fieldIdx) {
            if (fieldIdx < 0 || inNodeIdx < 0) {
                throw new IllegalStateException("Coordinates must have non-negative coordinates: " + inNodeIdx + ", " + fieldIdx);
            } else {
                this.inNodeIdx = inNodeIdx;
                this.fieldIdx = fieldIdx;
            }
        }

        @Override
        public String toString() {
            return "{inNodeIdx=" + inNodeIdx +
                    ", fieldIdx=" + fieldIdx +
                    '}';
        }

        static Coordinate[] factory(LhsConditionDH<FactType, ActiveField> condition, ReteKnowledgeNode node) {
            LhsField.Array<FactType, ActiveField> descriptor = condition.getDescriptor();
            Coordinate[] coordinates = new Coordinate[descriptor.length()];
            for (int i = 0; i < coordinates.length; i++) {
                LhsField<FactType, ActiveField> lhsField = descriptor.get(i);
                coordinates[i] = instance(lhsField, node);
            }
            return coordinates;
        }

        static Coordinate instance(LhsField<FactType, ActiveField> lhsField, ReteKnowledgeNode node) {

            int inNodeIdx = -1;
            FactType[] nodeFactTypes = node.getNodeFactTypes();

            // Finding grouped fact type and its position in the node
            for (int f = 0; f < nodeFactTypes.length; f++) {
                FactType groupedFactType = nodeFactTypes[f];
                if (groupedFactType.getInRuleIndex() == lhsField.fact().getInRuleIndex()) {
                    inNodeIdx = f;
                    break;
                }
            }

            return new Coordinate(inNodeIdx, lhsField.field().valueIndex());
        }
    }
}
