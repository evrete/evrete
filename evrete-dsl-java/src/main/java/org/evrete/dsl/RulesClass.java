package org.evrete.dsl;

import org.evrete.Configuration;
import org.evrete.api.*;
import org.evrete.api.builders.LhsBuilder;
import org.evrete.api.builders.RuleBuilder;
import org.evrete.api.builders.RuleSetBuilder;
import org.evrete.dsl.annotation.EventSubscription;
import org.evrete.dsl.annotation.FieldDeclaration;
import org.evrete.dsl.annotation.MethodPredicate;
import org.evrete.dsl.annotation.Rule;

import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import static org.evrete.dsl.AbstractDSLProvider.PROP_EXTEND_RULE_CLASSES;

class RulesClass extends WrappedClass {

    final List<RuleMethod> ruleMethods = new LinkedList<>();
    final List<WrappedFieldDeclarationMethod<?,?>> fieldDeclarationMethods = new LinkedList<>();
    final List<WrappedEventSubscriptionMethod<?>> subscriptionMethods = new LinkedList<>();

    RulesClass(WrappedClass other) {
        super(other);

        for (Method m : this.publicMethods) {
            Rule ruleAnnotation = m.getAnnotation(Rule.class);
            FieldDeclaration fieldDeclaration = m.getAnnotation(FieldDeclaration.class);
            EventSubscription eventSubscription = m.getAnnotation(EventSubscription.class);
            // The annotations above are mutually exclusive.
            if (ruleAnnotation != null) {
                this.ruleMethods.add(new RuleMethod(this, m, ruleAnnotation));
            } else if (fieldDeclaration != null) {
                this.fieldDeclarationMethods.add(new WrappedFieldDeclarationMethod<>(this, m, fieldDeclaration));
            } else if (eventSubscription != null) {
                this.subscriptionMethods.add(new WrappedEventSubscriptionMethod<>(this, m, eventSubscription.async()));
            }
        }

        // Sort the rules
        org.evrete.dsl.annotation.RuleSet.Sort defaultSort = Utils.deriveSort(delegate);
        ruleMethods.sort(new RuleComparator(defaultSort));

    }


    final void collectMetaData(RuntimeContext<?> context, MetadataCollector collector) {
        // 1. Field declaration
        for (WrappedFieldDeclarationMethod<?,?> m : fieldDeclarationMethods) {
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
        for (WrappedEventSubscriptionMethod<?> m : subscriptionMethods) {
            if(m.isStatic) {
                m.selfSubscribe(context);
            } else {
                collector.addEventSubscriptionMethod(m);
            }
        }

    }

    void applyTo(RuleSetBuilder<?> target, MetadataCollector collector) {

        boolean extendClassFlag = Boolean.parseBoolean(target.getContext().getConfiguration().getProperty(PROP_EXTEND_RULE_CLASSES, "true"));

        // Read and define each rule
        for(RuleMethod ruleMethod : ruleMethods) {
            // 2.1 Rule name & salience
            RuleBuilder<?> ruleBuilder = target.newRule(ruleMethod.getRuleName());

            if(extendClassFlag) {
                ruleBuilder.set(Configuration.RULE_BASE_CLASS, delegate.getCanonicalName());
            }

            int salience = ruleMethod.getSalience();
            if(salience != Rule.DEFAULT_SALIENCE) {
                ruleBuilder.salience(salience);
            }

            LhsBuilder<?> lhsBuilder = ruleBuilder.getLhs();
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
                WrappedMethod conditionMethod = lookup(methodName, methodType);
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
