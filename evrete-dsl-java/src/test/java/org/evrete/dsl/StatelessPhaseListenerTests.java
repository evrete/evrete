package org.evrete.dsl;

import org.evrete.KnowledgeService;
import org.evrete.api.ActivationMode;
import org.evrete.api.Knowledge;
import org.evrete.api.StatelessSession;
import org.evrete.dsl.rules.PhaseListenerRuleSet1;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class StatelessPhaseListenerTests extends CommonTestMethods {
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
    void test1(ActivationMode mode) {
        TestUtils.PhaseHelperData.reset();
        Knowledge knowledge = applyToRuntimeAsStream(service, PhaseListenerRuleSet1.class);
        assert TestUtils.PhaseHelperData.total() == 1 && TestUtils.PhaseHelperData.count(Phase.BUILD) == 1;

        StatelessSession session = session(knowledge, mode);
        assert TestUtils.PhaseHelperData.count(Phase.CREATE) == 3 : "Actual: " + TestUtils.PhaseHelperData.EVENTS;
        assert TestUtils.PhaseHelperData.total() == 6; // 4 + additional two coming from the multiple() method
        TestUtils.PhaseHelperData.reset();
        session.insert(1);
        session.fire();
        assert TestUtils.PhaseHelperData.total() == 8 : " " + TestUtils.PhaseHelperData.EVENTS;
    }
}
