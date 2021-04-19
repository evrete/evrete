package org.evrete.dsl;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.RuleScope;
import org.evrete.api.StatefulSession;
import org.evrete.util.NextIntSupplier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FilePermission;

class JavaJarSecurityTests extends CommonTestMethods {
    private KnowledgeService service;

    @BeforeEach
    void init() {
        service = new KnowledgeService();
    }

    @Test
    void test1Fail() {
        assert System.getSecurityManager() != null;

        Assertions.assertThrows(
                SecurityException.class,
                () -> {
                    service
                            .getConfiguration()
                            .setProperty(DSLJarProvider.CLASSES_PROPERTY, "pkg1.evrete.tests.rule.RuleSet2");
                    Knowledge knowledge = applyToRuntimeAsURLs(service, AbstractDSLProvider.PROVIDER_JAVA_J, new File("src/test/resources/jars/jar1/jar1-tests.jar"));
                    StatefulSession session = knowledge.createSession();
                    assert session.getRules().size() == 2 : "Actual: " + session.getRules().size();
                    for (int i = 2; i < 100; i++) {
                        session.insert(i);
                    }
                    session.fire();
                }
        );
    }

    @Test
    void test1Pass() {
        assert System.getSecurityManager() != null;

        service
                .getSecurity()
                .addPermission(RuleScope.BOTH, new FilePermission("<<ALL FILES>>", "read"));
        service
                .getConfiguration()
                .setProperty(DSLJarProvider.CLASSES_PROPERTY, "pkg1.evrete.tests.rule.RuleSet2");


        Knowledge knowledge = applyToRuntimeAsURLs(service, AbstractDSLProvider.PROVIDER_JAVA_J, new File("src/test/resources/jars/jar1/jar1-tests.jar"));
        StatefulSession session = knowledge.createSession();
        assert session.getRules().size() == 2 : "Actual: " + session.getRules().size();
        for (int i = 2; i < 100; i++) {
            session.insert(i);
        }
        session.fire();

        NextIntSupplier primeCounter = new NextIntSupplier();
        session.forEachFact((h, o) -> primeCounter.next());

        assert primeCounter.get() == 25;

    }
}
