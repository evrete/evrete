package org.evrete.runtime;

import org.evrete.Configuration;
import org.evrete.KnowledgeService;
import org.evrete.api.*;
import org.evrete.api.builders.RuleSetBuilder;
import org.evrete.classes.*;
import org.evrete.helper.FactEntry;
import org.evrete.helper.RhsAssert;
import org.evrete.helper.TestUtils;
import org.evrete.util.NextIntSupplier;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static org.evrete.api.FactBuilder.fact;

//TODO !!!! important: use a provided delayed executor to check memories, especially session memory scans and retrievals
class SessionAsyncTests {
    private static KnowledgeService service;
    private Knowledge knowledge;

    @BeforeAll
    static void setUpClass() {
        service = new KnowledgeService();
    }

    @AfterAll
    static void shutDownClass() {
        service.shutdown();
    }


    private StatefulSession newSession() {
        return knowledge.newStatefulSession();
    }


    @BeforeEach
    void init() {
        knowledge = service.newKnowledge();
    }





    @Test
    void sessionFactReads() {
        AtomicInteger counter = new AtomicInteger();
        knowledge
                .builder()
                .newRule()
                .forEach("$n", Integer.class)
                .execute(ctx->counter.incrementAndGet())
                .build();


        try (StatefulSession session = newSession()) {
            Set<Integer> facts = new HashSet<>();

            for (int i = 0; i < 1_000; i++) {
                facts.add(i);
            }
            session.insert0(facts, true);
            session.fire();

            Set<Integer> collected = Collections.synchronizedSet(new HashSet<>());
            session.streamFacts(Integer.class).forEach(collected::add);

            System.out.println(collected.size() + " vs " + facts.size());
        }
    }



}
