package org.evrete.dsl;

import org.evrete.api.*;
import org.evrete.api.annotations.Nullable;
import org.evrete.api.builders.LhsBuilder;
import org.evrete.api.builders.RuleBuilder;
import org.evrete.api.builders.RuleSetBuilder;
import org.evrete.dsl.annotation.MethodPredicate;
import org.evrete.dsl.annotation.Rule;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.Consumer;

//TODO duplicate rule names must throw exception!!! Both in AJR and in the Core API
abstract class DSLMeta<C extends RuntimeContext<C>> {
    final MethodHandles.Lookup globalPublicLookup;

    DSLMeta(MethodHandles.Lookup globalPublicLookup) {
        this.globalPublicLookup = globalPublicLookup;
    }

    @Nullable
    abstract JavaSourceCompiler.ClassSource sourceToCompile();

    abstract void applyCompiledSource(Class<?> compiledClass);

    abstract RulesClass getPreparedData();

    final void collectMetaData(RuntimeContext<C> context, MetadataCollector collector) {
        RulesClass prepared = getPreparedData();
        if(prepared == null) {
            throw new IllegalStateException("Did not find any prepared class, please report it as a bug.");
        } else {
            // The goal is collect and store auxiliary Java methods such as field declarations and various listeners
            // Static methods will be applied in-place, while virtual ones stored in the collector and bound to
            // specific lifecycle events.

            // 1. Field declaration
            for (WrappedFieldDeclarationMethod<?,?> m : prepared.fieldDeclarationMethods) {
                if(m.isStatic) {
                    // Field declarations expressed as static methods can be applied immediately
                    context.configureTypes(m::selfRegister);
                } else {
                    // Virtual methods declarations are postponed
                    context.configureTypes(m::dummyRegister);
                    collector.addFieldDeclarationMethod(m);
                }
            }

            // 2. Event subscriptions
            for (WrappedEventSubscriptionMethod<?> m : prepared.subscriptionMethods) {
                if(m.isStatic) {
                    m.selfSubscribe(context);
                } else {
                    collector.addEventSubscriptionMethod(m);
                }
            }
        }

    }

    void applyTo(RuleSetBuilder<C> target, MetadataCollector collector) {
        RulesClass prepared = getPreparedData();

        org.evrete.dsl.annotation.RuleSet.Sort defaultSort = Utils.deriveSort(prepared.delegate);
        prepared.ruleMethods.sort(new RuleComparator(defaultSort));


        // 1. Read and define each rule
        for(RuleMethod ruleMethod : prepared.ruleMethods) {
            // 2.1 Rule name & salience
            RuleBuilder<C> ruleBuilder = target.newRule(ruleMethod.getRuleName());

            int salience = ruleMethod.getSalience();
            if(salience != Rule.DEFAULT_SALIENCE) {
                ruleBuilder.salience(salience);
            }

            LhsBuilder<C> lhsBuilder = ruleBuilder.getLhs();
            // 2.2 Fact declarations
            for(RuleMethod.FactDeclaration factDeclaration : ruleMethod.rhs.factDeclarations) {
                Class<?> cl = Utils.box(factDeclaration.javaType);
                if (factDeclaration.logicalType == null) {
                    // No explicit logical type
                    lhsBuilder.addFactDeclaration(factDeclaration.name, cl);
                } else {
                    lhsBuilder.addFactDeclaration(factDeclaration.name, factDeclaration.logicalType);
                }
            }
            // 2.3 Literal conditions
            lhsBuilder.where(ruleMethod.literalConditions);

            // 2.4 Method conditions
            for(MethodPredicate mp :  ruleMethod.methodPredicates) {
                // For each method condition we need to find the referenced method first
                String methodName = mp.method();
                LhsField.Array<String, TypeField> descriptor = LhsField.Array.toFields(mp.args(), ruleBuilder);
                Class<?>[] signature = Utils.asMethodSignature(descriptor);
                MethodType methodType = MethodType.methodType(boolean.class, signature);
                WrappedMethod conditionMethod = prepared.lookup(methodName, methodType);
                WrappedConditionMethod condition = new WrappedConditionMethod(conditionMethod);
                if(conditionMethod.isStatic) {
                    // Static conditions can be added right away
                    ruleBuilder.getConditionManager().addCondition(condition, mp.args());
                } else {
                    // Virtual condition methods will require a class instance. Setting a dummy condition
                    // and scheduling its update for each new session

                    // It's important to create new instance of failing conditions.
                    // DO NOT USE functional interfaces instead, otherwise assigning condition handles WILL FAIL
                    EvaluatorHandle handle = ruleBuilder.getConditionManager().addCondition(new FailingPredicate(), mp.args());

                    collector.scheduleConditionMethodUpdate(handle, condition);
                }
            }

            // 2.5 Set a dummy RHS action that will be replaced for each new session
            if(ruleMethod.rhs.isStatic) {
                // Static RHS action can be defined right now
                lhsBuilder.execute(ruleMethod.rhs);
            } else {
                // Virtual RHS will require a class instance. Setting a dummy action and scheduling its update
                // for each new session
                lhsBuilder.execute(new FailingRhs());
                collector.scheduleRhsMethodUpdate(ruleBuilder.getName(), ruleMethod.rhs);
            }

        }
    }

    private static class FailingRhs implements Consumer<RhsContext> {
        @Override
        public void accept(RhsContext rhsContext) {
            throw new UnsupportedOperationException("RHS action not updated. Please report it as a bug.");
        }
    }

    private static class FailingPredicate implements ValuesPredicate {

        @Override
        public boolean test(IntToValue t) {
            throw new UnsupportedOperationException("LHS condition not updated. Please report it as a bug.");
        }

        @Override
        public String toString() {
            return "FailingPredicate{" +
                    "hash=" + System.identityHashCode(this) +
                    '}';
        }
    }
}
