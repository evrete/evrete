package org.evrete;

import org.evrete.api.*;
import org.evrete.classes.TypeA;
import org.evrete.classes.TypeB;
import org.evrete.helper.RhsAssert;
import org.evrete.runtime.evaluation.EvaluatorOfPredicate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.LinkedList;
import java.util.List;

class EvaluationContextTests {
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
    void testAlphaBeta(ActivationMode mode) {

        RhsAssert rhsAssert = new RhsAssert(
                "$a", TypeA.class,
                "$b", TypeB.class
        );

        List<Object> facts = new LinkedList<>();
        int count = 5;
        for (int i = 0; i < count; i++) {
            TypeA a1 = new TypeA("A" + i);
            a1.setAllNumeric(i);

            TypeB b1 = new TypeB("B" + i);
            b1.setAllNumeric(i);

            facts.add(a1);
            facts.add(b1);
        }

        LhsBuilder<Knowledge> lhsBuilder = knowledge.newRule()
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class
                );

        EvaluatorHandle betaHandle = lhsBuilder.addWhere("$a.i == $b.i");
        EvaluatorHandle alphaHandle1 = lhsBuilder.addWhere("$a.i > 1");
        EvaluatorHandle alphaHandle2 = lhsBuilder.addWhere("$b.i > 1");

        lhsBuilder.execute(rhsAssert);

        StatefulSession session1 = knowledge.createSession().setActivationMode(mode);
        session1.insertAndFire(facts);
        rhsAssert.assertCount(count - 2).reset(); // With zero 'i' values excluded
        rhsAssert.reset();

        // Updating conditions for a new session
        StatefulSession session2 = knowledge.createSession().setActivationMode(mode);

        FieldReference[] fieldReferences = knowledge.getExpressionResolver().resolve(lhsBuilder, "$a.i", "$b.i");


        ValuesPredicate betaPredicate = t -> {
            int ai = t.get(0);
            int bi = t.get(1);
            return ai != bi; // Inverse condition
        };

        ValuesPredicate alphaPredicate = t -> {
            int i = t.get(0);
            return i >= 0;
        };

        Evaluator betaNew = new EvaluatorOfPredicate(betaPredicate, fieldReferences); // Becomes "$a.i != $b.i"
        Evaluator alpha1New = new EvaluatorOfPredicate(alphaPredicate, fieldReferences[0]); // Becomes $a.i =>=0
        Evaluator alpha2New = new EvaluatorOfPredicate(alphaPredicate, fieldReferences[1]); // Becomes $b.i =>=0

        session2.replaceEvaluator(betaHandle, betaNew);
        session2.replaceEvaluator(alphaHandle1, alpha1New);
        session2.replaceEvaluator(alphaHandle2, alpha2New);

        session2.insertAndFire(facts);
        rhsAssert.assertCount(count * (count - 1)).reset(); // n * (n - 1)


    }
}
