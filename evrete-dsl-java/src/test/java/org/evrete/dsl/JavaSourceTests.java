package org.evrete.dsl;

import org.evrete.KnowledgeService;
import org.evrete.api.ActivationMode;
import org.evrete.api.Knowledge;
import org.evrete.api.RuntimeRule;
import org.evrete.api.StatefulSession;
import org.evrete.util.NextIntSupplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.File;
import java.util.List;

class JavaSourceTests extends CommonTestMethods {
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
        return knowledge.newStatefulSession().setActivationMode(mode);
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void sort1(ActivationMode mode) {
        Knowledge knowledge = applyToRuntimeAsFile(service, AbstractDSLProvider.PROVIDER_JAVA_S, new File("src/test/resources/java/SortTest1.java"));
        StatefulSession session = session(knowledge, mode);
        List<RuntimeRule> rules = session.getRules();
        assert rules.size() == 3;
        assert rules.get(0).getName().endsWith("rule1");
        assert rules.get(1).getName().endsWith("rule2");
        assert rules.get(2).getName().endsWith("rule3");
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void sort2(ActivationMode mode) {
        Knowledge knowledge = applyToRuntimeAsFile(service, AbstractDSLProvider.PROVIDER_JAVA_S, new File("src/test/resources/java/SortTest2.java"));
        StatefulSession session = session(knowledge, mode);
        List<RuntimeRule> rules = session.getRules();
        assert rules.size() == 3;
        assert rules.get(0).getName().endsWith("rule1");
        assert rules.get(1).getName().endsWith("rule2");
        assert rules.get(2).getName().endsWith("rule3");
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void sort3(ActivationMode mode) {
        Knowledge knowledge = applyToRuntimeAsFile(service, AbstractDSLProvider.PROVIDER_JAVA_S, new File("src/test/resources/java/SortTest3.java"));
        StatefulSession session = session(knowledge, mode);
        List<RuntimeRule> rules = session.getRules();
        assert rules.size() == 3;
        assert rules.get(0).getName().endsWith("rule3");
        assert rules.get(1).getName().endsWith("rule2");
        assert rules.get(2).getName().endsWith("rule1");
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void sort4(ActivationMode mode) {
        Knowledge knowledge = applyToRuntimeAsFile(service, AbstractDSLProvider.PROVIDER_JAVA_S, new File("src/test/resources/java/SortTest4.java"));
        StatefulSession session = session(knowledge, mode);
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
        Knowledge knowledge = applyToRuntimeAsFile(service, AbstractDSLProvider.PROVIDER_JAVA_S, new File("src/test/resources/java/PrimeNumbers1.java"));
        StatefulSession session = session(knowledge, mode);

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
        Knowledge knowledge = applyToRuntimeAsFile(service, AbstractDSLProvider.PROVIDER_JAVA_S, new File("src/test/resources/java/PrimeNumbers2.java"));
        StatefulSession session = session(knowledge, mode);

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
        Knowledge knowledge = applyToRuntimeAsFile(service, AbstractDSLProvider.PROVIDER_JAVA_S, new File("src/test/resources/java/PrimeNumbers3.java"));
        StatefulSession session = session(knowledge, mode);

        assert session.getRules().size() == 1;
        for (int i = 2; i < 100; i++) {
            session.insert(i);
        }
        session.fire();

        NextIntSupplier primeCounter = new NextIntSupplier();
        session.forEachFact((h, o) -> primeCounter.next());

        assert primeCounter.get() == 25 : "Actual: " + primeCounter.get();
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void primeNonStaticMethodStaticCondition(ActivationMode mode) {
        Knowledge knowledge = applyToRuntimeAsFile(service, AbstractDSLProvider.PROVIDER_JAVA_S, new File("src/test/resources/java/PrimeNumbers4.java"));
        StatefulSession session = session(knowledge, mode);

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
        Knowledge knowledge = applyToRuntimeAsFile(service, AbstractDSLProvider.PROVIDER_JAVA_S, new File("src/test/resources/java/PrimeNumbers5.java"));
        StatefulSession session = session(knowledge, mode);

        assert session.getRules().size() == 1;
        for (int i = 2; i < 100; i++) {
            session.insert(i);
        }
        session.fire();

        NextIntSupplier primeCounter = new NextIntSupplier();
        session.forEachFact((h, o) -> primeCounter.next());

        assert primeCounter.get() == 25;
    }
}
