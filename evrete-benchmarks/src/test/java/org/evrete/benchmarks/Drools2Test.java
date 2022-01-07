package org.evrete.benchmarks;

import org.evrete.KnowledgeService;
import org.evrete.api.ActivationManager;
import org.evrete.api.Knowledge;
import org.evrete.api.RuntimeRule;
import org.evrete.api.StatefulSession;
import org.evrete.benchmarks.helper.SessionWrapper;
import org.evrete.benchmarks.helper.TestUtils;
import org.evrete.benchmarks.models.misc.TypeA;
import org.evrete.util.RhsAssert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

class Drools2Test {
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
        Knowledge knowledge = service
                .newKnowledge()
                .activationManager(ActivationGroupManager.class)
                .newRule("Rule 1")
                .salience(20)
                .property("ACTIVATION-GROUP", "AG1")
                .forEach("$a", TypeA.class)
                .where("$a.i > 5")
                .execute(ctx -> {
                    TypeA a = ctx.get("$a");
                    a.setI(a.getI() - 1);
                    ctx.update(a);
                })
                .newRule("Rule 2")
                .salience(100)
                .property("ACTIVATION-GROUP", "AG1")
                .forEach("$a", TypeA.class)
                .where("$a.i > 5")
                .execute(ctx -> {
                    TypeA a = ctx.get("$a");
                    a.setI(a.getI() - 1);
                    ctx.update(a);
                });


        KieContainer droolsKnowledge = TestUtils.droolsKnowledge("src/test/drl/droolsTest2_1.drl");
        KieSession dSession = droolsKnowledge.newKieSession();
        StatefulSession eSession = knowledge.newStatefulSession();

        eSession.setActivationManagerFactory(ActivationGroupManager.class);

        RhsAssert rhsAssert1 = new RhsAssert(eSession, "Rule 1");
        RhsAssert rhsAssert2 = new RhsAssert(eSession, "Rule 2");

        SessionWrapper s1 = SessionWrapper.of(eSession);
        SessionWrapper s2 = SessionWrapper.of(dSession);

        TypeA a1 = new TypeA("A0");
        a1.setI(10);
        s1.insertAndFire(a1);

        TypeA a2 = new TypeA("A0");
        a2.setI(10);
        s2.insertAndFire(a2);

        rhsAssert1.assertCount(0);
        Drools2Rhs.assertCount(rhsAssert2.getCount());

    }

    public static class ActivationGroupManager implements ActivationManager {
        private final Set<String> activatedGroups = new HashSet<>();

        static String getActivationGroup(RuntimeRule rule) {
            return rule.get("ACTIVATION-GROUP");
        }

        @Override
        public void onAgenda(int sequenceId, List<RuntimeRule> agenda) {
            activatedGroups.clear();
        }

        @Override
        public boolean test(RuntimeRule rule) {
            return !activatedGroups.contains(getActivationGroup(rule));
        }

        @Override
        public void onActivation(RuntimeRule rule, long count) {
            String activationGroup = getActivationGroup(rule);
            if (activationGroup != null) {
                activatedGroups.add(activationGroup);
            }
        }
    }
}
