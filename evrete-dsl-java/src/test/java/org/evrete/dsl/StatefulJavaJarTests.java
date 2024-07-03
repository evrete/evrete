package org.evrete.dsl;

import org.evrete.KnowledgeService;
import org.evrete.api.ActivationMode;
import org.evrete.api.Knowledge;
import org.evrete.api.StatefulSession;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.atomic.AtomicInteger;

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
        File dir = TestUtils.testResourceAsFile("jars/jar2");

        TestUtils.createTempJarFile(dir, jarFile -> {
            try {
                Knowledge knowledge = service.newKnowledge()
                        .set(DSLJarProvider.CLASSES_PROPERTY, "pkg1.evrete.tests.rule.RuleSet2")
                        .importRules(Constants.PROVIDER_JAVA_JAR, jarFile.toURI().toURL());

                try (StatefulSession session = session(knowledge, mode)) {
                    assert session.getRules().size() == 2;
                    for (int i = 2; i < 100; i++) {
                        session.insert(i);
                    }
                    session.fire();

                    AtomicInteger primeCounter = new AtomicInteger();
                    session.forEachFact((h, o) -> primeCounter.incrementAndGet());

                    assert primeCounter.get() == 25;
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void test2(ActivationMode mode) throws Exception {
        File dir = TestUtils.testResourceAsFile("jars/jar2");
        TestUtils.createTempJarFile(dir, jarFile -> {
            try {
                Knowledge knowledge = service.newKnowledge()
                        .set(DSLJarProvider.RULESETS_PROPERTY, "Test Ruleset 2")
                        .importRules(new DSLJarProvider(), jarFile.toURI().toURL());

                try (StatefulSession session = session(knowledge, mode)) {
                    assert session.getRules().size() == 2;
                    for (int i = 2; i < 100; i++) {
                        session.insert(i);
                    }
                    session.fire();

                    AtomicInteger primeCounter = new AtomicInteger();
                    session.forEachFact((h, o) -> primeCounter.incrementAndGet());

                    assert primeCounter.get() == 25;
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

    }
}
