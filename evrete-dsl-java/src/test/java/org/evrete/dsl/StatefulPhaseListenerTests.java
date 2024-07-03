package org.evrete.dsl;

import org.evrete.KnowledgeService;
import org.evrete.api.ActivationMode;
import org.evrete.api.Knowledge;
import org.evrete.api.StatefulSession;
import org.evrete.api.events.EnvironmentChangeEvent;
import org.evrete.api.events.SessionClosedEvent;
import org.evrete.api.events.SessionFireEvent;
import org.evrete.dsl.rules.PhaseListenerRuleSet1;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;

class StatefulPhaseListenerTests {
    private static KnowledgeService service;

    @BeforeAll
    static void setUpClass() {
        service = new KnowledgeService();
    }

    @AfterAll
    static void shutDownClass() {
        service.shutdown();
    }

    private static StatefulSession session(Knowledge knowledge, ActivationMode mode) {
        return knowledge.newStatefulSession(mode);
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void test1(ActivationMode mode) throws IOException {
        TestUtils.PhaseHelperData.reset();
        Knowledge knowledge = service.newKnowledge()
                .importRules(Constants.PROVIDER_JAVA_CLASS, PhaseListenerRuleSet1.class);

        knowledge.set("some property", "some value 1");
        assert TestUtils.PhaseHelperData.count(EnvironmentChangeEvent.class) == 1;

        try(StatefulSession session = session(knowledge, mode)) {
            session.set("some property", "some value 2");
            assert TestUtils.PhaseHelperData.count(EnvironmentChangeEvent.class) == 2;
            session.insert(1);
            session.fire();
            assert TestUtils.PhaseHelperData.count(SessionFireEvent.class) == 1;
            session.fire();
            assert TestUtils.PhaseHelperData.count(SessionFireEvent.class) == 2;

        }
        assert TestUtils.PhaseHelperData.count(SessionClosedEvent.class) == 1;
    }
}
