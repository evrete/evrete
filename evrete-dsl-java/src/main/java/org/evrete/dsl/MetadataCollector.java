package org.evrete.dsl;

import org.evrete.api.*;

import java.util.*;

class MetadataCollector {

    final Set<Class<?>> classesToInstantiate = new HashSet<>();

    final Map<Class<?>, Collection<RhsUpdate>> rhsUpdateTasks = new HashMap<>();
    final Map<Class<?>, Collection<ConditionUpdate>> conditionUpdateTasks = new HashMap<>();
    final Map<Class<?>, Collection<WrappedFieldDeclarationMethod<?,?>>> fieldDeclarationMethods = new HashMap<>();
    final Map<Class<?>, Collection<WrappedEventSubscriptionMethod<?>>> eventSubscriptionMethods = new HashMap<>();

    void scheduleConditionMethodUpdate(EvaluatorHandle evaluatorHandle, WrappedConditionMethod condition) {
        this.conditionUpdateTasks.computeIfAbsent(registerKey(condition), k->new LinkedList<>())
                .add(new ConditionUpdate(evaluatorHandle, condition));
    }

    void scheduleRhsMethodUpdate(String ruleName, WrappedRhsMethod rhs) {
        this.rhsUpdateTasks.computeIfAbsent(registerKey(rhs), k -> new LinkedList<>())
                .add(new RhsUpdate(ruleName, rhs));
    }

    void addFieldDeclarationMethod(WrappedFieldDeclarationMethod<?,?> method) {
        this.fieldDeclarationMethods.computeIfAbsent(registerKey(method), k -> new LinkedList<>()).add(method);
    }

    void addEventSubscriptionMethod(WrappedEventSubscriptionMethod<?> method) {
        this.eventSubscriptionMethods.computeIfAbsent(registerKey(method), k -> new LinkedList<>()).add(method);
    }

    private Class<?> registerKey(WrappedMethod method) {
        Class<?> key = method.declaringClass.delegate;
        this.classesToInstantiate.add(key);
        return key;
    }

    void applyToSession(RuleSession<?> newSession) {
        // When a new session is spawned, every virtual method we've collected must be bound
        // to its respective class instance
        Map<Class<?>, Object> instances = new HashMap<>(classesToInstantiate.size());

        // 1. Create instances of each class
        for (Class<?> clazz : classesToInstantiate) {
            try {
                Object newInstance = clazz.getConstructor().newInstance();
                instances.put(clazz, newInstance);
            } catch (Exception e) {
                throw new IllegalStateException("Unable to create class instance", e);
            }
        }

        // 2. Update scheduled RHS actions
        for(Map.Entry<Class<?>, Collection<RhsUpdate>> entry : rhsUpdateTasks.entrySet()) {
            Object instance = instances.get(entry.getKey());
            for(RhsUpdate rhsUpdate : entry.getValue()) {
                Rule rule = newSession.getRule(rhsUpdate.ruleName);
                if (rule != null) {
                    rule.setRhs(rhsUpdate.rhs.bindTo(instance));
                }
            }
        }

        // 3. Update conditions
        for(Map.Entry<Class<?>, Collection<ConditionUpdate>> entry : conditionUpdateTasks.entrySet()) {
            Object instance = instances.get(entry.getKey());
            EvaluatorsContext evaluatorsContext = newSession.getEvaluatorsContext();
            for(ConditionUpdate conditionUpdate : entry.getValue()) {
                evaluatorsContext
                        .replacePredicate(conditionUpdate.evaluatorHandle, conditionUpdate.condition.bindTo(instance));
            }
        }

        // 4. Update field declarations
        for(Map.Entry<Class<?>, Collection<WrappedFieldDeclarationMethod<?, ?>>> entry : fieldDeclarationMethods.entrySet()) {
            Object instance = instances.get(entry.getKey());

            for(WrappedFieldDeclarationMethod<?, ?> fieldDeclaration : entry.getValue()) {
                newSession.configureTypes(typeResolver -> fieldDeclaration.bindTo(instance).selfRegister(typeResolver));
            }
        }

        // 5. Create subscriptions
        for(Map.Entry<Class<?>, Collection<WrappedEventSubscriptionMethod<?>>> entry : eventSubscriptionMethods.entrySet()) {
            Object instance = instances.get(entry.getKey());

            for(WrappedEventSubscriptionMethod<?> fieldDeclaration : entry.getValue()) {
                newSession.configureTypes(typeResolver -> fieldDeclaration.bindTo(instance).selfSubscribe(newSession));
            }
        }
    }

    static class RhsUpdate {
        final String ruleName;
        final WrappedRhsMethod rhs;

        public RhsUpdate(String ruleName, WrappedRhsMethod rhs) {
            this.ruleName = ruleName;
            this.rhs = rhs;
        }
    }

    static class ConditionUpdate {
        final EvaluatorHandle evaluatorHandle;
        final WrappedConditionMethod condition;

        ConditionUpdate(EvaluatorHandle evaluatorHandle, WrappedConditionMethod condition) {
            this.evaluatorHandle = evaluatorHandle;
            this.condition = condition;
        }
    }
}
