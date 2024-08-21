package org.evrete.dsl;

import org.evrete.KnowledgeService;
import org.evrete.api.ActivationMode;
import org.evrete.api.Knowledge;
import org.evrete.api.StatelessSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.atomic.AtomicInteger;

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

        File dir = TestUtils.testResourceAsFile("jars/jar1");
        TestUtils.createTempJarFile(dir, jarFile -> {
            try {
                Knowledge knowledge = service.newKnowledge()
                        .set(DSLJarProvider.PROP_RULE_CLASSES, "pkg1.evrete.tests.rule.RuleSet1")
                        .importRules(Constants.PROVIDER_JAVA_JAR, jarFile);
                StatelessSession session = session(knowledge, mode);
                assert session.getRules().size() == 2;
                for (int i = 2; i < 100; i++) {
                    session.insert(i);
                }

                AtomicInteger primeCounter = new AtomicInteger();
                session.fire((o) -> primeCounter.incrementAndGet());

                assert primeCounter.get() == 25 : "Actual: " + primeCounter.get();
            } catch (IOException e){
                throw new UncheckedIOException(e);
            }
        });
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void test2(ActivationMode mode) throws Exception {
        File dir = TestUtils.testResourceAsFile("jars/jar1");
        TestUtils.createTempJarFile(dir, jarFile->{
            try {
                Knowledge knowledge = service.newKnowledge()
                        .set(DSLJarProvider.PROP_RULESETS, "Test Ruleset")
                        .importRules(new DSLJarProvider(), jarFile);

                StatelessSession session = session(knowledge, mode);
                assert session.getRules().size() == 2;
                for (int i = 2; i < 100; i++) {
                    session.insert(i);
                }

                AtomicInteger primeCounter = new AtomicInteger();
                session.fire((o) -> primeCounter.incrementAndGet());

                assert primeCounter.get() == 25;
            } catch (IOException e){
                throw new UncheckedIOException(e);
            }
        });
    }
}
