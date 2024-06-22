package org.evrete.runtime;

import org.evrete.api.LhsField;
import org.evrete.runtime.evaluation.BetaEvaluator;
import org.evrete.runtime.evaluation.DefaultEvaluatorHandle;
import org.evrete.util.MapFunction;

import java.util.*;
import java.util.function.Function;

/**
 * A utility class to analyze ruleset conditions and help building runtime fact types and Rete condition nodes
 */
class RuleBuilderActiveConditions {
    private final Collection<LhsConditionDH<String, ActiveField>> allBetaConditions = new LinkedList<>();
    private final MapOfSet<String, DefaultEvaluatorHandle> alphaConditionsByFactName = new MapOfSet<>();

    void add(LhsConditionDH<String, ActiveField> condition) {
        LhsField.Array<String, ActiveField> descriptor = condition.getDescriptor();
        if (descriptor.length() == 0) {
            throw new IllegalStateException("Empty condition descriptor");
        } else if (descriptor.length() == 1) {
            // This is an alpha condition
            String factVarName = descriptor.get(0).fact();
            alphaConditionsByFactName.add(factVarName, condition.getCondition());
        } else {
            // This is a beta condition, storing the pair in a helper format
            allBetaConditions.add(condition);
        }
    }

    Set<DefaultEvaluatorHandle> getAlphaConditionsOf(String factName) {
        return alphaConditionsByFactName.getOrDefault(factName, Collections.emptySet());
    }

    /**
     * Combines conditions with the same signature into a single beta condition.
     * Two conditions are said to have the same signature if their descriptors refer to the same set
     * of LHS types. Combining beta conditions greatly simplifies building RETE condition graphs and
     * decreases the total count of RETE condition nodes we will need to create.
     *
     * @param ruleFactTypes The fact type declarations of a rule.
     * @return A collection of grouped beta evaluators
     */
    Collection<BetaEvaluator> flattenBetaConditions(Collection<FactType> ruleFactTypes) {
        // 0. Initial check
        if (allBetaConditions.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. Create a mapping between fact name's variable and its fact type declaration
        Function<String, FactType> nameToIndexMapping = new MapFunction<>(ruleFactTypes, FactType::getVarName);

        // 2. Group conditions by the fact types involved
        MapOfSet<Mask<FactType>, LhsConditionDH<FactType, ActiveField>> grouping = new MapOfSet<>();
        for (LhsConditionDH<String, ActiveField> condition : allBetaConditions) {
            // Turning String fact names into FactType instances
            LhsField.Array<FactType, ActiveField> descriptor = condition.getDescriptor()
                    .transform(lhsField -> {
                        FactType referencedType = nameToIndexMapping.apply(lhsField.fact());
                        return new LhsField<>(referencedType, lhsField);
                    });

            // Compute the mask value (every bit represents the fact's in-rule index)
            Mask<FactType> mask = Mask.factTypeMask();
            for (int i = 0; i < descriptor.length(); i++) {
                mask.set(descriptor.get(i).fact());
            }
            grouping.add(mask, new LhsConditionDH<>(condition, descriptor));
        }

        // 3. Map to beta-conditions and return
        Collection<BetaEvaluator> result = new ArrayList<>(allBetaConditions.size());
        for (Map.Entry<Mask<FactType>, Set<LhsConditionDH<FactType, ActiveField>>> entry : grouping.entrySet()) {
            Mask<FactType> mask = entry.getKey();
            result.add(new BetaEvaluator(mask, entry.getValue()));
        }
        return result;
    }
}
