package org.evrete;

import org.evrete.api.*;
import org.evrete.classes.TypeA;
import org.evrete.classes.TypeB;
import org.evrete.util.NextIntSupplier;
import org.evrete.util.RhsAssert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.concurrent.atomic.AtomicInteger;

class EvaluationListenersTests {
    private static KnowledgeService service;
    private Knowledge knowledge;

    @BeforeAll
    static void setUpClass() {
        service = new KnowledgeService();
    }

    @AfterAll
    static void shutDownClass() {
        service.shutdown();
    }

    @BeforeEach
    void init() {
        knowledge = service.newKnowledge();
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void plainTest(ActivationMode mode) {
        NextIntSupplier knowledgeListenerCounter = new NextIntSupplier();
        NextIntSupplier sessionListenerCounter = new NextIntSupplier();

        RhsAssert rhsAssert = new RhsAssert("$n", Integer.class);
        knowledge.newRule()
                .forEach("$n", Integer.class)
                .where("$n.intValue > 1")
                .execute(rhsAssert);


        EvaluationListener kl = new EvaluationListener() {
            @Override
            public void fire(Evaluator evaluator, IntToValue values, boolean result) {
                knowledgeListenerCounter.next();
            }

            @Override
            public String toString() {
                return "KN-LISTENER";
            }
        };


        EvaluationListener sl = new EvaluationListener() {
            @Override
            public void fire(Evaluator evaluator, IntToValue values, boolean result) {
                sessionListenerCounter.next();
            }

            @Override
            public String toString() {
                return "SN-LISTENER";
            }
        };

        knowledge.addListener(kl);
        StatefulSession session = knowledge.newStatefulSession().setActivationMode(mode);
        session.addListener(sl);
        session.insertAndFire(1, 2, 3);
        rhsAssert.assertCount(2).reset();
        assert knowledgeListenerCounter.get() == 3 : "Actual: " + knowledgeListenerCounter.get();
        assert knowledgeListenerCounter.get() == sessionListenerCounter.get() : "Session count: " + sessionListenerCounter.get();
        session.close();
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void testAlphaBeta(ActivationMode mode) {
        AtomicInteger knowledgeListenerCounter = new AtomicInteger(0);
        AtomicInteger sessionListenerCounter = new AtomicInteger(0);

        RhsAssert rhsAssert = new RhsAssert(
                "$a", TypeA.class,
                "$b", TypeB.class
        );

        knowledge.newRule()
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class
                )
                .where("$a.i == $b.i")
                .where("$a.i > 0")
                .where("$b.i > 0")
                .execute(rhsAssert);

        knowledge.addListener((evaluator, values, result) -> knowledgeListenerCounter.incrementAndGet());

        TypeB b1;
        TypeA a1_1;
        try (StatefulSession s = knowledge.newStatefulSession().setActivationMode(mode)) {
            s.addListener((evaluator, values, result) -> sessionListenerCounter.incrementAndGet());

            TypeA a1 = new TypeA("A1");
            a1.setAllNumeric(1);

            TypeA a2 = new TypeA("A2");
            a2.setAllNumeric(2);

            b1 = new TypeB("B1");
            b1.setAllNumeric(1);

            TypeB b2 = new TypeB("B2");
            b2.setAllNumeric(2);

            s.insertAndFire(a1, a2, b1, b2);
            rhsAssert.assertCount(2).reset();

            a1_1 = new TypeA("A1_1");
            a1_1.setAllNumeric(1);
            s.insertAndFire(a1_1);

            rhsAssert.assertCount(1);
            rhsAssert.assertContains("$a", a1_1);
            rhsAssert.assertContains("$b", b1);

            int expected = 8 + 3; // 8 first fire (4 alpha + 4 beta)  + 3 second fire (1 alpha + 2 beta)
            assert sessionListenerCounter.get() == knowledgeListenerCounter.get() : "Actual " + sessionListenerCounter.get() + " vs " + knowledgeListenerCounter.get();
            assert sessionListenerCounter.get() == expected : "Actual " + sessionListenerCounter.get() + " vs expected " + expected;
        }
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void testBeta(ActivationMode mode) {
        AtomicInteger sessionListenerCounter = new AtomicInteger(0);

        RhsAssert rhsAssert = new RhsAssert(
                "$a", TypeA.class,
                "$b", TypeB.class
        );

        knowledge.newRule()
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class
                )
                .where("$a.i == $b.i")
                .execute(rhsAssert);


        int mod;
        try (StatefulSession s = knowledge.newStatefulSession().setActivationMode(mode)) {
            s.addListener((evaluator, values, result) -> sessionListenerCounter.incrementAndGet());

            mod = 4;

            for (int i = 0; i < 512; i++) {
                int val = i % mod;
                TypeA a = new TypeA("A" + i);
                a.setAllNumeric(val);
                TypeB b = new TypeB("B" + i);
                b.setAllNumeric(val);
                s.insert(a);
                s.insert(b);
            }
            s.fire();
            assert sessionListenerCounter.get() == mod * mod;
        }
    }
}
