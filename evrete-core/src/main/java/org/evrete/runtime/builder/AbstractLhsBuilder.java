package org.evrete.runtime.builder;

import org.evrete.api.*;
import org.evrete.runtime.AbstractRuntime;
import org.evrete.runtime.evaluation.EvaluatorWrapper;
import org.evrete.util.MapOfSet;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class AbstractLhsBuilder<C extends RuntimeContext<C>, G extends AbstractLhsBuilder<C, ?>> {
    private final RuleBuilderImpl<C> ruleBuilder;
    private final Map<String, FactTypeBuilder> declaredLhsTypes;
    private final Set<AbstractExpression> conditions = new HashSet<>();
    private final Function<String, FactTypeBuilder> factTypeMapper;
    private Compiled compiledData;

    AbstractLhsBuilder(RuleBuilderImpl<C> ruleBuilder) {
        this.ruleBuilder = ruleBuilder;
        this.declaredLhsTypes = new HashMap<>();
        this.factTypeMapper = declaredLhsTypes::get;
    }

    protected abstract G self();

    private TypeResolver getTypeResolver() {
        return ruleBuilder.getRuntimeContext().getTypeResolver();
    }

    public Compiled getCompiledData() {
        if (compiledData == null) {
            throw new IllegalStateException("Conditions not compiled");
        }
        return compiledData;
    }

    public Function<String, NamedType> getFactTypeMapper() {
        return factTypeMapper::apply;
    }

    void compileConditions(AbstractRuntime<?> runtime) {
        if (compiledData != null) {
            throw new IllegalStateException("Conditions already compiled");
        } else {
            this.compiledData = new Compiled(runtime, this);
        }
    }

    private AbstractLhsBuilder<?, ?> locateLhsGroup(NamedType type) {
        FactTypeBuilder builder = factTypeMapper.apply(type.getVar());
        if (builder == null) {
            throw new IllegalStateException();
        } else {
            return builder.getGroup();
        }
    }

    public Set<FactTypeBuilder> getDeclaredFactTypes() {
        return new HashSet<>(declaredLhsTypes.values());
    }

    RuleBuilderImpl<C> getRuleBuilder() {
        return ruleBuilder;
    }

    public G where(String... expressions) {
        if (expressions == null || expressions.length == 0) return self();
        for (String expression : expressions) {
            addExpression(expression);
        }
        return self();
    }

    public final RuntimeContext<C> getRuntime() {
        return ruleBuilder.getRuntime();
    }

    public G where(String expression, double complexity) {
        addExpression(expression, complexity);
        return self();
    }

    public G where(Predicate<Object[]> predicate, double complexity, String... references) {
        addExpression(predicate, complexity, references);
        return self();
    }

    public G where(Predicate<Object[]> predicate, String... references) {
        addExpression(predicate, references);
        return self();
    }

    public G where(ValuesPredicate predicate, double complexity, String... references) {
        addExpression(predicate, complexity, references);
        return self();
    }

    public G where(ValuesPredicate predicate, String... references) {
        addExpression(predicate, references);
        return self();
    }

    public G where(Predicate<Object[]> predicate, double complexity, FieldReference... references) {
        addExpression(predicate, complexity, references);
        return self();
    }

    public G where(Predicate<Object[]> predicate, FieldReference... references) {
        addExpression(predicate, references);
        return self();
    }

    public G where(ValuesPredicate predicate, double complexity, FieldReference... references) {
        addExpression(predicate, complexity, references);
        return self();
    }

    public G where(ValuesPredicate predicate, FieldReference... references) {
        addExpression(predicate, references);
        return self();
    }

    private void addExpression(String expression, double complexity) {
        this.conditions.add(new PredicateExpression0(expression, complexity));
    }

    private void addExpression(Predicate<Object[]> predicate, double complexity, String[] references) {
        this.conditions.add(new PredicateExpression2(predicate, complexity, references));
    }

    private void addExpression(Predicate<Object[]> predicate, String[] references) {
        this.conditions.add(new PredicateExpression2(predicate, references));
    }

    private void addExpression(ValuesPredicate predicate, double complexity, String[] references) {
        this.conditions.add(new PredicateExpression1(predicate, complexity, references));
    }

    private void addExpression(ValuesPredicate predicate, String[] references) {
        this.conditions.add(new PredicateExpression1(predicate, references));
    }

    private void addExpression(Predicate<Object[]> predicate, double complexity, FieldReference[] references) {
        this.conditions.add(new PredicateExpression3(predicate, complexity, references));
    }

    private void addExpression(Predicate<Object[]> predicate, FieldReference[] references) {
        this.conditions.add(new PredicateExpression3(predicate, references));
    }

    private void addExpression(ValuesPredicate predicate, double complexity, FieldReference[] references) {
        this.conditions.add(new PredicateExpression4(predicate, complexity, references));
    }

    private void addExpression(ValuesPredicate predicate, FieldReference[] references) {
        this.conditions.add(new PredicateExpression4(predicate, references));
    }

    private void addExpression(String expression) {
        this.conditions.add(new PredicateExpression0(expression));
    }

    public synchronized FactTypeBuilder buildLhs(String name, Type<?> type) {
        checkRefName(name);
        FactTypeBuilder factType = new FactTypeBuilder(this, name, type);
        this.declaredLhsTypes.put(name, factType);
        return factType;
    }

    public FactTypeBuilder buildLhs(String name, String type) {
        return buildLhs(name, getTypeResolver().getOrDeclare(type));
    }

    G buildLhs(Collection<FactBuilder> facts) {
        if (facts == null || facts.isEmpty()) return self();
        for (FactBuilder f : facts) {
            buildLhs(f.getName(), f.getType());
        }
        return self();
    }

    public FactTypeBuilder buildLhs(String name, Class<?> type) {
        return buildLhs(name, type.getName());
    }

    private void checkRefName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Null or empty type reference in " + ruleBuilder);
        }

        if (declaredLhsTypes.containsKey(name)) {
            throw new IllegalArgumentException("Duplicate type reference '" + name + "' in rule " + ruleBuilder);
        }
    }

    public static class Compiled {
        private static final Set<EvaluatorWrapper> EMPTY_ALPHA_CONDITIONS = new HashSet<>();
        private final Set<EvaluatorWrapper> betaConditions = new HashSet<>();
        private final MapOfSet<NamedType, EvaluatorWrapper> alphaConditions = new MapOfSet<>();
        //private final Set<Evaluator> aggregateConditions = new HashSet<>();
        private final AbstractLhsBuilder<?, ?> lhsBuilder;

        Compiled(AbstractRuntime<?> runtime, AbstractLhsBuilder<?, ?> lhsBuilder) {
            this.lhsBuilder = lhsBuilder;
            for (AbstractExpression condition : lhsBuilder.conditions) {
                Evaluator evaluator = condition.build(runtime, lhsBuilder.getFactTypeMapper());
                this.addCondition(evaluator);
            }
        }


        private void addCondition(Evaluator e) {
            // Wrap the evaluator
            EvaluatorWrapper expression = new EvaluatorWrapper(e);

            FieldReference[] descriptor = expression.descriptor();
            Set<NamedType> involvedTypes = expression.getNamedTypes();

            if (involvedTypes.size() == 1) {
                // Alpha condition
                NamedType type = involvedTypes.iterator().next();
                this.alphaConditions.add(type, expression);
            } else {
                // Beta condition
                Set<AbstractLhsBuilder<?, ?>> involvedGroups = new HashSet<>();
                for (FieldReference ref : descriptor) {
                    FactTypeBuilder factTypeBuilder = lhsBuilder.factTypeMapper.apply(ref.type().getVar());
                    factTypeBuilder.addBetaField(ref);
                    AbstractLhsBuilder<?, ?> refBuilder = lhsBuilder.locateLhsGroup(ref.type());
                    involvedGroups.add(refBuilder);
                }

                if (!involvedGroups.contains(lhsBuilder)) {
                    throw new IllegalStateException("Aggregate group contains external condition: " + expression);
                }

                if (involvedGroups.size() == 1) {
                    // The condition spans this group only
                    this.betaConditions.add(expression);
                } else {
                    // The condition spans several groups, which makes it an aggregate condition
                    for (FieldReference ref : expression.descriptor()) {
                        AbstractLhsBuilder<?, ?> group = lhsBuilder.locateLhsGroup(ref.type());
                        if (group == lhsBuilder) {
                            throw new UnsupportedOperationException("Aggregate groups are currently not supported");
                        }
                    }
                }
            }
        }


        public Set<EvaluatorWrapper> getAlphaConditions(FactTypeBuilder builder) {
            return alphaConditions.getOrDefault(builder, EMPTY_ALPHA_CONDITIONS);
        }

        public Set<EvaluatorWrapper> getBetaConditions() {
            return betaConditions;
        }
    }
}
