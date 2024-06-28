package org.evrete.dsl;

import org.evrete.KnowledgeService;
import org.evrete.api.ActivationMode;
import org.evrete.api.Knowledge;
import org.evrete.api.StatelessSession;
import org.evrete.api.events.EnvironmentChangeEvent;
import org.evrete.api.events.SessionCreatedEvent;
import org.evrete.api.events.SessionFireEvent;
import org.evrete.dsl.rules.PhaseListenerRuleSet1;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;

class StatelessPhaseListenerTests {
    private static KnowledgeService service;

    @BeforeAll
    static void setUpClass() {
        service = new KnowledgeService();
    }

    @AfterAll
    static void shutDownClass() {
        service.shutdown();
    }

    private static StatelessSession session(Knowledge knowledge, ActivationMode mode) {
        return knowledge.newStatelessSession(mode);
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void test1(ActivationMode mode) throws IOException {
        TestUtils.PhaseHelperData.reset();
        Knowledge knowledge = service.newKnowledge()
                .builder()
                .importRules(Constants.PROVIDER_JAVA_CLASS, PhaseListenerRuleSet1.class)
                .build();
        knowledge.set("some property", "some value");
        assert TestUtils.PhaseHelperData.count(EnvironmentChangeEvent.class) == 1;

        StatelessSession session = session(knowledge, mode);
        assert TestUtils.PhaseHelperData.count(SessionCreatedEvent.class) == 1;
        TestUtils.PhaseHelperData.reset();
        session.insert(1);
        session.fire();
        assert TestUtils.PhaseHelperData.count(SessionFireEvent.class) == 1;
    }
}
