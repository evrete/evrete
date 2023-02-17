package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.api.annotations.NonNull;
import org.evrete.runtime.evaluation.EvaluatorOfArray;
import org.evrete.runtime.evaluation.EvaluatorOfPredicate;
import org.evrete.util.NamedTypeImpl;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Predicate;

class LhsBuilderImpl<C extends RuntimeContext<C>> implements LhsBuilder<C> {
    private final RuleBuilderImpl<C> ruleBuilder;
    private final Collection<NamedTypeImpl> declaredLhsTypes;
    private final AbstractRuntime<?, C> runtime;
    private final Collection<Callable<EvaluatorHandle>> conditions = new LinkedList<>();
    private NamedTypeResolver typeResolver;

    LhsBuilderImpl(RuleBuilderImpl<C> ruleBuilder) {
        this.ruleBuilder = ruleBuilder;
        this.declaredLhsTypes = new LinkedList<>();
        this.runtime = ruleBuilder.getRuntimeContext();
    }

    @Override
    public synchronized NamedTypeImpl addFactDeclaration(@NonNull String name, @NonNull Type<?> type) {
        // Resetting type resolver
        this.typeResolver = null;
        NamedTypeImpl factType = new NamedTypeImpl(type, name);
        this.declaredLhsTypes.add(factType);
        return factType;
    }

    Set<NamedType> getDeclaredFactTypes() {
        return new HashSet<>(resolver().resolver.values());
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
    public NamedType addFactDeclaration(@NonNull String name, @NonNull Class<?> type) {
        Type<?> t = runtime.getTypeResolver().getOrDeclare(type);
        return addFactDeclaration(name, t);
    }

    @Override
    public NamedType addFactDeclaration(@NonNull String name, @NonNull String type) {
        return addFactDeclaration(name, runtime.getTypeResolver().getOrDeclare(type));
    }

    @Override
    public RuleBuilder<C> setRhs(String literalConsumer) {
        ruleBuilder.setRhs(literalConsumer);
        return ruleBuilder;
    }


    Collection<EvaluatorHandle> resolveConditions() {
        try {
            Collection<EvaluatorHandle> resolved = new ArrayList<>(this.conditions.size());
            for (Callable<EvaluatorHandle> c : this.conditions) {
                resolved.add(c.call());
            }
            return resolved;
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    public LhsBuilderImpl<C> where(EvaluatorHandle... expressions) {
        if (expressions == null) return this;
        for (EvaluatorHandle expression : expressions) {
            add(() -> expression);
        }
        return this;
    }

    @Override
    public LhsBuilderImpl<C> where(@NonNull String expression, double complexity) {
        whereInner(expression, complexity);
        return this;
    }

    @Override
    public LhsBuilderImpl<C> where(@NonNull ValuesPredicate predicate, double complexity, String... references) {
        whereInner(predicate, complexity, references);
        return this;
    }

    @Override
    public LhsBuilderImpl<C> where(@NonNull Predicate<Object[]> predicate, double complexity, FieldReference... references) {
        whereInner(predicate, complexity, references);
        return this;
    }

    @Override
    public LhsBuilderImpl<C> where(@NonNull Predicate<Object[]> predicate, double complexity, String... references) {
        whereInner(predicate, complexity, references);
        return this;
    }

    @NonNull
    @Override
    public NamedType resolve(@NonNull String var) {
        return resolver().resolve(var);
    }

    @Override
    public LhsBuilderImpl<C> where(@NonNull ValuesPredicate predicate, double complexity, FieldReference... references) {
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
        add(() -> {
            Evaluator evaluator = runtime.compile(expression, LhsBuilderImpl.this);
            return runtime.addEvaluator(evaluator, complexity);
        });
    }

    private void whereInner(ValuesPredicate predicate, double complexity, FieldReference[] references) {
        add(() -> {
            EvaluatorOfPredicate evaluator = new EvaluatorOfPredicate(predicate, references);
            return runtime.addEvaluator(evaluator, complexity);
        });
    }

    private void whereInner(Predicate<Object[]> predicate, double complexity, FieldReference[] references) {
        add(() -> {
            EvaluatorOfArray evaluator = new EvaluatorOfArray(predicate, references);
            return runtime.addEvaluator(evaluator, complexity);
        });
    }

    private void whereInner(ValuesPredicate predicate, double complexity, String[] references) {
        add(() -> {
            FieldReference[] descriptor = resolveFieldReferences(references);
            EvaluatorOfPredicate evaluator = new EvaluatorOfPredicate(predicate, descriptor);
            return runtime.addEvaluator(evaluator, complexity);
        });
    }

    private void whereInner(Predicate<Object[]> predicate, double complexity, String[] references) {
        add(() -> {
            FieldReference[] descriptor = resolveFieldReferences(references);
            EvaluatorOfArray evaluator = new EvaluatorOfArray(predicate, descriptor);
            return runtime.addEvaluator(evaluator, complexity);
        });
    }

    private FieldReference[] resolveFieldReferences(String[] references) {
        return runtime.resolveFieldReferences(references, LhsBuilderImpl.this);
    }

    private void add(Callable<EvaluatorHandle> handle) {
        this.conditions.add(handle);
    }

    private synchronized NamedTypeResolver resolver() {
        if(typeResolver == null) {
            typeResolver = new NamedTypeResolver();
        }
        return typeResolver;
    }


    private class NamedTypeResolver implements NamedType.Resolver{
        private final DefaultNamedTypeResolver<NamedTypeImpl> resolver;

        NamedTypeResolver() {
            this.resolver = new DefaultNamedTypeResolver<>();
            for(NamedTypeImpl t : declaredLhsTypes) {
                this.resolver.put(t.getName(), t);
            }
        }

        @NonNull
        @Override
        public NamedType resolve(@NonNull String var) {
            return resolver.resolve(var);
        }
    }
}
