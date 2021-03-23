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
    void test1Fail() {
        assert System.getSecurityManager() != null;

        Assertions.assertThrows(
                SecurityException.class,
                () -> {
                    runtime
                            .getConfiguration()
                            .setProperty(JavaDSLJarProvider.CLASSES_PROPERTY, "pkg1.evrete.tests.rule.RuleSet2");
                    applyToRuntimeAsURLs(runtime, AbstractJavaDSLProvider.PROVIDER_JAVA_J, new File("src/test/resources/jars/jar1/jar1-tests.jar"));
                    StatefulSession session = session();
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

        runtime
                .getService()
                .getSecurity()
                .addPermission(RuleScope.BOTH, new FilePermission("<<ALL FILES>>", "read"));
        runtime
                .getConfiguration()
                .setProperty(JavaDSLJarProvider.CLASSES_PROPERTY, "pkg1.evrete.tests.rule.RuleSet2");


        applyToRuntimeAsURLs(runtime, AbstractJavaDSLProvider.PROVIDER_JAVA_J, new File("src/test/resources/jars/jar1/jar1-tests.jar"));
        StatefulSession session = session();
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
