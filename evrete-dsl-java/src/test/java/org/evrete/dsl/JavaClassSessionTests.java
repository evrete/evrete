package org.evrete.dsl;

import org.evrete.KnowledgeService;
import org.evrete.api.ActivationMode;
import org.evrete.api.RuntimeRule;
import org.evrete.api.StatefulSession;
import org.evrete.dsl.rules.*;
import org.evrete.util.NextIntSupplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

class JavaClassSessionTests {
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
    void primeTest1(ActivationMode mode) {
        applyToRuntimeAsStream(SampleRuleSet1.class);
        StatefulSession session = session(mode);

        assert session.getRules().size() == 1;
        for (int i = 2; i < 100; i++) {
            session.insert(i);
        }
        session.fire();

        NextIntSupplier primeCounter = new NextIntSupplier();
        session.forEachFact((h, o) -> primeCounter.next());

        assert primeCounter.get() == 25;

    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void primeTest2(ActivationMode mode) {
        applyToRuntimeAsURL(SampleRuleSet2.class);
        StatefulSession session = session(mode);

        assert session.getRules().size() == 1;
        for (int i = 2; i < 100; i++) {
            session.insert(i);
        }
        session.fire();

        NextIntSupplier primeCounter = new NextIntSupplier();
        session.forEachFact((h, o) -> primeCounter.next());

        assert primeCounter.get() == 25;
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void primeTest3(ActivationMode mode) {
        applyToRuntimeAsStream(SampleRuleSet3.class);
        StatefulSession session = session(mode);

        assert session.getRules().size() == 1;
        for (int i = 2; i < 100; i++) {
            session.insert(i);
        }
        session.fire();

        NextIntSupplier primeCounter = new NextIntSupplier();
        session.forEachFact((h, o) -> primeCounter.next());

        assert primeCounter.get() == 25;
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void sortInheritance1(ActivationMode mode) {
        applyToRuntimeAsURL(SortedRuleSet1.class);
        StatefulSession session = session(mode);
        List<RuntimeRule> rules = session.getRules();
        assert rules.size() == 5;
        assert rules.get(0).getName().endsWith("rule2"); // Salience 100
        assert rules.get(1).getName().endsWith("rule3"); // Salience 10
        assert rules.get(2).getName().endsWith("rule1"); // Salience -1
        assert rules.get(3).getName().endsWith("rule5");
        assert rules.get(4).getName().endsWith("rule4");
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void sortInheritance2(ActivationMode mode) {
        applyToRuntimeAsStream(SortedRuleSet2.class);
        StatefulSession session = session(mode);
        List<RuntimeRule> rules = session.getRules();
        assert rules.size() == 5;
        assert rules.get(0).getName().endsWith("rule2"); // Salience 100
        assert rules.get(1).getName().endsWith("rule3"); // Salience 10
        assert rules.get(2).getName().endsWith("rule1"); // Salience -1
        assert rules.get(3).getName().endsWith("rule5");
        assert rules.get(4).getName().endsWith("rule4");
    }


    private void applyToRuntimeAsStream(Class<?> ruleClass) {
        try {
            String url = ruleClass.getName().replaceAll("\\.", "/") + ".class";
            InputStream is = ruleClass.getClassLoader().getResourceAsStream(url);
            runtime.appendDslRules(JavaDSLClassProvider.NAME, is);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void applyToRuntimeAsURL(Class<?> ruleClass) {
        try {
            String url = ruleClass.getName().replaceAll("\\.", "/") + ".class";
            URL u = ruleClass.getClassLoader().getResource(url);
            runtime.appendDslRules(JavaDSLClassProvider.NAME, u);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
