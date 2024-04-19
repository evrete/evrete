package org.evrete.dsl;

import org.evrete.KnowledgeService;
import org.evrete.api.ActivationMode;
import org.evrete.api.Knowledge;
import org.evrete.api.StatefulSession;
import org.evrete.api.TypeResolver;
import org.evrete.dsl.rules.*;
import org.evrete.util.NextIntSupplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Future;

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
        return knowledge.newStatefulSession().setActivationMode(mode);
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void test1(ActivationMode mode) throws IOException {
        Knowledge knowledge = service.newKnowledge(AbstractDSLProvider.PROVIDER_JAVA_CLASS, DeclarationRuleSet1.class);
        NextIntSupplier primeCounter;
        try (StatefulSession session = session(knowledge, mode)) {

            for (int i = 2; i < 100; i++) {
                session.insert(String.valueOf(i));
            }

            session.fire();

            primeCounter = new NextIntSupplier();
            session.forEachFact((h, o) -> primeCounter.next());
            assert primeCounter.get() == 25;
        }
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void test2(ActivationMode mode) throws IOException {
        Knowledge knowledge = service.newKnowledge(AbstractDSLProvider.PROVIDER_JAVA_CLASS, DeclarationRuleSet2.class);
        NextIntSupplier primeCounter;
        try (StatefulSession session = session(knowledge, mode)) {

            for (int i = 2; i < 100; i++) {
                session.insert(String.valueOf(i));
            }

            session.fire();

            primeCounter = new NextIntSupplier();
            session.forEachFact((h, o) -> primeCounter.next());
            assert primeCounter.get() == 25;
        }
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void test3(ActivationMode mode) throws IOException {
        Knowledge knowledge = service.newKnowledge(AbstractDSLProvider.PROVIDER_JAVA_CLASS, DeclarationRuleSet3.class);
        NextIntSupplier primeCounter;
        try (StatefulSession session = session(knowledge, mode)) {
            session.set("random-offset", 0);

            for (int i = 2; i < 100; i++) {
                session.insert(String.valueOf(i));
            }

            session.fire();

            primeCounter = new NextIntSupplier();
            session.forEachFact((h, o) -> primeCounter.next());
            assert primeCounter.get() == 25 : "Actual: " + primeCounter.get();

        }

    }

    @SuppressWarnings("resource")
    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void test4(ActivationMode mode) throws IOException {
        Knowledge knowledge = service.newKnowledge(DSLClassProvider.class, DeclarationRuleSet4.class);

        Set<Future<StatefulSession>> futures = new HashSet<>();

        Random random = new Random();
        for (int shift = 1; shift < 10; shift++) {
            StatefulSession session = session(knowledge, mode);
            session.set("random-offset", random.nextInt(1000) + 1);
            for (int i = 2; i < 100; i++) {
                session.insert(String.valueOf(i));
            }

            Future<StatefulSession> future = session.fireAsync();
            futures.add(future);
        }

        while (!futures.isEmpty()) {
            Iterator<Future<StatefulSession>> it = futures.iterator();
            while (it.hasNext()) {
                Future<StatefulSession> future = it.next();
                if (future.isDone()) {
                    try {
                        StatefulSession session = future.get();
                        NextIntSupplier primeCounter = new NextIntSupplier();
                        session.forEachFact((h, o) -> primeCounter.next());
                        assert primeCounter.get() == 25 : "Actual: " + primeCounter.get();
                        session.close();
                    } catch (Throwable e) {
                        throw new IllegalStateException(e);
                    } finally {
                        it.remove();
                    }
                }
            }
        }
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void test5(ActivationMode mode) throws IOException {
        TypeResolver typeResolver = service.newTypeResolver();
        String type = "Hello world type";
        typeResolver.declare(type, String.class);

        Knowledge knowledge = service.newKnowledge(DSLClassProvider.class, typeResolver, DeclarationRuleSet5.class);
        try (StatefulSession session = session(knowledge, mode)) {
            session.set("random-offset", 0);

            for (int i = 2; i < 100; i++) {
                session.insertAs(type, String.valueOf(i));
            }

            session.fire();

            NextIntSupplier primeCounter = new NextIntSupplier();
            session.forEachFact((h, o) -> primeCounter.next());

            assert primeCounter.get() == 25 : "Actual: " + primeCounter.get();
        }
    }

}
