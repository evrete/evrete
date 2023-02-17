package org.evrete.dsl;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.RuleSession;
import org.evrete.dsl.rules.EnvListenerRuleSet1;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;

class EnvListenerTests {
    private static KnowledgeService service;

    @BeforeAll
    static void setUpClass() {
        service = new KnowledgeService();
    }

    @AfterAll
    static void shutDownClass() {
        service.shutdown();
    }

    @ParameterizedTest
    @EnumSource(SessionTypes.class)
    void test1(SessionTypes t) throws IOException {
        Knowledge knowledge = service.newKnowledge(AbstractDSLProvider.PROVIDER_JAVA_C, EnvListenerRuleSet1.class);
        TestUtils.EnvHelperData.reset();
        knowledge.set("property1", "1");
        knowledge.set("property2", 11);

        assert TestUtils.EnvHelperData.total() == 1;
        assert TestUtils.EnvHelperData.total("property1") == 1;

        RuleSession<?> s = t.session(knowledge);
        s.set("property1", "2");
        s.set("property2", 22);
        assert TestUtils.EnvHelperData.total() == 3;
        assert TestUtils.EnvHelperData.total("property1") == 2;
        assert TestUtils.EnvHelperData.total("property2") == 1;
    }
}
