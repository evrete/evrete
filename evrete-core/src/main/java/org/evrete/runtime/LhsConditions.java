package org.evrete.runtime;

import org.evrete.api.Evaluator;
import org.evrete.api.EvaluatorHandle;
import org.evrete.api.LiteralEvaluator;
import org.evrete.api.LiteralExpression;
import org.evrete.api.annotations.NonNull;
import org.evrete.runtime.compiler.CompilationException;

import java.util.*;
import java.util.function.Function;

class LhsConditions {
    private final Collection<EvaluatorHandle> directHandles = new LinkedList<>();
    private final Collection<WithComplexity<Evaluator>> evaluators = new LinkedList<>();
    private final Collection<WithComplexity<LiteralExpression>> literals = new LinkedList<>();

    void add(@NonNull Evaluator evaluator, double complexity) {
        this.evaluators.add(new WithComplexity<>(Objects.requireNonNull(evaluator), complexity));
    }

    void add(@NonNull LiteralExpression expression, double complexity) {
        this.literals.add(new WithComplexity<>(Objects.requireNonNull(expression), complexity));
    }

    void add(@NonNull EvaluatorHandle handle) {
        this.directHandles.add(Objects.requireNonNull(handle));
    }

    static <T extends LhsConditionsHolder> Function<T, LhsConditionHandles> compile(AbstractRuntime<?, ?> runtime, Collection<T> sources) throws CompilationException {
        Map<T, LhsConditionHandles> map = new IdentityHashMap<>();

        Map<LiteralExpression, LiteralExpressionHelper<T>> allExpressions = new IdentityHashMap<>();
        for(T source : sources) {
            LhsConditions conditions = source.getConditions();
            for(WithComplexity<LiteralExpression> e : conditions.literals) {
                allExpressions.put(e.subject, new LiteralExpressionHelper<>(source, e.complexity));
            }
        }

        Collection<LiteralExpression> allSources = allExpressions.keySet();
        Map<T, Collection<WithComplexity<Evaluator>>> perSourceLiterals = new IdentityHashMap<>();

        if(!allSources.isEmpty()) {
            Collection<LiteralEvaluator> allCompiled = runtime.getExpressionResolver().buildExpressions(allSources);
            for(LiteralEvaluator e : allCompiled) {
                LiteralExpressionHelper<T> helper = allExpressions.get(e.getSource());
                if(helper != null) {
                    perSourceLiterals.computeIfAbsent(helper.holder, t -> new LinkedList<>()).add(new WithComplexity<>(e, helper.complexity));
                } else {
                    throw new IllegalStateException("Couldn't find source condition by identity");
                }
            }
        }

        // Build the result
        for(T source : sources) {
            LhsConditionHandles handles = map.computeIfAbsent(source, k->new LhsConditionHandles());
            LhsConditions conditions = source.getConditions();

            // Add direct handles
            for(EvaluatorHandle d : conditions.directHandles) {
                handles.add(d);
            }

            // Add evaluators
            for(WithComplexity<Evaluator> d : conditions.evaluators) {
                EvaluatorHandle h = runtime.addEvaluator(d.subject, d.complexity);
                handles.add(h);
            }

            // Add compiled literal evaluators
            Collection<WithComplexity<Evaluator>> literals = perSourceLiterals.get(source);
            if(literals != null) {
                for(WithComplexity<Evaluator> d : literals) {
                    EvaluatorHandle h = runtime.addEvaluator(d.subject, d.complexity);
                    handles.add(h);
                }
            }
        }

        return t -> Objects.requireNonNull(map.get(t), "Illegal state");
    }

    static class LiteralExpressionHelper<T extends LhsConditionsHolder> {
        final T holder;
        final double complexity;

        public LiteralExpressionHelper(T holder, double complexity) {
            this.holder = holder;
            this.complexity = complexity;
        }
    }

    static class WithComplexity<T> {
        final T subject;
        final double complexity;

        WithComplexity(T subject, double complexity) {
            this.subject = subject;
            this.complexity = complexity;
        }
    }
}
