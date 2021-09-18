package org.evrete.dsl;

import org.evrete.api.*;
import org.evrete.dsl.annotation.MethodPredicate;
import org.evrete.dsl.annotation.RuleSet;
import org.evrete.util.KnowledgeWrapper;

import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

class DSLKnowledge extends KnowledgeWrapper {
    private final Constructor<?> constructor;
    private final RulesetMeta meta;
    private final List<DSLRule> rules = new ArrayList<>();

    DSLKnowledge(Knowledge delegate, RulesetMeta meta) {
        super(delegate);
        this.meta = meta;
        try {
            this.constructor = meta.javaClass.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new MalformedResourceException("Unable to locate a zero-arg public constructor in the " + meta.javaClass, e);
        }

        // Field declarations must be applied before any rules. At this stage, we'll be using a dummy
        // "not supported" declaration so that rule builders would be able to resolve fields.
        FieldDeclarations fieldDeclarations = meta.fieldDeclarations;
        fieldDeclarations.applyInitial(getTypeResolver());


        // Building rules
        RuleSet.Sort defaultSort = Utils.deriveSort(meta.javaClass);
        meta.ruleMethods.sort(new RuleComparator(defaultSort));
        for (RuleMethod rm : meta.ruleMethods) {
            RuleBuilder<Knowledge> builder = this
                    .newRule(rm.getRuleName());

            if (rm.getSalience() != Integer.MIN_VALUE) {
                builder.salience(rm.getSalience());
            }

            // Creating facts
            LhsBuilder<Knowledge> lhs = builder.getLhs();
            for (RuleMethod.FactDeclaration p : rm.factDeclarations) {
                lhs.addFactDeclaration(p.name, Utils.box(p.type));
            }

            // Adding literal conditions
            lhs = lhs.where(rm.stringPredicates);
            // Adding method predicates
            List<PredicateMethod> predicateMethods = new LinkedList<>();
            for (MethodPredicate mp : rm.methodPredicates) {
                String methodName = mp.method();
                final FieldReference[] descriptor = getExpressionResolver().resolve(lhs, mp.descriptor());
                Class<?>[] signature = Utils.asMethodSignature(descriptor);
                MethodType methodType = MethodType.methodType(boolean.class, signature);
                ClassMethod mv = ClassMethod.lookup(meta.lookup, methodName, methodType);

                // Creating a dummy condition and necessary information to update that condition
                // upon session initialization
                EvaluatorHandle evaluatorHandle = this.addEvaluator(new DummyEvaluator(descriptor));
                lhs.where(evaluatorHandle);
                predicateMethods.add(new PredicateMethod(mv, descriptor, evaluatorHandle));
            }

            // Assigning dummy RHS too
            lhs.execute(c -> {
                throw new IllegalStateException();
            });
            rules.add(new DSLRule(rm, predicateMethods));
        }

        // There is one listener that should be called right now
        meta.listeners.fire(Phase.BUILD, this);
    }

    @Override
    public StatefulSession newStatefulSession() {
        return new DSLStatefulSession(super.newStatefulSession(), meta, meta.fieldDeclarations, rules, classInstance());
    }

    @Override
    public StatelessSession newStatelessSession() {
        return new DSLStatelessSession(super.newStatelessSession(), meta, meta.fieldDeclarations, rules, classInstance());
    }

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
