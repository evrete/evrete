package org.evrete.dsl;

import org.evrete.KnowledgeService;
import org.evrete.api.ActivationMode;
import org.evrete.api.Knowledge;
import org.evrete.api.StatefulSession;
import org.evrete.dsl.rules.DeclarationRuleSet1;
import org.evrete.dsl.rules.DeclarationRuleSet2;
import org.evrete.util.NextIntSupplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class FieldDeclarationsTests extends CommonTestMethods {
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
        Knowledge knowledge = applyToRuntimeAsStream(service, DeclarationRuleSet1.class);
        StatefulSession session = session(knowledge, mode);

        for (int i = 2; i < 100; i++) {
            session.insert(String.valueOf(i));
        }

        session.fire();

        NextIntSupplier primeCounter = new NextIntSupplier();
        session.forEachFact((h, o) -> primeCounter.next());

        assert primeCounter.get() == 25;

    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void test2(ActivationMode mode) {
        ListenerInvocationData.reset();
        Knowledge knowledge = applyToRuntimeAsStream(service, DeclarationRuleSet2.class);
        StatefulSession session = session(knowledge, mode);

        for (int i = 2; i < 100; i++) {
            session.insert(String.valueOf(i));
        }

        session.fire();

        NextIntSupplier primeCounter = new NextIntSupplier();
        session.forEachFact((h, o) -> primeCounter.next());

        assert primeCounter.get() == 25;

    }
}
