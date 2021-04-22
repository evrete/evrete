package org.evrete.dsl;

import org.evrete.KnowledgeService;
import org.evrete.api.ActivationMode;
import org.evrete.api.Knowledge;
import org.evrete.api.StatefulSession;
import org.evrete.dsl.rules.ListenerRuleSet1;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ListenerTests extends CommonTestMethods {
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
        return knowledge.createSession().setActivationMode(mode);
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void test1(ActivationMode mode) {
        ListenerInvocationData.reset();
        Knowledge knowledge = applyToRuntimeAsStream(service, ListenerRuleSet1.class);
        assert ListenerInvocationData.total() == 1 && ListenerInvocationData.count(Phase.BUILD) == 1;

        StatefulSession session = session(knowledge, mode);
        assert ListenerInvocationData.count(Phase.CREATE) == 3 : "Actual: " + ListenerInvocationData.EVENTS;
        assert ListenerInvocationData.total() == 6; // 4 + additional two coming from the multiple() method
        ListenerInvocationData.reset();
        session.insert(1);
        session.fire();
        assert ListenerInvocationData.total() == 4 : " " + ListenerInvocationData.EVENTS;
        session.fire();
        assert ListenerInvocationData.total() == 8 : " " + ListenerInvocationData.EVENTS;


        ListenerInvocationData.reset();
        session.close();
        assert ListenerInvocationData.total() == 4 : " " + ListenerInvocationData.EVENTS;


    }
}
