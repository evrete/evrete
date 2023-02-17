package org.evrete.spi.minimal;

import org.evrete.Configuration;
import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.StatefulSession;
import org.evrete.api.Type;
import org.evrete.api.TypeResolver;
import org.evrete.util.RhsAssert;
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
                .newRule()
                .forEach("$s", StatefulSession.class)
                .execute(rhsAssert);

        StatefulSession session = knowledge.newStatefulSession();
        session.insertAndFire(session);
        rhsAssert.assertCount(1).assertContains("$s", session);
    }

    @Test
    void testInheritance2() {
        RhsAssert rhsAssert = new RhsAssert("$s", StatefulSession.class);
        TypeResolver typeResolver = service.newTypeResolver();
        Type<StatefulSession> sessionType = typeResolver.declare(StatefulSession.class);
        sessionType.declareBooleanField("hasTestRule", s -> s.getRule("Test") != null);

        Knowledge knowledge = service
                .newKnowledge(typeResolver)
                .newRule("Test")
                .forEach("$s", StatefulSession.class)
                .where("$s.hasTestRule")
                .execute(rhsAssert);

        StatefulSession session = knowledge.newStatefulSession();
        session.insertAndFire(session);
        rhsAssert.assertCount(1).assertContains("$s", session);
    }

    @Test
    void testInheritance3() {
        TypeResolver typeResolver = service.newTypeResolver();
        Type<StatefulSession> sessionType = typeResolver.declare(StatefulSession.class);
        sessionType.declareBooleanField("hasTestRule", s -> s.getRule("Test") != null);

        Set<Integer> assertSet = new HashSet<>();
        Knowledge knowledge = service
                .newKnowledge(typeResolver)
                .newRule()
                .forEach("$s", StatefulSession.class)
                .where("$s.hasTestRule == false")
                .execute(ctx -> {
                    StatefulSession session = ctx.get("$s");
                    //noinspection resource
                    session
                            .newRule("Test")
                            .forEach("$i", Integer.class)
                            .where("$i > 5")
                            .execute(context -> {
                                Integer i = context.get("$i");
                                assertSet.add(i);
                            });
                    ctx.update(session);
                });

        StatefulSession session = knowledge.newStatefulSession();
        session.insertAndFire(session);

        session.insertAndFire(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assert assertSet.size() == 5 && assertSet.containsAll(Arrays.asList(6, 7, 8, 9, 10));
    }
}