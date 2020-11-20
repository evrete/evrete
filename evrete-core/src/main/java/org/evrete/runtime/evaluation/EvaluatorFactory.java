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

    public static Collection<EvaluatorGroup> flattenEvaluators(Collection<Evaluator> rawEvaluators, Function<NamedType, FactType> typeFunction) {
        Collection<EvaluatorInternal> evaluators = convert(rawEvaluators, typeFunction);

        MapOfSet<Set<FactType>, EvaluatorInternal> groupedConditions = new MapOfSet<>();

        // Group conditions by involved fact type builders
        for (EvaluatorInternal e : evaluators) {
            Set<FactType> set = Arrays.stream(e.descriptor()).map(FactTypeField::getFactType).collect(Collectors.toSet());
            groupedConditions.computeIfAbsent(set, k -> new HashSet<>()).add(e);
        }

        // Union conditions if they share the same set of fact types
        Collection<EvaluatorGroup> result = new ArrayList<>(groupedConditions.size());
        for (Map.Entry<Set<FactType>, Set<EvaluatorInternal>> entry : groupedConditions.entrySet()) {
            // FactType.toArray(entry.getKey(), typeFunction);
            Collection<EvaluatorInternal> collection = entry.getValue();
            result.add(flattenEvaluators(collection));
        }
        return result;
    }

    private static Collection<EvaluatorInternal> convert(Collection<Evaluator> rawEvaluators, Function<NamedType, FactType> typeFunction) {
        Collection<EvaluatorInternal> evaluators = new ArrayList<>(rawEvaluators.size());
        for (Evaluator e : rawEvaluators) {
            validateExpression(e);
            evaluators.add(new EvaluatorInternal(e, typeFunction));
        }
        return evaluators;
    }

    private static EvaluatorGroup flattenEvaluators(Collection<EvaluatorInternal> collection) {
        assert collection.size() > 0;
        return new EvaluatorGroup(collection);
    }


    public static EvaluatorGroup unionEvaluators(Collection<Evaluator> raw, Function<NamedType, FactType> typeFunction) {
        Collection<EvaluatorInternal> collection = convert(raw, typeFunction);
        return new EvaluatorGroup(collection);
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
