package org.evrete;

import org.evrete.api.*;
import org.evrete.api.builders.ConditionManager;
import org.evrete.api.builders.LhsBuilder;
import org.evrete.api.builders.RuleBuilder;
import org.evrete.classes.TypeA;
import org.evrete.classes.TypeB;
import org.evrete.helper.RhsAssert;
import org.evrete.spi.minimal.DefaultLiteralSourceCompiler;
import org.evrete.spi.minimal.DefaultMemoryFactoryProvider;
import org.evrete.spi.minimal.DefaultTypeResolverProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

class EvaluationContextTests {
    private static KnowledgeService service;
    private Knowledge knowledge;

    @BeforeAll
    static void setUpClass() {
        service = KnowledgeService.builder()
                .withMemoryFactoryProvider(DefaultMemoryFactoryProvider.class)
                .withTypeResolverProvider(DefaultTypeResolverProvider.class)
                .withLiteralSourceCompiler(DefaultLiteralSourceCompiler.class)
                .build();
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

        RuleBuilder<Knowledge> ruleBuilder = knowledge.builder().newRule();

        LhsBuilder<Knowledge> lhsBuilder = ruleBuilder
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class
                );


        ConditionManager manager = ruleBuilder.getConditionManager();
        CompletableFuture<EvaluatorHandle> betaHandleF = manager.addCondition("$a.i == $b.i");
        CompletableFuture<EvaluatorHandle> alphaHandle1F = manager.addCondition("$a.i > 1");
        CompletableFuture<EvaluatorHandle> alphaHandle2F = manager.addCondition("$b.i > 1");

        AtomicInteger counter = new AtomicInteger();
        lhsBuilder
                .execute(ctx -> counter.incrementAndGet()) // Do nothing
                .build();

        EvaluatorHandle betaHandle = betaHandleF.join();
        EvaluatorHandle alphaHandle1 = alphaHandle1F.join();
        EvaluatorHandle alphaHandle2 = alphaHandle2F.join();


        try (
                StatefulSession session1 = knowledge.newStatefulSession().setActivationMode(mode);
                StatefulSession session2 = knowledge.newStatefulSession().setActivationMode(mode)) {

            session1.insertAndFire(facts);
            Assertions.assertEquals(count - 2, counter.get());

            // Updating conditions for the second session
            ValuesPredicate betaPredicate = t -> {
                int ai = t.get(0);
                int bi = t.get(1);
                return ai != bi; // Inverse condition
            };

            ValuesPredicate alphaPredicate = t -> t.get(0, int.class) >= 0;

//            Evaluator betaNew = new EvaluatorOfPredicate(betaPredicate, 1.0, fieldReferences); // Becomes "$a.i != $b.i"
//            Evaluator alpha1New = new EvaluatorOfPredicate(alphaPredicate, 1.0,  fieldReferences[0]); // Becomes $a.i =>=0
//            Evaluator alpha2New = new EvaluatorOfPredicate(alphaPredicate, 1.0, fieldReferences[1]); // Becomes $b.i =>=0

            EvaluatorsContext evaluatorsContext = session2.getEvaluatorsContext();
            evaluatorsContext.replacePredicate(betaHandle, betaPredicate);
            evaluatorsContext.replacePredicate(alphaHandle1, alphaPredicate);
            evaluatorsContext.replacePredicate(alphaHandle2, alphaPredicate);

            session2.insertAndFire(facts);
            //rhsAssert.assertCount(count * (count - 1)).reset(); // n * (n - 1)
        }


    }
}
