package org.evrete.spi.minimal;

import org.evrete.Configuration;
import org.evrete.KnowledgeService;
import org.evrete.api.*;
import org.evrete.classes.TypeA;
import org.evrete.classes.TypeB;
import org.evrete.classes.TypeC;
import org.evrete.classes.TypeD;
import org.evrete.runtime.KnowledgeRuntime;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.evrete.Configuration.CONDITION_BASE_CLASS;


class EvaluatorCompilerTest {
    private static final String CONDITION = "$a.i == $b.i";
    private static KnowledgeService service;
    private KnowledgeRuntime knowledge;

    @BeforeAll
    static void setUpClass() {
        service = new KnowledgeService(new Configuration());
    }

    @AfterAll
    static void shutDownClass() {
        service.shutdown();
    }

    @BeforeEach
    void init() {
        knowledge = (KnowledgeRuntime) service.newKnowledge();
    }

    @Test
    void testConditionBaseClass1() {

        RuleBuilder<Knowledge> ruleBuilder = knowledge.newRule();

        LhsBuilder<Knowledge> lhsBuilder = ruleBuilder
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class
                );

        EvaluatorHandle betaHandle = ruleBuilder.createCondition(CONDITION);
        CompiledEvaluator condition = (CompiledEvaluator) knowledge.getEvaluator(betaHandle);
        String source = condition.getJavaSource();

        assert source.contains("extends " + BaseConditionClass.class.getName());

        lhsBuilder.where(betaHandle);
        testRhs(lhsBuilder);
    }

    @Test
    void testConditionBaseClass2() {

        RuleBuilder<Knowledge> ruleBuilder = knowledge.newRule();

        LhsBuilder<Knowledge> lhsBuilder = ruleBuilder
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class
                );

        Class<?>[] baseClasses = new Class[]{
                TypeC.class,
                TypeD.class
        };

        EvaluatorHandle[] handles = new EvaluatorHandle[baseClasses.length];
        for (int i = 0; i < baseClasses.length; i++) {
            Class<?> baseClass = baseClasses[i];
            knowledge.getConfiguration().setProperty(CONDITION_BASE_CLASS, baseClass.getName());
            EvaluatorHandle betaHandle = ruleBuilder.createCondition(CONDITION);

            handles[i] = betaHandle;

            CompiledEvaluator condition = (CompiledEvaluator) knowledge.getEvaluator(betaHandle);
            String source = condition.getJavaSource();
            assert source.contains("extends " + baseClass.getName());

        }
        lhsBuilder.where(handles);


        testRhs(lhsBuilder);
    }

    private void testRhs(LhsBuilder<Knowledge> lhsBuilder) {
        AtomicInteger counter = new AtomicInteger();
        Knowledge k = lhsBuilder.execute(ctx -> {
            counter.incrementAndGet();
            Object a = ctx.get("$a");
            Object b = ctx.get("$b");

            assert a instanceof TypeA;
            assert b instanceof TypeB;
        });

        try (StatefulSession s = k.newStatefulSession()) {
            for (int i = 0; i < 10; i++) {
                s.insert(new TypeA(i));
                s.insert(new TypeB(i));
            }
            s.fire();

            assert counter.get() == 10;
        }
    }
}