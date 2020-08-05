package org.evrete;

import org.evrete.api.Knowledge;
import org.evrete.api.RuleBuilder;
import org.evrete.runtime.KnowledgeImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RuleBuilderTest {
    private static KnowledgeService service;
    private static KnowledgeImpl knowledge;

    @BeforeAll
    static void setUpClass() {
        service = new KnowledgeService();
        knowledge = (KnowledgeImpl) service.newKnowledge();
    }

    @AfterAll
    static void shutDownClass() {
        service.shutdown();
    }

    @Test
    void newRule1() {
        RuleBuilder<Knowledge> r1 = knowledge.newRule();
        RuleBuilder<Knowledge> r2 = knowledge.newRule("A");
        RuleBuilder<Knowledge> r3 = knowledge.newRule("B");
        assert knowledge.getRuleBuilder(r1.getName()) == r1;
        assert knowledge.getRuleBuilder(r2.getName()) == r2;
        assert knowledge.getRuleBuilder(r3.getName()) == r3;
    }

}