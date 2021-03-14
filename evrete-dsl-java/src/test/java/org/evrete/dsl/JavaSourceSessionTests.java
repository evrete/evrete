package org.evrete.dsl;

import org.evrete.KnowledgeService;
import org.evrete.api.ActivationMode;
import org.evrete.api.RuntimeRule;
import org.evrete.api.StatefulSession;
import org.evrete.util.NextIntSupplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

class JavaSourceSessionTests {
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
    void sort1(ActivationMode mode) {
        applyToRuntime("src/test/resources/java/SortTest1.java");
        StatefulSession session = session(mode);
        List<RuntimeRule> rules = session.getRules();
        assert rules.size() == 3;
        assert rules.get(0).getName().endsWith("rule1");
        assert rules.get(1).getName().endsWith("rule2");
        assert rules.get(2).getName().endsWith("rule3");
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void sort2(ActivationMode mode) {
        applyToRuntime("src/test/resources/java/SortTest2.java");
        StatefulSession session = session(mode);
        List<RuntimeRule> rules = session.getRules();
        assert rules.size() == 3;
        assert rules.get(0).getName().endsWith("rule1");
        assert rules.get(1).getName().endsWith("rule2");
        assert rules.get(2).getName().endsWith("rule3");
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void sort3(ActivationMode mode) {
        applyToRuntime("src/test/resources/java/SortTest3.java");
        StatefulSession session = session(mode);
        List<RuntimeRule> rules = session.getRules();
        assert rules.size() == 3;
        assert rules.get(0).getName().endsWith("rule3");
        assert rules.get(1).getName().endsWith("rule2");
        assert rules.get(2).getName().endsWith("rule1");
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void sort4(ActivationMode mode) {
        applyToRuntime("src/test/resources/java/SortTest4.java");
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
    void primeNonStaticMethod(ActivationMode mode) {
        applyToRuntime("src/test/resources/java/PrimeNumbers1.java");
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
    void primeStaticMethod(ActivationMode mode) {
        applyToRuntime("src/test/resources/java/PrimeNumbers2.java");
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
    void primeNonStaticMethodNonStaticCondition(ActivationMode mode) {
        applyToRuntime("src/test/resources/java/PrimeNumbers3.java");
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
    void primeNonStaticMethodStaticCondition(ActivationMode mode) {
        applyToRuntime("src/test/resources/java/PrimeNumbers4.java");
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
    void primeStaticMethodStaticCondition(ActivationMode mode) {
        applyToRuntime("src/test/resources/java/PrimeNumbers5.java");
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


    private void applyToRuntime(String file) {
        try {
            runtime.appendDslRules(JavaDSLSourceProvider.NAME, new FileInputStream(file));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
