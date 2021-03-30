package org.evrete.dsl;

import org.evrete.KnowledgeService;
import org.evrete.api.ActivationMode;
import org.evrete.api.RuntimeRule;
import org.evrete.api.StatefulSession;
import org.evrete.dsl.rules.SampleRuleSet1;
import org.evrete.dsl.rules.SampleRuleSet3;
import org.evrete.dsl.rules.SortedRuleSet1;
import org.evrete.dsl.rules.SortedRuleSet2;
import org.evrete.util.NextIntSupplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.File;
import java.util.List;

class JavaClassSessionTests extends CommonTestMethods {
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
        applyToRuntimeAsStream(runtime, SampleRuleSet1.class);
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
        // Applying Class as a File (relies on standard maven project layout)
        applyToRuntimeAsFile(runtime, AbstractJavaDSLProvider.PROVIDER_JAVA_C, new File("target/test-classes/org/evrete/dsl/rules/SampleRuleSet2.class"));
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
        applyToRuntimeAsStream(runtime, SampleRuleSet3.class);
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
        applyToRuntimeAsURL(runtime, SortedRuleSet1.class);
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
        applyToRuntimeAsStream(runtime, SortedRuleSet2.class);
        StatefulSession session = session(mode);
        List<RuntimeRule> rules = session.getRules();
        assert rules.size() == 5;
        assert rules.get(0).getName().endsWith("rule2"); // Salience 100
        assert rules.get(1).getName().endsWith("rule3"); // Salience 10
        assert rules.get(2).getName().endsWith("rule1"); // Salience -1
        assert rules.get(3).getName().endsWith("rule5");
        assert rules.get(4).getName().endsWith("rule4");
    }
}