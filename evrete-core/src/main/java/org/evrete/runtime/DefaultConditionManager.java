package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.api.builders.ConditionManager;
import org.evrete.runtime.evaluation.DefaultEvaluatorHandle;
import org.evrete.runtime.evaluation.ValuePredicateOfArray;
import org.evrete.util.CommonUtils;

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

class DefaultConditionManager extends RuntimeAware implements ConditionManager {
    private final Collection<Literal> literals = new LinkedList<>();
    private final Collection<LhsConditionDH<String, ActiveField>> evaluators = new LinkedList<>();
    private final NamedType.Resolver namedTypeResolver;

    DefaultConditionManager(AbstractRuntime<?, ?> runtime, NamedType.Resolver namedTypeResolver) {
        super(runtime);
        this.namedTypeResolver = namedTypeResolver;
    }

    void addLhsBuilderCondition(String expression, double complexity) {
        this.addCondition(expression, complexity);
    }

    void addLhsBuilderCondition(ValuesPredicate predicate, double complexity, String[] references) {
        this.addCondition(predicate, complexity, references);
    }

    void addLhsBuilderCondition(Predicate<Object[]> predicate, double complexity, String[] references) {
        this.addCondition(predicate, complexity, references);
    }

    Collection<Literal> getLiterals() {
        return literals;
    }

    Collection<LhsConditionDH<String, ActiveField>> getEvaluators() {
        return evaluators;
    }

    @Override
    public EvaluatorHandle addCondition(ValuesPredicate predicate, double complexity, String... references) {
        // 1. Convert field references to LHS fields
        LhsField.Array<String, TypeField> stringFields = toFields(references, namedTypeResolver);
        LhsField.Array<String, ActiveField> activeFields = runtime.toActiveFields(stringFields);
        // 2. Obtain the handle
        DefaultEvaluatorHandle handle = runtime.getEvaluatorsContext().addEvaluator(predicate, complexity, activeFields);
        // 3. Register the condition
        this.evaluators.add(new LhsConditionDH<>(handle, activeFields));
        return handle;
    }

    @Override
    public EvaluatorHandle addCondition(Predicate<Object[]> predicate, double complexity, String... references) {
        return this.addCondition(new ValuePredicateOfArray(predicate, references.length), complexity, references);
    }

    @Override
    public CompletableFuture<EvaluatorHandle> addCondition(String expression, double complexity) {
        Literal condition = new Literal(expression, complexity);
        this.literals.add(condition);
        return condition.getHandle().thenApply(handle -> handle);
    }

    private static LhsField.Array<String, TypeField> toFields(String[] fieldNames, NamedType.Resolver namedTypeResolver) {
        LhsField.Array<String, String> lhsFields = LhsField.Array.fromDottedVariables(fieldNames);
        return lhsFields.transform(lhsField -> CommonUtils.toTypeField(lhsField, namedTypeResolver));
    }


    static class Literal implements LiteralPredicate {
        private final String expression;
        private final double complexity;
        private final CompletableFuture<DefaultEvaluatorHandle> handle;

        public Literal(String expression, double complexity) {
            this.expression = expression;
            this.complexity = complexity;
            this.handle = new CompletableFuture<>();
        }

        @Override
        public String getSource() {
            return expression;
        }

        @Override
        public double getComplexity() {
            return complexity;
        }

        public CompletableFuture<DefaultEvaluatorHandle> getHandle() {
            return handle;
        }

        @Override
        public String toString() {
            if(complexity == WorkUnit.DEFAULT_COMPLEXITY) {
                return "'" + expression + "'";
            } else {
                return "{" +
                        "'" + expression + '\'' +
                        ", complexity=" + complexity +
                        '}';
            }
        }
    }
}
