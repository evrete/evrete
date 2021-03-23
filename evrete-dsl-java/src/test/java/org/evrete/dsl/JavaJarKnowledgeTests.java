package org.evrete.dsl;

import org.evrete.KnowledgeService;
import org.evrete.api.ActivationMode;
import org.evrete.api.Knowledge;
import org.evrete.api.StatefulSession;
import org.evrete.util.NextIntSupplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.File;

class JavaJarKnowledgeTests extends CommonTestMethods {
    private static KnowledgeService service;
    private Knowledge runtime;

    @BeforeAll
    static void setUpClass() {
        service = new KnowledgeService();
    }

    @AfterAll
    static void shutDownClass() {
        service.shutdown();
    }

    @BeforeEach
    void init() {
        runtime = service.newKnowledge();
    }

    private StatefulSession session(ActivationMode mode) {
        return runtime.createSession().setActivationMode(mode);
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void test1(ActivationMode mode) {
        runtime
                .getConfiguration()
                .setProperty(JavaDSLJarProvider.CLASSES_PROPERTY, "pkg1.evrete.tests.rule.RuleSet1");
        applyToRuntimeAsURLs(runtime, AbstractJavaDSLProvider.PROVIDER_JAVA_J, new File("src/test/resources/jars/jar1//jar1-tests.jar"));
        StatefulSession session = session(mode);
        assert session.getRules().size() == 2;
        for (int i = 2; i < 100; i++) {
            session.insert(i);
        }
        session.fire();

        NextIntSupplier primeCounter = new NextIntSupplier();
        session.forEachFact((h, o) -> primeCounter.next());

        assert primeCounter.get() == 25;

    }
}
