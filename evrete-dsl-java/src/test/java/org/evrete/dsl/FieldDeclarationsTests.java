package org.evrete.dsl;

import org.evrete.KnowledgeService;
import org.evrete.api.ActivationMode;
import org.evrete.api.Knowledge;
import org.evrete.api.StatefulSession;
import org.evrete.dsl.rules.DeclarationRuleSet1;
import org.evrete.dsl.rules.DeclarationRuleSet2;
import org.evrete.dsl.rules.DeclarationRuleSet3;
import org.evrete.dsl.rules.DeclarationRuleSet4;
import org.evrete.util.NextIntSupplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Future;

class FieldDeclarationsTests extends CommonTestMethods {
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
        return knowledge.createSession().setActivationMode(mode);
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void test1(ActivationMode mode) {
        Knowledge knowledge = applyToRuntimeAsStream(service, DeclarationRuleSet1.class);
        StatefulSession session = session(knowledge, mode);

        for (int i = 2; i < 100; i++) {
            session.insert(String.valueOf(i));
        }

        session.fire();

        NextIntSupplier primeCounter = new NextIntSupplier();
        session.forEachFact((h, o) -> primeCounter.next());

        assert primeCounter.get() == 25;

    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void test2(ActivationMode mode) {
        Knowledge knowledge = applyToRuntimeAsStream(service, DeclarationRuleSet2.class);
        StatefulSession session = session(knowledge, mode);

        for (int i = 2; i < 100; i++) {
            session.insert(String.valueOf(i));
        }

        session.fire();

        NextIntSupplier primeCounter = new NextIntSupplier();
        session.forEachFact((h, o) -> primeCounter.next());

        assert primeCounter.get() == 25;

    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void test3(ActivationMode mode) {
        Knowledge knowledge = applyToRuntimeAsStream(service, DeclarationRuleSet3.class);
        StatefulSession session = session(knowledge, mode);
        session.set("random-offset", 0);

        for (int i = 2; i < 100; i++) {
            session.insert(String.valueOf(i));
        }

        session.fire();

        NextIntSupplier primeCounter = new NextIntSupplier();
        session.forEachFact((h, o) -> primeCounter.next());

        assert primeCounter.get() == 25 : "Actual: " + primeCounter.get();

    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void test4(ActivationMode mode) {
        Knowledge knowledge = applyToRuntimeAsStream(service, DeclarationRuleSet4.class);

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
                    } catch (Throwable e) {
                        throw new IllegalStateException(e);
                    } finally {
                        it.remove();
                    }
                }
            }
        }
    }
}
