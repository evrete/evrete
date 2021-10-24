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
        return alphaConditions.getOrDefault(type.getName(), EMPTY_ALPHA_CONDITIONS);
    }

    public Set<TypeField> getBetaFields(NamedType type) {
        return betaFields.getOrDefault(type.getName(), EMPTY_TYPE_FIELDS);
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

    private EvaluatorHandle add(EvaluatorHandle handle) {
        Set<NamedType> involvedTypes = handle.namedTypes();
        if (involvedTypes.size() == 1) {
            // This is an alpha condition
            this.alphaConditions.add(involvedTypes.iterator().next().getName(), handle);
        } else {
            // This is a beta condition
            this.betaConditions.add(handle);
            for (FieldReference ref : handle.descriptor()) {
                this.betaFields.add(ref.type().getName(), ref.field());
            }
        }
        return handle;
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
            whereInner(expression);
        }
        return self();
    }

    @Override
    public G where(EvaluatorHandle... expressions) {
        if (expressions == null || expressions.length == 0) return self();
        for (EvaluatorHandle expression : expressions) {
            add(expression);
        }
        return self();
    }

    @Override
    public G where(String expression, double complexity) {
        whereInner(expression, complexity);
        return self();
    }

    @Override
    public G where(Predicate<Object[]> predicate, double complexity, String... references) {
        whereInner(predicate, complexity, references);
        return self();
    }

    @Override
    public G where(Predicate<Object[]> predicate, String... references) {
        whereInner(predicate, references);
        return self();
    }

    @Override
    public G where(ValuesPredicate predicate, double complexity, String... references) {
        whereInner(predicate, complexity, references);
        return self();
    }

    @Override
    public G where(ValuesPredicate predicate, String... references) {
        whereInner(predicate, references);
        return self();
    }

    @Override
    public G where(Predicate<Object[]> predicate, double complexity, FieldReference... references) {
        whereInner(predicate, complexity, references);
        return self();
    }

    @Override
    public G where(Predicate<Object[]> predicate, FieldReference... references) {
        whereInner(predicate, references);
        return self();
    }

    @Override
    public G where(ValuesPredicate predicate, double complexity, FieldReference... references) {
        whereInner(predicate, complexity, references);
        return self();
    }

    @Override
    public G where(ValuesPredicate predicate, FieldReference... references) {
        whereInner(predicate, references);
        return self();
    }

    @Override
    public EvaluatorHandle addWhere(String expression, double complexity, ClassLoader classLoader, Properties properties) {
        Evaluator evaluator = runtime.compile(expression, this, ruleBuilder.getImports(), classLoader, properties);
        return add(runtime.addEvaluator(evaluator));
    }

    @Override
    public EvaluatorHandle addWhere(ValuesPredicate predicate, double complexity, String... references) {
        return addWhere(predicate, complexity, resolveFieldReferences(references));
    }

    @Override
    public EvaluatorHandle addWhere(Predicate<Object[]> predicate, double complexity, String... references) {
        return addWhere(predicate, complexity, resolveFieldReferences(references));
    }

    @Override
    public EvaluatorHandle addWhere(ValuesPredicate predicate, double complexity, FieldReference... references) {
        Evaluator evaluator = new EvaluatorOfPredicate(predicate, references);
        return add(runtime.addEvaluator(evaluator));
    }

    @Override
    public EvaluatorHandle addWhere(Predicate<Object[]> predicate, double complexity, FieldReference... references) {
        Evaluator evaluator = new EvaluatorOfArray(predicate, references);
        return add(runtime.addEvaluator(evaluator));
    }

    private void whereInner(String expression, double complexity) {
        Evaluator evaluator = runtime.compile(expression, this, ruleBuilder.getImports());
        add(runtime.addEvaluator(evaluator, complexity));
    }

    private void whereInner(Predicate<Object[]> predicate, double complexity, String[] references) {
        FieldReference[] descriptor = resolveFieldReferences(references);
        EvaluatorOfArray evaluator = new EvaluatorOfArray(predicate, descriptor);
        add(runtime.addEvaluator(evaluator, complexity));
    }

    private void whereInner(Predicate<Object[]> predicate, String[] references) {
        whereInner(predicate, EvaluatorHandle.DEFAULT_COMPLEXITY, references);
    }

    private void whereInner(ValuesPredicate predicate, double complexity, String[] references) {
        FieldReference[] descriptor = resolveFieldReferences(references);
        EvaluatorOfPredicate evaluator = new EvaluatorOfPredicate(predicate, descriptor);
        add(runtime.addEvaluator(evaluator, complexity));
    }

    private void whereInner(ValuesPredicate predicate, String[] references) {
        whereInner(predicate, EvaluatorHandle.DEFAULT_COMPLEXITY, references);
    }

    private void whereInner(Predicate<Object[]> predicate, double complexity, FieldReference[] references) {
        EvaluatorOfArray evaluator = new EvaluatorOfArray(predicate, references);
        add(runtime.addEvaluator(evaluator, complexity));
    }

    private void whereInner(Predicate<Object[]> predicate, FieldReference[] references) {
        whereInner(predicate, EvaluatorHandle.DEFAULT_COMPLEXITY, references);
    }

    private void whereInner(ValuesPredicate predicate, double complexity, FieldReference[] references) {
        EvaluatorOfPredicate evaluator = new EvaluatorOfPredicate(predicate, references);
        add(runtime.addEvaluator(evaluator, complexity));
    }

    private void whereInner(ValuesPredicate predicate, FieldReference[] references) {
        whereInner(predicate, EvaluatorHandle.DEFAULT_COMPLEXITY, references);
    }

    private void whereInner(String expression) {
        whereInner(expression, EvaluatorHandle.DEFAULT_COMPLEXITY);
    }

    private FieldReference[] resolveFieldReferences(String[] references) {
        return runtime.resolveFieldReferences(references, this);
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

    G buildLhs(Collection<FactBuilder> facts) {
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
        public String getName() {
            return name;
        }
    }
}
