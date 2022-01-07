package org.evrete.benchmarks;

import org.evrete.KnowledgeService;
import org.evrete.benchmarks.helper.SessionWrapper;
import org.evrete.benchmarks.helper.TestUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import java.util.Collection;

class Drools5PrimaryTest {
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


        KieContainer droolsKnowledge = TestUtils.droolsKnowledge("src/test/drl/droolsTest5.drl");
        KieSession dSession = droolsKnowledge.newKieSession();

        SessionWrapper s2 = SessionWrapper.of(dSession);

        for (int i = 2; i < 100; i++) {
            s2.insert(i);
        }


        s2.fire();

        Collection<Object> primary = s2.getMemoryObjects();
        assert primary.size() == 25;

    }
}
