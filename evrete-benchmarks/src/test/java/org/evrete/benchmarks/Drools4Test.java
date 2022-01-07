package org.evrete.benchmarks;

import org.evrete.KnowledgeService;
import org.evrete.api.ActivationManager;
import org.evrete.api.Knowledge;
import org.evrete.api.RuntimeRule;
import org.evrete.api.StatefulSession;
import org.evrete.benchmarks.helper.SessionWrapper;
import org.evrete.benchmarks.helper.TestUtils;
import org.evrete.benchmarks.models.misc.TypeA;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

class Drools4Test {
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
        AtomicInteger rhsCounter = new AtomicInteger();
        Knowledge knowledge = service
                .newKnowledge()
                .activationManager(ActivationGroupManager.class)
                .newRule("Rule 1")
                .salience(20)
                .forEach("$a", TypeA.class)
                .where("$a.l < 5")
                .execute(ctx -> rhsCounter.incrementAndGet())
                .newRule("Rule 2")
                .salience(100)
                .forEach("$a", TypeA.class)
                .where("$a.i > 5")
                .execute(ctx -> {
                    TypeA a = ctx.get("$a");
                    ctx.delete(a);
                    rhsCounter.incrementAndGet();
                })
                .newRule("Rule 3")
                .salience(10)
                .forEach("$a", TypeA.class)
                .execute(ctx -> rhsCounter.incrementAndGet())
                .newRule("Rule 4")
                .salience(1000)
                .forEach("$a", TypeA.class)
                .execute(ctx -> rhsCounter.incrementAndGet());


        KieContainer droolsKnowledge = TestUtils.droolsKnowledge("src/test/drl/droolsTest4_1.drl");
        KieSession dSession = droolsKnowledge.newKieSession();
        StatefulSession eSession = knowledge.newStatefulSession();

        eSession.setActivationManagerFactory(ActivationGroupManager.class.getName());
        assert eSession.getActivationManagerFactory().equals(ActivationGroupManager.class);

        SessionWrapper s1 = SessionWrapper.of(eSession);
        SessionWrapper s2 = SessionWrapper.of(dSession);

        for (int i = 0; i < 10; i++) {
            TypeA a1 = new TypeA("A" + i);
            a1.setAllNumeric(i);
            s1.insert(a1);

            TypeA a2 = new TypeA("A" + i);
            a2.setAllNumeric(i);
            s2.insert(a2);

        }

        s2.fire();
        s1.fire();

        Drools4Rhs.assertCount(rhsCounter.get());
    }

    public static class ActivationGroupManager implements ActivationManager {
        @Override
        public void onAgenda(int sequenceId, List<RuntimeRule> agenda) {
        }
    }
}
