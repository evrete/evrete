package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.api.annotations.NonNull;
import org.evrete.runtime.evaluation.EvaluatorOfArray;
import org.evrete.runtime.evaluation.EvaluatorOfPredicate;
import org.evrete.util.MapOfSet;
import org.evrete.util.NamedTypeImpl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

class LhsBuilderImpl<C extends RuntimeContext<C>> implements LhsBuilder<C> {
    private static final Set<EvaluatorHandle> EMPTY_ALPHA_CONDITIONS = new HashSet<>();
    private static final Set<TypeField> EMPTY_TYPE_FIELDS = new HashSet<>();
    private final RuleBuilderImpl<C> ruleBuilder;
    private final DefaultNamedTypeResolver<NamedTypeImpl> declaredLhsTypes;
    private final Set<EvaluatorHandle> betaConditions = new HashSet<>();
    private final MapOfSet<String, EvaluatorHandle> alphaConditions = new MapOfSet<>();
    private final MapOfSet<String, TypeField> betaFields = new MapOfSet<>();
    private final AbstractRuntime<?, C> runtime;

    LhsBuilderImpl(RuleBuilderImpl<C> ruleBuilder) {
        this.ruleBuilder = ruleBuilder;
        this.declaredLhsTypes = new DefaultNamedTypeResolver<>();
        this.runtime = ruleBuilder.getRuntimeContext();
    }

    @Override
    @NonNull
    public NamedType resolve(@NonNull String var) {
        return declaredLhsTypes.resolve(var);
    }

    @Override
    public synchronized NamedTypeImpl addFactDeclaration(String name, Type<?> type) {
        checkRefName(name);
        NamedTypeImpl factType = new NamedTypeImpl(type, name);
        this.declaredLhsTypes.put(name, factType);
        return factType;
    }

    Set<NamedType> getDeclaredFactTypes() {
        return new HashSet<>(declaredLhsTypes.values());
    }

    Set<EvaluatorHandle> getAlphaConditions(NamedType type) {
        return alphaConditions.getOrDefault(type.getName(), EMPTY_ALPHA_CONDITIONS);
    }

    Set<EvaluatorHandle> getBetaConditions() {
        return betaConditions;
    }

    Set<TypeField> getBetaFields(NamedType type) {
        return betaFields.getOrDefault(type.getName(), EMPTY_TYPE_FIELDS);
    }

    private void checkRefName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Null or empty type reference in " + ruleBuilder);
        }

        if (declaredLhsTypes.containsKey(name)) {
            throw new IllegalArgumentException("Duplicate type reference '" + name + "' in rule " + ruleBuilder);
        }
    }

    @Override
    public RuleBuilder<C> create() {
        return ruleBuilder;
    }

    @Override
    public C execute(Consumer<RhsContext> consumer) {
        return ruleBuilder.build(consumer);
    }

    @Override
    public C execute(String literalRhs) {
        return ruleBuilder.build(literalRhs);
    }

    @Override
    public NamedType addFactDeclaration(String name, Class<?> type) {
        Type<?> t = runtime.getTypeResolver().getOrDeclare(type);
        return addFactDeclaration(name, t);
    }

    @Override
    public NamedType addFactDeclaration(String name, String type) {
        return addFactDeclaration(name, runtime.getTypeResolver().getOrDeclare(type));
    }

    @Override
    public RuleBuilder<C> setRhs(String literalConsumer) {
        ruleBuilder.setRhs(literalConsumer);
        return ruleBuilder;
    }

    @Override
    public EvaluatorHandle addWhere(ValuesPredicate predicate, double complexity, FieldReference... references) {
        return add(runtime.addEvaluator(new EvaluatorOfPredicate(predicate, references)));
    }

    @Override
    public EvaluatorHandle addWhere(Predicate<Object[]> predicate, double complexity, FieldReference... references) {
        return add(runtime.addEvaluator(new EvaluatorOfArray(predicate, references)));
    }

    @Override
    //TODO complexity not in use!!!!! check everywhere in this class
    public EvaluatorHandle addWhere(String expression, double complexity) {
        Evaluator evaluator = runtime.compileUnchecked(expression, this);
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
    public LhsBuilderImpl<C> where(EvaluatorHandle... expressions) {
        if (expressions == null) return this;
        for (EvaluatorHandle expression : expressions) {
            add(expression);
        }
        return this;
    }

    @Override
    public LhsBuilderImpl<C> where(String expression, double complexity) {
        whereInner(expression, complexity);
        return this;
    }

    @Override
    public LhsBuilderImpl<C> where(ValuesPredicate predicate, double complexity, String... references) {
        whereInner(predicate, complexity, references);
        return this;
    }

    @Override
    public LhsBuilderImpl<C> where(Predicate<Object[]> predicate, double complexity, FieldReference... references) {
        whereInner(predicate, complexity, references);
        return this;
    }

    @Override
    public LhsBuilderImpl<C> where(Predicate<Object[]> predicate, double complexity, String... references) {
        whereInner(predicate, complexity, references);
        return this;
    }

    @Override
    public LhsBuilderImpl<C> where(ValuesPredicate predicate, double complexity, FieldReference... references) {
        whereInner(predicate, complexity, references);
        return this;
    }

    @Override
    public C execute() {
        return ruleBuilder.build();
    }

    LhsBuilderImpl<C> buildLhs(Collection<FactBuilder> facts) {
        if (facts == null || facts.isEmpty()) return this;
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
        return this;
    }

    private void whereInner(String expression, double complexity) {
        Evaluator evaluator = runtime.compileUnchecked(expression, this);
        add(runtime.addEvaluator(evaluator, complexity));
    }

    private void whereInner(ValuesPredicate predicate, double complexity, FieldReference[] references) {
        EvaluatorOfPredicate evaluator = new EvaluatorOfPredicate(predicate, references);
        add(runtime.addEvaluator(evaluator, complexity));
    }

    private void whereInner(Predicate<Object[]> predicate, double complexity, FieldReference[] references) {
        EvaluatorOfArray evaluator = new EvaluatorOfArray(predicate, references);
        add(runtime.addEvaluator(evaluator, complexity));
    }

    private void whereInner(ValuesPredicate predicate, double complexity, String[] references) {
        FieldReference[] descriptor = resolveFieldReferences(references);
        EvaluatorOfPredicate evaluator = new EvaluatorOfPredicate(predicate, descriptor);
        add(runtime.addEvaluator(evaluator, complexity));
    }

    private void whereInner(Predicate<Object[]> predicate, double complexity, String[] references) {
        FieldReference[] descriptor = resolveFieldReferences(references);
        EvaluatorOfArray evaluator = new EvaluatorOfArray(predicate, descriptor);
        add(runtime.addEvaluator(evaluator, complexity));
    }

    private FieldReference[] resolveFieldReferences(String[] references) {
        return runtime.resolveFieldReferences(references, this);
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
}
