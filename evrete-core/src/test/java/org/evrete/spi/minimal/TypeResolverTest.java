package org.evrete.spi.minimal;

import org.evrete.Configuration;
import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.StatefulSession;
import org.evrete.api.Type;
import org.evrete.helper.RhsAssert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

class TypeResolverTest {
    private static KnowledgeService service;

    @BeforeAll
    static void setUpClass() {
        service = new KnowledgeService(new Configuration());
    }

    @AfterAll
    static void shutDownClass() {
        service.shutdown();
    }

    @Test
    void testInheritance1() {
        RhsAssert rhsAssert = new RhsAssert("$s", StatefulSession.class);
        Knowledge knowledge = service
                .newKnowledge()
                .builder()
                .newRule()
                .forEach("$s", StatefulSession.class)
                .execute(rhsAssert)
                .build();

        StatefulSession session = knowledge.newStatefulSession();
        session.insertAndFire(session);
        rhsAssert.assertCount(1).assertContains("$s", session);
    }

    @Test
    void testInheritance2() {
        RhsAssert rhsAssert = new RhsAssert("$s", StatefulSession.class);

        Knowledge knowledge = service

                .newKnowledge()
                .configureTypes(typeResolver -> {
                    Type<StatefulSession> sessionType = typeResolver.declare(StatefulSession.class);
                    sessionType.declareBooleanField("hasTestRule", s -> s.getRule("Test") != null);
                })
                .builder()
                .newRule("Test")
                .forEach("$s", StatefulSession.class)
                .where("$s.hasTestRule")
                .execute(rhsAssert)
                .build();

        StatefulSession session = knowledge.newStatefulSession();
        session.insertAndFire(session);
        rhsAssert.assertCount(1).assertContains("$s", session);
    }

    @Test
    void testInheritance3() {
        Set<Integer> assertSet = new HashSet<>();
        Knowledge knowledge = service
                .newKnowledge()
                .configureTypes(typeResolver -> {
                    Type<StatefulSession> sessionType = typeResolver.declare(StatefulSession.class);
                    sessionType.declareBooleanField("hasTestRule", s -> s.getRule("Test") != null);
                })

                .builder()
                .newRule()
                .forEach("$s", StatefulSession.class)
                .where("$s.hasTestRule == false")
                .execute(ctx -> {
                    StatefulSession session = ctx.get("$s");
                    //noinspection resource
                    session
                            .builder()
                            .newRule("Test")
                            .forEach("$i", Integer.class)
                            .where("$i > 5")
                            .execute(context -> {
                                Integer i = context.get("$i");
                                assertSet.add(i);
                            })
                            .build();
                    ctx.update(session);
                })
                .build();

        StatefulSession session = knowledge.newStatefulSession();
        session.insertAndFire(session);

        session.insertAndFire(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assert assertSet.size() == 5 && assertSet.containsAll(Arrays.asList(6, 7, 8, 9, 10));
    }
}
