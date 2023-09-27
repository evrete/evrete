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

class StatefulJavaJarTests {
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
        return knowledge.newStatefulSession(mode);
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void test1(ActivationMode mode) throws Exception {
        File jarFile = TestUtils.jarFile("src/test/resources/jars/jar2");

        try {
            Knowledge knowledge = service.newKnowledge(AbstractDSLProvider.PROVIDER_JAVA_J, jarFile.toURI().toURL());
            try (StatefulSession session = session(knowledge, mode)) {
                assert session.getRules().size() == 2;
                for (int i = 2; i < 100; i++) {
                    session.insert(i);
                }
                session.fire();

                NextIntSupplier primeCounter = new NextIntSupplier();
                session.forEachFact((h, o) -> primeCounter.next());

                assert primeCounter.get() == 25;
            }
        } finally {
            assert jarFile.delete();
        }
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void test2(ActivationMode mode) throws Exception {
        File jarFile = TestUtils.jarFile("src/test/resources/jars/jar2");

        try {
            Knowledge knowledge = service.newKnowledge(DSLJarProvider.class, jarFile.toURI().toURL());

            try (StatefulSession session = session(knowledge, mode)) {
                assert session.getRules().size() == 2;
                for (int i = 2; i < 100; i++) {
                    session.insert(i);
                }
                session.fire();

                NextIntSupplier primeCounter = new NextIntSupplier();
                session.forEachFact((h, o) -> primeCounter.next());

                assert primeCounter.get() == 25;
            }
        } finally {
            assert jarFile.delete();
        }
    }
}
