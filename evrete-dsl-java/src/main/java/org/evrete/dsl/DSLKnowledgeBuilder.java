package org.evrete.dsl;

import org.evrete.api.*;
import org.evrete.api.builders.LhsBuilder;
import org.evrete.api.builders.RuleBuilder;
import org.evrete.api.builders.RuleSetBuilder;
import org.evrete.dsl.annotation.MethodPredicate;
import org.evrete.dsl.annotation.RuleSet;
import org.evrete.util.CommonUtils;

import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.evrete.Configuration.RULE_BASE_CLASS;

class DSLKnowledgeBuilder  {
    private final Constructor<?> constructor;
    private final RulesetMeta meta;
    private final List<DSLRule> rules = new ArrayList<>();

    DSLKnowledgeBuilder(RuleSetBuilder<Knowledge> rulesetBuilder, RulesetMeta meta) {
        this.meta = meta;
        try {
            this.constructor = meta.javaClass.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new MalformedResourceException("Unable to locate a zero-arg public constructor in the " + meta.javaClass, e);
        }

        Knowledge context = rulesetBuilder.getContext();

        // Field declarations must be applied before any rules. At this stage, we'll be using a dummy
        // "not supported" declaration so that rule builders would be able to resolve fields.
        FieldDeclarations fieldDeclarations = meta.fieldDeclarations;
        fieldDeclarations.applyInitial(context.getTypeResolver());

        // Building rules
        RuleSet.Sort defaultSort = Utils.deriveSort(meta.javaClass);
        meta.ruleMethods.sort(new RuleComparator(defaultSort));

        for (RuleMethod rm : meta.ruleMethods) {
            RuleBuilder<Knowledge> ruleBuilder = rulesetBuilder
                    .newRule(rm.getRuleName())
                    .set(RULE_BASE_CLASS, canonicalName(meta.javaClass));

            if (rm.getSalience() != Integer.MIN_VALUE) {
                ruleBuilder.salience(rm.getSalience());
            }

            // Creating facts
            LhsBuilder<Knowledge> lhs = ruleBuilder.getLhs();
            for (RuleMethod.FactDeclaration p : rm.factDeclarations) {
                Class<?> cl = Utils.box(p.javaType);
                if (p.namedType == null) {
                    lhs.addFactDeclaration(p.name, cl);
                } else {
                    lhs.addFactDeclaration(p.name, p.namedType);
                }
            }

            // Adding literal conditions
            lhs.where(rm.stringPredicates);

            // Adding method predicates
            List<PredicateMethod> predicateMethods = new LinkedList<>();
            for (MethodPredicate mp : rm.methodPredicates) {
                String methodName = mp.method();

                String[] args = mp.args();
                final FieldReference[] descriptor = CommonUtils.resolveFieldReferences(ruleBuilder, args);
                Class<?>[] signature = Utils.asMethodSignature(descriptor);
                MethodType methodType = MethodType.methodType(boolean.class, signature);
                ClassMethod mv = ClassMethod.lookup(meta.lookup, meta.javaClass, methodName, methodType);

                // Creating a dummy condition and necessary information to update that condition
                // upon session initialization
                EvaluatorHandle evaluatorHandle = context.addEvaluator(new DummyEvaluator(descriptor));
                lhs.where(evaluatorHandle);
                predicateMethods.add(new PredicateMethod(mv, descriptor, evaluatorHandle));
            }

            // Assigning dummy RHS to finalize the builder
            lhs.execute(c -> {
                throw new IllegalStateException();
            });
            rules.add(new DSLRule(rm, predicateMethods));
        }

        // There is one listener that should be called right now
        meta.phaseListeners.fire(Phase.BUILD, context);
    }


    //TODO remove
    private static String canonicalName(Class<?> cl) {
        try {
            return cl.getCanonicalName();
        } catch (Throwable t) {
            return cl.getName().replaceAll("\\$", ".");
        }
    }

/*
    @Override
    public StatefulSession newStatefulSession() {
        return new DSLStatefulSession(super.newStatefulSession(), meta, meta.fieldDeclarations, rules, classInstance());
    }

    @Override
    public StatelessSession newStatelessSession() {
        return new DSLStatelessSession(super.newStatelessSession(), meta, meta.fieldDeclarations, rules, classInstance());
    }

    @Override
    public Knowledge set(String property, Object value) {
        super.set(property, value);
        meta.envListeners.fire(property, value, true);
        return this;
    }
*/

    private Object classInstance() {
        try {
            return constructor.newInstance();
        } catch (Throwable t) {
            throw new MalformedResourceException("Unable to create instance of the " + meta.javaClass, t);
        }
    }

    private static class DummyEvaluator implements Evaluator {
        private final FieldReference[] descriptor;

        DummyEvaluator(FieldReference[] descriptor) {
            this.descriptor = descriptor;
        }

        @Override
        public FieldReference[] descriptor() {
            return descriptor;
        }

        @Override
        public boolean test(IntToValue t) {
            throw new IllegalStateException();
        }
    }
}