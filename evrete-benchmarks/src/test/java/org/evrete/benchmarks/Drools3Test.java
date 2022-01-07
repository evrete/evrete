package org.evrete.benchmarks;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.StatefulSession;
import org.evrete.benchmarks.helper.SessionWrapper;
import org.evrete.benchmarks.helper.TestUtils;
import org.evrete.benchmarks.models.misc.TypeA;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import java.util.concurrent.atomic.AtomicInteger;

class Drools3Test {
    private static KnowledgeService service;

    @BeforeAll
    static void setUpClass() {
        service = new KnowledgeService();
    }

    @AfterAll
    static void shutDownClass() {
        service.shutdown();
    }

    @Test
    void test1() {
        Drools3Rhs.reset();
        AtomicInteger rhsCounter = new AtomicInteger();
        Knowledge knowledge = service
                .newKnowledge()
                .newRule("Rule 1")
                .salience(100)
                .forEach("$a", TypeA.class)
                .where("$a.l > 5")
                .execute(ctx -> rhsCounter.incrementAndGet())
                .newRule("Rule 2")
                .salience(1000)
                .forEach("$a", TypeA.class)
                .where("$a.i > 5")
                .execute(ctx -> {
                    TypeA a = ctx.get("$a");
                    a.setI(a.getI() - 1);
                    ctx.update(a);
                    rhsCounter.incrementAndGet();
                })
                .newRule("Rule 3")
                .salience(10)
                .forEach("$a", TypeA.class)
                .execute(ctx -> rhsCounter.incrementAndGet());


        KieContainer droolsKnowledge = TestUtils.droolsKnowledge("src/test/drl/droolsTest3_1.drl");
        KieSession dSession = droolsKnowledge.newKieSession();
        StatefulSession eSession = knowledge.newStatefulSession();

        SessionWrapper s1 = SessionWrapper.of(eSession);
        SessionWrapper s2 = SessionWrapper.of(dSession);


        TypeA a1 = new TypeA("A0");
        a1.setI(10);
        a1.setL(10);
        s1.insertAndFire(a1);

        TypeA a2 = new TypeA("A0");
        a2.setI(10);
        a2.setL(10);
        s2.insertAndFire(a2);


        Drools3Rhs.assertCount(rhsCounter.get());
        assert rhsCounter.get() == 7 : "Actual: " + rhsCounter.get();


    }

    @Test
    void test2() {
        Drools3Rhs.reset();
        AtomicInteger rhsCounter = new AtomicInteger();
        Knowledge knowledge = service
                .newKnowledge()
                .newRule("Rule 1")
                .salience(1000)
                .forEach("$a", TypeA.class)
                .where("$a.l > 5")
                .execute(ctx -> rhsCounter.incrementAndGet())
                .newRule("Rule 2")
                .salience(100)
                .forEach("$a", TypeA.class)
                .where("$a.i > 5")
                .execute(ctx -> {
                    TypeA a = ctx.get("$a");
                    a.setI(a.getI() - 1);
                    ctx.update(a);
                    rhsCounter.incrementAndGet();
                })
                .newRule("Rule 3")
                .salience(10)
                .forEach("$a", TypeA.class)
                .execute(ctx -> rhsCounter.incrementAndGet());


        KieContainer droolsKnowledge = TestUtils.droolsKnowledge("src/test/drl/droolsTest3_2.drl");
        KieSession dSession = droolsKnowledge.newKieSession();
        StatefulSession eSession = knowledge.newStatefulSession();

        SessionWrapper s1 = SessionWrapper.of(eSession);
        SessionWrapper s2 = SessionWrapper.of(dSession);


        TypeA a1 = new TypeA("A0");
        a1.setI(10);
        a1.setL(10);
        s1.insertAndFire(a1);

        TypeA a2 = new TypeA("A0");
        a2.setI(10);
        a2.setL(10);
        s2.insertAndFire(a2);


        Drools3Rhs.assertCount(rhsCounter.get());
        assert rhsCounter.get() == 12 : "Actual: " + rhsCounter.get();


    }
}
