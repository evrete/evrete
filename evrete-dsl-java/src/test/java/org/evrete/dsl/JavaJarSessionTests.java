package org.evrete.dsl;

import org.evrete.KnowledgeService;
import org.evrete.api.ActivationMode;
import org.evrete.api.StatefulSession;
import org.evrete.util.NextIntSupplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.File;
import java.net.URL;

class JavaJarSessionTests {
    private static KnowledgeService service;
    private StatefulSession runtime;

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
        runtime = service.newSession();
    }

    private StatefulSession session(ActivationMode mode) {
        return runtime.setActivationMode(mode);
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void test1(ActivationMode mode) {
        runtime
                .getConfiguration()
                .setProperty(JavaDSLJarProvider.CLASSES_PROPERTY, "org.evrete.tests.rule.RuleSet1");
        applyToRuntimeAsStream("src/test/resources/jars/jar1-tests.jar");
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


    private void applyToRuntimeAsStream(String... files) {
        assert files != null && files.length > 0;
        try {
            URL[] urls = new URL[files.length];
            for (int i = 0; i < urls.length; i++) {
                File f = new File(files[i]);
                urls[i] = f.toURI().toURL();
            }
            runtime.appendDslRules(JavaDSLJarProvider.NAME, urls);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
