package org.evrete;

import org.evrete.api.ActivationMode;
import org.evrete.api.Knowledge;
import org.evrete.api.StatefulSession;
import org.evrete.classes.TypeA;
import org.evrete.classes.TypeB;
import org.evrete.helper.RhsAssert;
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
        AtomicInteger knowledgeListenerCounter = new AtomicInteger(0);
        AtomicInteger sessionListenerCounter = new AtomicInteger(0);

        RhsAssert rhsAssert = new RhsAssert("$n", Integer.class);
        knowledge.newRule()
                .forEach("$n", Integer.class)
                .where("$n.intValue > 1")
                .execute(rhsAssert);

        knowledge.addListener((evaluator, values, result) -> knowledgeListenerCounter.incrementAndGet());

        StatefulSession session = knowledge.createSession().setActivationMode(mode);
        session.addListener((evaluator, values, result) -> sessionListenerCounter.incrementAndGet());
        session.insertAndFire(1, 2, 3);
        rhsAssert.assertCount(2).reset();
        assert knowledgeListenerCounter.get() == 3;
        assert knowledgeListenerCounter.get() == sessionListenerCounter.get();
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

        StatefulSession s = knowledge.createSession().setActivationMode(mode);
        s.addListener((evaluator, values, result) -> sessionListenerCounter.incrementAndGet());

        TypeA a1 = new TypeA("A1");
        a1.setAllNumeric(1);

        TypeA a2 = new TypeA("A2");
        a2.setAllNumeric(2);

        TypeB b1 = new TypeB("B1");
        b1.setAllNumeric(1);

        TypeB b2 = new TypeB("B2");
        b2.setAllNumeric(2);

        s.insertAndFire(a1, a2, b1, b2);
        rhsAssert.assertCount(2).reset();

        TypeA a1_1 = new TypeA("A1_1");
        a1_1.setAllNumeric(1);
        s.insertAndFire(a1_1);
        rhsAssert.assertCount(1);
        rhsAssert.assertContains("$a", a1_1);
        rhsAssert.assertContains("$b", b1);

        assert sessionListenerCounter.get() == 9 : "Actual: " + sessionListenerCounter.get();
        assert sessionListenerCounter.get() == knowledgeListenerCounter.get() : "Actual " + sessionListenerCounter.get() + " vs " + knowledgeListenerCounter.get();
    }
}
