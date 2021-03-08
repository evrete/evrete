package org.evrete.runtime.evaluation;

import org.evrete.api.Evaluator;
import org.evrete.api.FieldReference;
import org.evrete.api.NamedType;
import org.evrete.runtime.BetaFieldReference;
import org.evrete.runtime.FactType;
import org.evrete.util.MapOfSet;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class EvaluatorFactory {

    public static Collection<BetaEvaluator> flattenEvaluators(Collection<EvaluatorWrapper> rawEvaluators, Function<NamedType, FactType> typeFunction) {
        Collection<BetaEvaluatorSingle> evaluators = convert(rawEvaluators, typeFunction);

        MapOfSet<Set<FactType>, BetaEvaluatorSingle> groupedConditions = new MapOfSet<>();

        // Group conditions by involved fact type builders
        for (BetaEvaluatorSingle e : evaluators) {
            Set<FactType> set = Arrays.stream(e.betaDescriptor()).map(BetaFieldReference::getFactType).collect(Collectors.toSet());
            groupedConditions.computeIfAbsent(set, k -> new HashSet<>()).add(e);
        }

        // Union conditions if they share the same set of fact types
        Collection<BetaEvaluator> result = new ArrayList<>(groupedConditions.size());
        for (Map.Entry<Set<FactType>, Set<BetaEvaluatorSingle>> entry : groupedConditions.entrySet()) {
            // FactType.toArray(entry.getKey(), typeFunction);
            Collection<BetaEvaluatorSingle> collection = entry.getValue();
            if (collection.size() == 1) {
                result.add(collection.iterator().next());
            } else {
                result.add(flattenEvaluators(collection));
            }
        }
        return result;
    }

    private static Collection<BetaEvaluatorSingle> convert(Collection<EvaluatorWrapper> rawEvaluators, Function<NamedType, FactType> typeFunction) {
        Collection<BetaEvaluatorSingle> evaluators = new ArrayList<>(rawEvaluators.size());
        for (EvaluatorWrapper e : rawEvaluators) {
            validateExpression(e);
            evaluators.add(new BetaEvaluatorSingle(e, typeFunction));
        }
        return evaluators;
    }

    private static BetaEvaluatorGroup flattenEvaluators(Collection<BetaEvaluatorSingle> collection) {
        assert collection.size() > 0;
        return new BetaEvaluatorGroup(collection);
    }


    private static void validateExpression(Evaluator expression) {
        int refCount = expression.descriptor().length;
        Set<FieldReference> fields = new HashSet<>(Arrays.asList(expression.descriptor()));
        // Check duplicate fields
        if (fields.size() != refCount) {
            throw new UnsupportedOperationException("Duplicate field references like in 'a + a + b > 3' are currently not supported, please declare a new Type.Field instead.");
        }
    }

}
