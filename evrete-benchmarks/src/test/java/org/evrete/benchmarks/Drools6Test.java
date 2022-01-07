package org.evrete.benchmarks;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.StatefulSession;
import org.evrete.benchmarks.helper.TestUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

class Drools6Test {
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
        List<String> eLogs = new ArrayList<>();
        Knowledge knowledge = service
                .newKnowledge()
                .newRule("First")
                .forEach(
                        "$timer", AtomicLong.class,
                        "$number", AtomicInteger.class
                )
                .where("$timer.get % 2 == 0")
                .where("$number.get < $timer.get")
                .execute(ctx -> {
                    AtomicInteger $number = ctx.get("$number");
                    $number.incrementAndGet();
                    ctx.update($number);
                    eLogs.add("First" + $number.get());
                })
                .newRule("Second")
                .forEach(
                        "$timer", AtomicLong.class,
                        "$number", AtomicInteger.class
                )
                .where("$timer.get % 3 == 0")
                .where("$number.get < $timer.get")
                .execute(ctx -> {
                    AtomicInteger $number = ctx.get("$number");
                    $number.incrementAndGet();
                    ctx.update($number);
                    eLogs.add("Second" + $number.get());
                });


        //StatefulSession eSession = knowledge.newStatefulSession();

        //SessionWrapper s1 = SessionWrapper.of(eSession);
        //SessionWrapper s2 = SessionWrapper.of(dSession);

        // Drools
        KieContainer droolsKnowledge = TestUtils.droolsKnowledge("src/test/drl/droolsTest6.drl");
        KieSession dSession = droolsKnowledge.newKieSession();
        AtomicLong $timer1 = new AtomicLong(0);
        AtomicInteger $number1 = new AtomicInteger(0);
        FactHandle dTimerHandle = dSession.insert($timer1);
        dSession.insert($number1);
        dSession.fireAllRules();
        Drools4Rhs.reset();
        // Evrete
        AtomicLong $timer2 = new AtomicLong(0);
        AtomicInteger $number2 = new AtomicInteger(0);
        StatefulSession eSession = knowledge.newStatefulSession();

        eSession.insert($number2);
        org.evrete.api.FactHandle eTimerHandle = eSession.insert($timer2);
        eSession.fire();

        for (int i = 0; i < 64; i++) {
            $timer1.incrementAndGet();
            dSession.update(dTimerHandle, $timer1);
            dSession.fireAllRules();
            $timer2.incrementAndGet();
            eSession.update(eTimerHandle, $timer2);
            eSession.fire();
        }

        List<String> dLogs = Drools4Rhs.getData();

        assert dLogs.size() == eLogs.size() : dLogs.size() + " vs " + eLogs.size();
        for (int i = 0; i < eLogs.size(); i++) {
            assert dLogs.get(i).equals(eLogs.get(i));
        }
    }
}
