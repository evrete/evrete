package org.evrete.dsl;

import org.evrete.KnowledgeService;
import org.evrete.api.*;
import org.evrete.dsl.rules.*;
import org.evrete.util.NextIntSupplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

class StatelessJavaClassTests {
    private static KnowledgeService service;

    @BeforeAll
    static void setUpClass() {
        service = new KnowledgeService();
    }

    @AfterAll
    static void shutDownClass() {
        service.shutdown();
    }

    private static StatelessSession session(Knowledge knowledge, ActivationMode mode) {
        return knowledge.newStatelessSession(mode);
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void primeTest1VirtualMethod(ActivationMode mode) throws IOException {
        Knowledge knowledge = service.newKnowledge()
                .builder()
                .importRules(Constants.PROVIDER_JAVA_CLASS, SampleRuleSet1Virtual.class)
                .build();


        StatelessSession session = session(knowledge, mode);

        assert session.getRules().size() == 1;
        for (int i = 2; i < 100; i++) {
            session.insert(i);
        }

        NextIntSupplier primeCounter = new NextIntSupplier();
        session.fire((h, o) -> primeCounter.incrementAndGet());

        assert primeCounter.get() == 25;

    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void primeTest1StaticMethod(ActivationMode mode) throws IOException {
        Knowledge knowledge = service.newKnowledge()
                .builder()
                .importRules(Constants.PROVIDER_JAVA_CLASS, SampleRuleSet1Static.class)
                .build();


        StatelessSession session = session(knowledge, mode);

        assert session.getRules().size() == 1;
        for (int i = 2; i < 100; i++) {
            session.insert(i);
        }

        NextIntSupplier primeCounter = new NextIntSupplier();
        session.fire((h, o) -> primeCounter.incrementAndGet());

        assert primeCounter.get() == 25;

    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void primeTest2StaticVirtual(ActivationMode mode) throws IOException {
        Knowledge knowledge = service.newKnowledge()
                .builder()
                .importRules(new DSLClassProvider(), SampleRuleSet2StaticVirtual.class)
                .build();

        StatelessSession session = session(knowledge, mode);

        assert session.getRules().size() == 1;
        for (int i = 2; i < 100; i++) {
            session.insert(i);
        }

        NextIntSupplier primeCounter = new NextIntSupplier();
        session.fire((h, o) -> primeCounter.incrementAndGet());

        assert primeCounter.get() == 25;
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void primeTest2StaticStatic(ActivationMode mode) throws IOException {
        Knowledge knowledge = service.newKnowledge()
                .builder()
                .importRules(new DSLClassProvider(), SampleRuleSet2StaticStatic.class)
                .build();

        StatelessSession session = session(knowledge, mode);

        assert session.getRules().size() == 1;
        for (int i = 2; i < 100; i++) {
            session.insert(i);
        }

        NextIntSupplier primeCounter = new NextIntSupplier();
        session.fire((h, o) -> primeCounter.incrementAndGet());

        assert primeCounter.get() == 25;
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void primeTest2VirtualVirtual(ActivationMode mode) throws IOException {
        Knowledge knowledge = service.newKnowledge()
                .builder()
                .importRules(new DSLClassProvider(), SampleRuleSet2VirtualVirtual.class)
                .build();

        StatelessSession session = session(knowledge, mode);

        assert session.getRules().size() == 1;
        for (int i = 2; i < 100; i++) {
            session.insert(i);
        }

        NextIntSupplier primeCounter = new NextIntSupplier();
        session.fire((h, o) -> primeCounter.incrementAndGet());

        assert primeCounter.get() == 25;
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void primeTest2VirtualStatic(ActivationMode mode) throws IOException {
        Knowledge knowledge = service.newKnowledge()
                .builder()
                .importRules(new DSLClassProvider(), SampleRuleSet2VirtualStatic.class)
                .build();

        StatelessSession session = session(knowledge, mode);

        assert session.getRules().size() == 1;
        for (int i = 2; i < 100; i++) {
            session.insert(i);
        }

        NextIntSupplier primeCounter = new NextIntSupplier();
        session.fire((h, o) -> primeCounter.incrementAndGet());

        assert primeCounter.get() == 25;
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void primeTest3(ActivationMode mode) throws IOException {

        Knowledge knowledge = service.newKnowledge()
                .builder()
                .importRules(Constants.PROVIDER_JAVA_CLASS, SampleRuleSet3.class)
                .build();

        StatelessSession session = session(knowledge, mode);

        assert session.getRules().size() == 1;
        for (int i = 2; i < 100; i++) {
            session.insert(i);
        }

        NextIntSupplier primeCounter = new NextIntSupplier();
        session.fire((h, o) -> primeCounter.incrementAndGet());

        assert primeCounter.get() == 25;
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void sortInheritance1(ActivationMode mode) throws IOException {
        Knowledge knowledge = service.newKnowledge()
                .builder()
                .importRules(new DSLClassProvider(), SortedRuleSet1.class)
                .build();

        StatelessSession session = session(knowledge, mode);
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
    void sortInheritance2(ActivationMode mode) throws IOException {
        Knowledge knowledge = service.newKnowledge()
                .builder()
                .importRules(Constants.PROVIDER_JAVA_CLASS, SortedRuleSet2.class)
                .build();

        StatelessSession session = session(knowledge, mode);
        List<RuntimeRule> rules = session.getRules();
        assert rules.size() == 5 : "Actual: " + rules.size() + ": " + rules;

        assert rules.get(0).getName().endsWith("rule2"); // Salience 100
        assert rules.get(1).getName().endsWith("rule3"); // Salience 10
        assert rules.get(2).getName().endsWith("rule1"); // Salience -1
        assert rules.get(3).getName().endsWith("rule5");
        assert rules.get(4).getName().endsWith("rule4");
    }

    @Test
    void multipleRulesets() throws IOException {
        Knowledge knowledge = service.newKnowledge()
                .builder()
                .importRules(Constants.PROVIDER_JAVA_CLASS, Arrays.asList(SortedRuleSet1.class, SampleRuleSet3.class))
                .build();
        List<RuleDescriptor> knowledgeRules = knowledge.getRules();
        Assertions.assertEquals(6, knowledgeRules.size());

        StatelessSession session = session(knowledge, ActivationMode.DEFAULT);
        List<RuntimeRule> sessionRules = session.getRules();
        Assertions.assertEquals(6, sessionRules.size());


    }
}
