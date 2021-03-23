package org.evrete.dsl;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.RuleScope;
import org.evrete.api.StatefulSession;
import org.evrete.dsl.rules.SampleRuleSet4;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.FilePermission;
import java.util.PropertyPermission;

class JavaClassSecurityTests extends CommonTestMethods {
    private Knowledge runtime;

    @BeforeEach
    void init() {
        KnowledgeService service = new KnowledgeService();
        runtime = service.newKnowledge();
    }

    private StatefulSession session() {
        return runtime.createSession();
    }

    @Test
    void rule1TestFail() {
        assert System.getSecurityManager() != null;

        Assertions.assertThrows(
                SecurityException.class,
                () -> {
                    applyToRuntimeAsURL(runtime, SampleRuleSet4.class);
                    StatefulSession session = session();

                    session.insert(2);
                    session.fire();
                }
        );
    }

    @Test
    void rule1TestPass() {
        assert System.getSecurityManager() != null;

        runtime.getService().getSecurity().addPermission(RuleScope.BOTH, new FilePermission("<<ALL FILES>>", "read"));
        applyToRuntimeAsURL(runtime, SampleRuleSet4.class);
        StatefulSession session = session();

        session.insert(2);
        session.fire();

    }

    @Test
    void rule2TestFail() {
        assert System.getSecurityManager() != null;

        Assertions.assertThrows(
                SecurityException.class,
                () -> {
                    applyToRuntimeAsURL(runtime, SampleRuleSet4.class);
                    StatefulSession session = session();

                    session.insert(9L);
                    session.fire();
                }
        );
    }

    @Test
    void rule2TestFailDuplicate() {
        assert System.getSecurityManager() != null;

        Assertions.assertThrows(
                SecurityException.class,
                () -> {
                    applyToRuntimeAsURL(runtime, SampleRuleSet4.class);
                    StatefulSession session = session();

                    session.insert(9L);
                    session.fire();
                }
        );
    }

    @Test
    void rule2TestPass() {
        assert System.getSecurityManager() != null;

        runtime.getService().getSecurity()
                .addPermission(RuleScope.BOTH, new FilePermission("<<ALL FILES>>", "read"))
                .addPermission(RuleScope.BOTH, new PropertyPermission("some-unused-property", "write"))
        ;
        applyToRuntimeAsURL(runtime, SampleRuleSet4.class);
        StatefulSession session = session();

        session.insert(9L);
        session.fire();
    }
}
