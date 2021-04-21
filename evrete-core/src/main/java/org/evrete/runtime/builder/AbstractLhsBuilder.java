package org.evrete.runtime.builder;

import org.evrete.api.*;
import org.evrete.runtime.AbstractRuntime;
import org.evrete.runtime.evaluation.EvaluatorOfArray;
import org.evrete.runtime.evaluation.EvaluatorOfPredicate;
import org.evrete.util.MapOfSet;

import java.util.*;
import java.util.function.Predicate;

public abstract class AbstractLhsBuilder<C extends RuntimeContext<C>, G extends AbstractLhsBuilder<C, ?>> implements LhsBuilder<C> {
    private static final Set<EvaluatorHandle> EMPTY_ALPHA_CONDITIONS = new HashSet<>();
    private static final Set<TypeField> EMPTY_TYPE_FIELDS = new HashSet<>();
    private final RuleBuilderImpl<C> ruleBuilder;
    private final Map<String, NamedTypeImpl> declaredLhsTypes;
    private final Set<EvaluatorHandle> betaConditions = new HashSet<>();
    private final MapOfSet<String, EvaluatorHandle> alphaConditions = new MapOfSet<>();
    private final MapOfSet<String, TypeField> betaFields = new MapOfSet<>();

    private final AbstractRuntime<?, ?> runtime;

    AbstractLhsBuilder(RuleBuilderImpl<C> ruleBuilder) {
        this.ruleBuilder = ruleBuilder;
        this.declaredLhsTypes = new HashMap<>();
        this.runtime = ruleBuilder.getRuntimeContext();
    }

    protected abstract G self();

    public Set<EvaluatorHandle> getAlphaConditions(NamedType type) {
        return alphaConditions.getOrDefault(type.getVar(), EMPTY_ALPHA_CONDITIONS);
    }

    public Set<TypeField> getBetaFields(NamedType type) {
        return betaFields.getOrDefault(type.getVar(), EMPTY_TYPE_FIELDS);
    }

    public Set<EvaluatorHandle> getBetaConditions() {
        return betaConditions;
    }

    @Override
    public NamedType resolve(String var) {
        return declaredLhsTypes.get(var);
    }

    private TypeResolver getTypeResolver() {
        return ruleBuilder.getRuntimeContext().getTypeResolver();
    }

    private void addCondition(EvaluatorHandle handle) {
        Set<NamedType> involvedTypes = handle.namedTypes();
        if (involvedTypes.size() == 1) {
            // This is an alpha condition
            this.alphaConditions.add(involvedTypes.iterator().next().getVar(), handle);
        } else {
            // This is a beta condition
            this.betaConditions.add(handle);
            for (FieldReference ref : handle.descriptor()) {
                this.betaFields.add(ref.type().getVar(), ref.field());
            }
        }
    }

    public Set<NamedType> getDeclaredFactTypes() {
        return new HashSet<>(declaredLhsTypes.values());
    }

    @Override
    public RuleBuilderImpl<C> getRuleBuilder() {
        return ruleBuilder;
    }

    @Override
    public G where(String... expressions) {
        if (expressions == null || expressions.length == 0) return self();
        for (String expression : expressions) {
            addExpression(expression);
        }
        return self();
    }

    @Override
    public G where(String expression, double complexity) {
        addExpression(expression, complexity);
        return self();
    }

    @Override
    public G where(Predicate<Object[]> predicate, double complexity, String... references) {
        addExpression(predicate, complexity, references);
        return self();
    }

    @Override
    public G where(Predicate<Object[]> predicate, String... references) {
        addExpression(predicate, references);
        return self();
    }

    @Override
    public G where(ValuesPredicate predicate, double complexity, String... references) {
        addExpression(predicate, complexity, references);
        return self();
    }

    @Override
    public G where(ValuesPredicate predicate, String... references) {
        addExpression(predicate, references);
        return self();
    }

    @Override
    public G where(Predicate<Object[]> predicate, double complexity, FieldReference... references) {
        addExpression(predicate, complexity, references);
        return self();
    }

    @Override
    public G where(Predicate<Object[]> predicate, FieldReference... references) {
        addExpression(predicate, references);
        return self();
    }

    @Override
    public G where(ValuesPredicate predicate, double complexity, FieldReference... references) {
        addExpression(predicate, complexity, references);
        return self();
    }

    @Override
    public G where(ValuesPredicate predicate, FieldReference... references) {
        addExpression(predicate, references);
        return self();
    }

    private void addExpression(String expression, double complexity) {
        Evaluator evaluator = runtime.compile(expression, this, ruleBuilder.getImportsData());
        addCondition(runtime.getEvaluators().save(evaluator, complexity));
    }

    private void addExpression(Predicate<Object[]> predicate, double complexity, String[] references) {
        FieldReference[] descriptor = runtime.resolveFieldReferences(references, this);
        EvaluatorOfArray evaluator = new EvaluatorOfArray(predicate, descriptor);
        addCondition(runtime.getEvaluators().save(evaluator, complexity));
    }

    private void addExpression(Predicate<Object[]> predicate, String[] references) {
        addExpression(predicate, EvaluatorHandle.DEFAULT_COMPLEXITY, references);
    }

    private void addExpression(ValuesPredicate predicate, double complexity, String[] references) {
        FieldReference[] descriptor = runtime.resolveFieldReferences(references, this);
        EvaluatorOfPredicate evaluator = new EvaluatorOfPredicate(predicate, descriptor);
        addCondition(runtime.getEvaluators().save(evaluator, complexity));
    }

    private void addExpression(ValuesPredicate predicate, String[] references) {
        addExpression(predicate, EvaluatorHandle.DEFAULT_COMPLEXITY, references);
    }

    private void addExpression(Predicate<Object[]> predicate, double complexity, FieldReference[] references) {
        EvaluatorOfArray evaluator = new EvaluatorOfArray(predicate, references);
        addCondition(runtime.getEvaluators().save(evaluator, complexity));
    }

    private void addExpression(Predicate<Object[]> predicate, FieldReference[] references) {
        addExpression(predicate, EvaluatorHandle.DEFAULT_COMPLEXITY, references);
    }

    private void addExpression(ValuesPredicate predicate, double complexity, FieldReference[] references) {
        EvaluatorOfPredicate evaluator = new EvaluatorOfPredicate(predicate, references);
        addCondition(runtime.getEvaluators().save(evaluator, complexity));
    }

    private void addExpression(ValuesPredicate predicate, FieldReference[] references) {
        addExpression(predicate, EvaluatorHandle.DEFAULT_COMPLEXITY, references);
    }

    private void addExpression(String expression) {
        addExpression(expression, EvaluatorHandle.DEFAULT_COMPLEXITY);
    }

    @Override
    public synchronized NamedTypeImpl addFactDeclaration(String name, Type<?> type) {
        checkRefName(name);
        NamedTypeImpl factType = new NamedTypeImpl(name, type);
        this.declaredLhsTypes.put(name, factType);
        return factType;
    }

    @Override
    public NamedType addFactDeclaration(String name, String type) {
        return addFactDeclaration(name, getTypeResolver().getOrDeclare(type));
    }

    @Override
    public G buildLhs(Collection<FactBuilder> facts) {
        if (facts == null || facts.isEmpty()) return self();
        for (FactBuilder f : facts) {
            Class<?> c = f.getResolvedType();
            if (c == null) {
                // Unresolved
                addFactDeclaration(f.getName(), f.getUnresolvedType());
            } else {
                // Resolved
                addFactDeclaration(f.getName(), c);
            }
        }
        return self();
    }

    @Override
    public NamedType addFactDeclaration(String name, Class<?> type) {
        Type<?> t = getTypeResolver().getOrDeclare(type);
        return addFactDeclaration(name, t);
    }

    private void checkRefName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Null or empty type reference in " + ruleBuilder);
        }

        if (declaredLhsTypes.containsKey(name)) {
            throw new IllegalArgumentException("Duplicate type reference '" + name + "' in rule " + ruleBuilder);
        }
    }

    static class NamedTypeImpl implements NamedType {
        private final String name;
        private final Type<?> type;

        NamedTypeImpl(String name, Type<?> type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public Type<?> getType() {
            return type;
        }

        @Override
        public String getVar() {
            return name;
        }
    }
/*
    public static class Compiled {
        private static final Set<EvaluatorWrapper> EMPTY_ALPHA_CONDITIONS = new HashSet<>();
        private final Set<EvaluatorWrapper> betaConditions = new HashSet<>();
        private final MapOfSet<NamedType, EvaluatorWrapper> alphaConditions = new MapOfSet<>();
        private final AbstractLhsBuilder<?, ?> lhsBuilder;

        Compiled(AbstractRuntime<?, ?> runtime, AbstractLhsBuilder<?, ?> lhsBuilder) {
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
*/
}
