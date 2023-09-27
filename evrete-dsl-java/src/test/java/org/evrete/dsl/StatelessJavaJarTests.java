package org.evrete.dsl;

import org.evrete.KnowledgeService;
import org.evrete.api.ActivationMode;
import org.evrete.api.Knowledge;
import org.evrete.api.StatelessSession;
import org.evrete.util.NextIntSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.File;

class StatelessJavaJarTests {
    private KnowledgeService service;

    @BeforeEach
    void setUpClass() {
        service = new KnowledgeService();
    }

    @AfterEach
    void shutDownClass() {
        service.shutdown();
    }

    private static StatelessSession session(Knowledge knowledge, ActivationMode mode) {
        return knowledge.newStatelessSession(mode);
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void test1(ActivationMode mode) throws Exception {
        service
                .getConfiguration()
                .setProperty(DSLJarProvider.CLASSES_PROPERTY, "pkg1.evrete.tests.rule.RuleSet1");
        File jarFile = TestUtils.jarFile("src/test/resources/jars/jar1");

        try {
            Knowledge knowledge = service.newKnowledge(AbstractDSLProvider.PROVIDER_JAVA_J, jarFile);
            StatelessSession session = session(knowledge, mode);
            assert session.getRules().size() == 2;
            for (int i = 2; i < 100; i++) {
                session.insert(i);
            }

            NextIntSupplier primeCounter = new NextIntSupplier();
            session.fire((o) -> primeCounter.next());

            assert primeCounter.get() == 25 : "Actual: " + primeCounter.get();
        } finally {
            assert jarFile.delete();
        }

    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void test2(ActivationMode mode) throws Exception {

        File jarFile = TestUtils.jarFile("src/test/resources/jars/jar1");

        try {
            Knowledge knowledge = service.newKnowledge(DSLJarProvider.class, jarFile);
            StatelessSession session = session(knowledge, mode);
            assert session.getRules().size() == 2;
            for (int i = 2; i < 100; i++) {
                session.insert(i);
            }

            NextIntSupplier primeCounter = new NextIntSupplier();
            session.fire((o) -> primeCounter.next());

            assert primeCounter.get() == 25;
        } finally {
            assert jarFile.delete();
        }

    }
}
