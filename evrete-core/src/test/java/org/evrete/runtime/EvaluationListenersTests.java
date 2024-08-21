package org.evrete.runtime;

import org.evrete.KnowledgeService;
import org.evrete.api.*;
import org.evrete.api.builders.RuleBuilder;
import org.evrete.api.builders.RuleSetBuilder;
import org.evrete.api.events.ConditionEvaluationEvent;
import org.evrete.api.events.Events;
import org.evrete.classes.TypeA;
import org.evrete.classes.TypeB;
import org.evrete.helper.RhsAssert;
import org.evrete.helper.TestUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

class EvaluationListenersTests {
    private static final boolean[] ASYNC_VALUES = new boolean[]{false, true};

    private static KnowledgeService service;

    @BeforeAll
    static void setUpClass() {
        service = new KnowledgeService();
    }

    @AfterAll
    static void shutDownClass() {
        service.shutdown();
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void alphaConditionValues(ActivationMode mode) {
        final Events.Subscriptions sink = new Events.Subscriptions();

        for (boolean async : ASYNC_VALUES) {
            RuleSetBuilder<Knowledge> builder = service.newKnowledge()
                    .builder();
            RuleBuilder<Knowledge> ruleBuilder = builder.newRule();

            ruleBuilder
                    .forEach("$n", Integer.class)
                    .execute(ctx -> {});

            ValuesPredicate alphaCondition = t -> {
                TestUtils.sleep(100);
                return t.get(0, int.class) > 0;
            };

            EvaluatorHandle handle = ruleBuilder.getConditionManager()
                    .addCondition(alphaCondition, "$n.intValue");

            Knowledge knowledge = builder.build();


            Set<RuleSession<?>> contextSessions = Collections.synchronizedSet(new HashSet<>());

            knowledge.getEvaluatorsContext().publisher(handle).subscribe(async, event -> {
                assert event.getCondition() == alphaCondition;
                Object[] args = event.getArguments();
                assert args.length == 1;

                int arg = (int) args[0];
                if(event.isPassed()) {
                    assert arg == 1;
                } else {
                    assert arg == -1;
                }

                contextSessions.add(event.getContext());

                // Test times
                Instant start = event.getStartTime();
                Instant end = event.getEndTime();
                assert end.isAfter(start) : "Start: " + start + " End: " + end + ", Async: " + async;

            });

            try (StatefulSession session = knowledge.newStatefulSession().setActivationMode(mode)) {
                session.insertAndFire(-1, 1);
                if (async) {
                    TestUtils.sleep(500);
                }


                assert contextSessions.contains(session);
            }
        }


        sink.cancel();
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void alphaCondition(ActivationMode mode) {
        final Events.Subscriptions sink = new Events.Subscriptions();

        for (boolean async : ASYNC_VALUES) {
            AtomicInteger knowledgeListenerCounter = new AtomicInteger();
            AtomicInteger sessionListenerCounter = new AtomicInteger();
            RhsAssert rhsAssert = new RhsAssert("$n", Integer.class);
            Knowledge knowledge = service.newKnowledge()
                    .builder()
                    .newRule()
                    .forEach("$n", Integer.class)
                    .where("$n.intValue > 1")
                    .execute(rhsAssert)
                    .build();


            subscribeToAll(
                    knowledge,
                    async,
                    sink,
                    event -> knowledgeListenerCounter.incrementAndGet()
            );


            try (StatefulSession session = knowledge.newStatefulSession().setActivationMode(mode)) {
                subscribeToAll(
                        session,
                        async,
                        sink,
                        event -> sessionListenerCounter.incrementAndGet()
                );


                session.insertAndFire(1, 2, 3);
                rhsAssert.assertCount(2).reset();

                if (async) {
                    TestUtils.sleep(500);
                }
                Assertions.assertEquals(3, knowledgeListenerCounter.get());
                Assertions.assertEquals(knowledgeListenerCounter.get(), sessionListenerCounter.get());
            }
        }


        sink.cancel();
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void mixedAlphaBeta(ActivationMode mode) {
        final Events.Subscriptions sink = new Events.Subscriptions();

        for (boolean async : ASYNC_VALUES) {
            AtomicInteger knowledgeListenerCounter = new AtomicInteger(0);
            AtomicInteger sessionListenerCounter = new AtomicInteger(0);

            RhsAssert rhsAssert = new RhsAssert(
                    "$a", TypeA.class,
                    "$b", TypeB.class
            );

            Knowledge knowledge = service.newKnowledge()
                    .builder()
                    .newRule()
                    .forEach(
                            "$a", TypeA.class,
                            "$b", TypeB.class
                    )
                    .where("$a.i == $b.i")
                    .where("$a.i > 0")
                    .where("$b.i > 0")
                    .execute(rhsAssert)
                    .build();

            subscribeToAll(
                    knowledge,
                    async,
                    sink,
                    event -> knowledgeListenerCounter.incrementAndGet()
            );

            TypeB b1;
            TypeA a1_1;
            try (StatefulSession s = knowledge.newStatefulSession().setActivationMode(mode)) {
                subscribeToAll(
                        s,
                        async,
                        sink,
                        event -> sessionListenerCounter.incrementAndGet()
                );

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

                if (async) {
                    TestUtils.sleep(500);
                }

                int expected = 8 + 3; // 8 first fire (4 alpha + 4 beta)  + 3 second fire (1 alpha + 2 beta)
                assert sessionListenerCounter.get() == knowledgeListenerCounter.get() : "Actual " + sessionListenerCounter.get() + " vs " + knowledgeListenerCounter.get();
                assert sessionListenerCounter.get() == expected : "Actual " + sessionListenerCounter.get() + " vs expected " + expected;
            }

        }

        sink.cancel();
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void betaCondition(ActivationMode mode) {
        final Events.Subscriptions sink = new Events.Subscriptions();

        for (boolean async : ASYNC_VALUES) {
            AtomicInteger sessionListenerCounter = new AtomicInteger(0);

            RhsAssert rhsAssert = new RhsAssert(
                    "$a", TypeA.class,
                    "$b", TypeB.class
            );

            Knowledge knowledge = service.newKnowledge()
                    .builder()
                    .newRule()
                    .forEach(
                            "$a", TypeA.class,
                            "$b", TypeB.class
                    )
                    .where("$a.i == $b.i")
                    .execute(rhsAssert)
                    .build();

            int mod;
            try (StatefulSession s = knowledge.newStatefulSession().setActivationMode(mode)) {
                subscribeToAll(
                        s,
                        async,
                        sink,
                        event -> sessionListenerCounter.incrementAndGet()
                );

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

                if (async) {
                    TestUtils.sleep(500);
                }
                assert sessionListenerCounter.get() == mod * mod;
            }
        }

        sink.cancel();
    }

    private static void subscribeToAll(RuntimeContext<?> context, boolean async, Events.Subscriptions sink, Consumer<ConditionEvaluationEvent> consumer) {
        EvaluatorsContext ec = context.getEvaluatorsContext();
        ec.forEach((handle, evaluator) -> ec.publisher(handle).subscribe(sink, async, consumer));
    }
}
