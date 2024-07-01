package org.evrete.dsl;

import org.evrete.KnowledgeService;
import org.evrete.api.ActivationMode;
import org.evrete.api.Knowledge;
import org.evrete.api.StatefulSession;
import org.evrete.dsl.rules.DeclarationRuleSet1;
import org.evrete.dsl.rules.DeclarationRuleSet2;
import org.evrete.dsl.rules.DeclarationRuleSet3;
import org.evrete.dsl.rules.DeclarationRuleSet5;
import org.evrete.util.NextIntSupplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

class FieldDeclarationsTests {
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
    void test1(ActivationMode mode) throws IOException {
        Knowledge knowledge = service.newKnowledge()
                .importRules(Constants.PROVIDER_JAVA_CLASS, DeclarationRuleSet1.class)
                ;
        NextIntSupplier primeCounter;
        try (StatefulSession session = session(knowledge, mode)) {

            for (int i = 2; i < 100; i++) {
                session.insert(String.valueOf(i));
            }

            session.fire();

            primeCounter = new NextIntSupplier();
            session.forEachFact((h, o) -> primeCounter.incrementAndGet());
            assert primeCounter.get() == 25;
        }
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void test2(ActivationMode mode) throws IOException {
        Knowledge knowledge = service.newKnowledge()
                .importRules(Constants.PROVIDER_JAVA_CLASS, DeclarationRuleSet2.class);
        AtomicInteger primeCounter;
        try (StatefulSession session = session(knowledge, mode)) {

            for (int i = 2; i < 100; i++) {
                session.insert(String.valueOf(i));
            }

            session.fire();

            primeCounter = new AtomicInteger();
            session.forEachFact((h, o) -> primeCounter.incrementAndGet());
            assert primeCounter.get() == 25;
        }
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void test3(ActivationMode mode) throws IOException {
        Knowledge knowledge = service.newKnowledge()
                .importRules(Constants.PROVIDER_JAVA_CLASS, DeclarationRuleSet3.class);


        NextIntSupplier primeCounter;
        try (StatefulSession session = session(knowledge, mode)) {
            session.set("random-offset", 0);

            for (int i = 2; i < 100; i++) {
                session.insert(String.valueOf(i));
            }

            session.fire();

            primeCounter = new NextIntSupplier();
            session.forEachFact((h, o) -> primeCounter.incrementAndGet());
            assert primeCounter.get() == 25 : "Actual: " + primeCounter.get();

        }

    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void test5(ActivationMode mode) throws IOException {
        String logicalTypeName = "Hello world type";
        Knowledge knowledge = service.newKnowledge()
                .configureTypes(typeResolver -> typeResolver.declare(logicalTypeName, String.class))
                .importRules(Constants.PROVIDER_JAVA_CLASS, DeclarationRuleSet5.class);
        try (StatefulSession session = session(knowledge, mode)) {
            session.set("random-offset", 0);

            for (int i = 2; i < 100; i++) {
                session.insertAs(logicalTypeName, String.valueOf(i));
            }

            session.fire();

            AtomicInteger primeCounter = new AtomicInteger();
            session.forEachFact((h, o) -> primeCounter.incrementAndGet());

            assert primeCounter.get() == 25 : "Actual: " + primeCounter.get();
        }
    }

}
