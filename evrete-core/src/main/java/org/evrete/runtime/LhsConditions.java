package org.evrete.runtime;

import org.evrete.api.Evaluator;
import org.evrete.api.EvaluatorHandle;
import org.evrete.api.LiteralExpression;
import org.evrete.api.annotations.NonNull;
import org.evrete.util.WorkUnitObject;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;

/**
 * <p>
 *     Collection of LHS conditions for a rule builder.
 * </p>
 */
class LhsConditions {
    // References to existing conditions
    final Collection<EvaluatorHandle> directHandles = new LinkedList<>();
    // Functional conditions
    final Collection<WorkUnitObject<Evaluator>> evaluators = new LinkedList<>();
    // Literal conditions
    final Collection<WorkUnitObject<LiteralExpression>> literals = new LinkedList<>();

    void add(@NonNull Evaluator evaluator, double complexity) {
        this.evaluators.add(new WorkUnitObject<>(Objects.requireNonNull(evaluator), complexity));
    }

    void add(@NonNull LiteralExpression expression, double complexity) {
        this.literals.add(new WorkUnitObject<>(Objects.requireNonNull(expression), complexity));
    }

    void add(@NonNull EvaluatorHandle handle) {
        this.directHandles.add(Objects.requireNonNull(handle));
    }
}
