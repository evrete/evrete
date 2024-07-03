package org.evrete.runtime.evaluation;

import org.evrete.api.WorkUnit;
import org.evrete.runtime.ActiveField;
import org.evrete.runtime.FactType;
import org.evrete.runtime.LhsConditionDH;
import org.evrete.runtime.Mask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Non-empty collection of beta-evaluator references sorted by complexity. Every component of this group must evaluate
 * the same set of declared fact types.
 */
public class BetaEvaluator implements WorkUnit {
    private final LhsConditionDH<FactType, ActiveField>[] components;
    private final double complexity;
    private final Mask<FactType> typeMask;


    @SuppressWarnings("unchecked")
    public BetaEvaluator(Mask<FactType> typeMask, Set<LhsConditionDH<FactType, ActiveField>> conditions) {
        // Beta condition may contain several internal conditions, all dealing with the same
        // set of fact types. During the condition evaluation it's important that they're ordered
        List<LhsConditionDH<FactType, ActiveField>> sortedConditions = new ArrayList<>(conditions);
        sortedConditions.sort((c1, c2) -> {
            // Sort by explicit complexity first
            int cmp = Double.compare(c1.getCondition().getComplexity(), c2.getCondition().getComplexity());
            if (cmp == 0) {
                // Otherwise, fall back to comparing the respective field count
                return Integer.compare(c1.getDescriptor().length(), c2.getDescriptor().length());
            } else {
                return cmp;
            }
        });

        // Compute aggregate complexity
        double c = 0.0;
        int i = 0;
        this.components = (LhsConditionDH<FactType, ActiveField>[]) new LhsConditionDH<?,?>[sortedConditions.size()];
        for (LhsConditionDH<FactType, ActiveField> condition : sortedConditions) {
            c += condition.getCondition().getComplexity();
            this.components[i++] = condition;
        }
        this.complexity = c;
        this.typeMask = typeMask;
    }

    public LhsConditionDH<FactType, ActiveField>[] getComponents() {
        return components;
    }

    public Mask<FactType> getTypeMask() {
        return typeMask;
    }

    @Override
    public double getComplexity() {
        return this.complexity;
    }

    @Override
    public String toString() {
        return Arrays.toString(components);
    }

    public static int compare(BetaEvaluator e1, BetaEvaluator e2) {
        int typesInvolved1 = e1.getTypeMask().cardinality();
        int typesInvolved2 = e2.getTypeMask().cardinality();
        int cmp1 = Integer.compare(typesInvolved1, typesInvolved2);
        if (cmp1 == 0) {
            return Double.compare(e1.getComplexity(), e2.getComplexity());
        } else {
            return cmp1;
        }
    }
}
