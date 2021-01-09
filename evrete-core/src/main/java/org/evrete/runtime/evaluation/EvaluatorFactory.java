package org.evrete.runtime.evaluation;

import org.evrete.api.Evaluator;
import org.evrete.api.NamedType;
import org.evrete.runtime.FactType;
import org.evrete.runtime.FactTypeField;
import org.evrete.runtime.builder.FieldReference;
import org.evrete.util.MapOfSet;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class EvaluatorFactory {

    public static Collection<BetaEvaluatorGroup> flattenEvaluators(Collection<EvaluatorWrapper> rawEvaluators, Function<NamedType, FactType> typeFunction) {
        Collection<BetaEvaluator> evaluators = convert(rawEvaluators, typeFunction);

        MapOfSet<Set<FactType>, BetaEvaluator> groupedConditions = new MapOfSet<>();

        // Group conditions by involved fact type builders
        for (BetaEvaluator e : evaluators) {
            Set<FactType> set = Arrays.stream(e.betaDescriptor()).map(FactTypeField::getFactType).collect(Collectors.toSet());
            groupedConditions.computeIfAbsent(set, k -> new HashSet<>()).add(e);
        }

        // Union conditions if they share the same set of fact types
        Collection<BetaEvaluatorGroup> result = new ArrayList<>(groupedConditions.size());
        for (Map.Entry<Set<FactType>, Set<BetaEvaluator>> entry : groupedConditions.entrySet()) {
            // FactType.toArray(entry.getKey(), typeFunction);
            Collection<BetaEvaluator> collection = entry.getValue();
            result.add(flattenEvaluators(collection));
        }
        return result;
    }

    private static Collection<BetaEvaluator> convert(Collection<EvaluatorWrapper> rawEvaluators, Function<NamedType, FactType> typeFunction) {
        Collection<BetaEvaluator> evaluators = new ArrayList<>(rawEvaluators.size());
        for (EvaluatorWrapper e : rawEvaluators) {
            validateExpression(e);
            evaluators.add(new BetaEvaluator(e, typeFunction));
        }
        return evaluators;
    }

    private static BetaEvaluatorGroup flattenEvaluators(Collection<BetaEvaluator> collection) {
        assert collection.size() > 0;
        return new BetaEvaluatorGroup(collection);
    }


    public static BetaEvaluatorGroup unionEvaluators(Collection<EvaluatorWrapper> raw, Function<NamedType, FactType> typeFunction) {
        Collection<BetaEvaluator> collection = convert(raw, typeFunction);
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
