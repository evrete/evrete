package org.evrete.dsl;

import org.evrete.KnowledgeService;
import org.evrete.api.ActivationMode;
import org.evrete.api.Knowledge;
import org.evrete.api.StatefulSession;
import org.evrete.util.NextIntSupplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.File;

class JavaJarTests extends CommonTestMethods {
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
        service
                .getConfiguration()
                .setProperty(JavaDSLJarProvider.CLASSES_PROPERTY, "pkg1.evrete.tests.rule.RuleSet1");
        Knowledge knowledge = applyToRuntimeAsURLs(service, AbstractJavaDSLProvider.PROVIDER_JAVA_J, new File("src/test/resources/jars/jar1//jar1-tests.jar"));
        StatefulSession session = session(knowledge, mode);
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
