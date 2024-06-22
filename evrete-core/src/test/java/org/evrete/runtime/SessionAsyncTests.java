package org.evrete.runtime;

import org.evrete.Configuration;
import org.evrete.KnowledgeService;
import org.evrete.api.FactHandle;
import org.evrete.api.Knowledge;
import org.evrete.api.StatefulSession;
import org.evrete.classes.TypeA;
import org.evrete.helper.TestUtils;
import org.evrete.util.DelayedExecutorService;
import org.junit.jupiter.api.*;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

//TODO !!!! important: use a provided delayed executor to check memories, especially session memory scans and retrievals
class SessionAsyncTests {
    private static final long DELAY_MS = 1000L;
    private static ExecutorService delayedExecutor;
    private static KnowledgeService service;

    private Knowledge knowledge;

    @BeforeAll
    static void setUpClass() {
        delayedExecutor = new DelayedExecutorService(DELAY_MS, TimeUnit.MILLISECONDS);
        Configuration config = new Configuration();
        service = KnowledgeService.builder(config).withExecutor(delayedExecutor).build();
    }

    @AfterAll
    static void shutDownClass() {
        delayedExecutor.shutdown();
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
            long t0 = Instant.now().toEpochMilli();
            session.insert0(facts, true);
            long t1 = Instant.now().toEpochMilli();
            assert t1 - t0 < DELAY_MS / 10;

            // Collecting facts BEFORE firing the session
            Set<Integer> collected = Collections.synchronizedSet(new HashSet<>());
            session.streamFacts(Integer.class).forEach(collected::add);

            Assertions.assertEquals(facts, collected);

            session.fire();
        }
    }


    @Test
    void factExistenceUponInsertTest() {
        knowledge.builder()
                .newRule()
                .forEach("$a", TypeA.class)
                .execute()
                .build();

        try (StatefulSession session = knowledge.newStatefulSession()) {
            TypeA a1 = new TypeA();
            TypeA a2 = new TypeA();
            FactHandle h1 = session.insert(a1);
            FactHandle h2 = session.insert(a2);

            Assertions.assertEquals(a1, session.getFact(h1));
            Assertions.assertEquals(a2, session.getFact(h2));

            FactHandle unknown = () -> Long.MAX_VALUE;

            Assertions.assertThrows(IllegalArgumentException.class, () -> session.getFact(unknown));
            Assertions.assertThrows(IllegalArgumentException.class, () -> session.delete(unknown));

            // Test deletion
            session.delete(h1);
            session.delete(h1); // Subsequent delete calls must not throw anything

            assert session.getFact(h1) == null;
            assert session.getFact(h2) == a2;

        }
    }


    @Test
    void testDelayedExecutor() {
        long delayMs = 200;
        try (DelayedExecutorService executorService = new DelayedExecutorService(delayMs, TimeUnit.MILLISECONDS)) {
            AtomicLong startTime = new AtomicLong();

            Runnable r = () -> startTime.set(Instant.now().toEpochMilli());
            executorService.submit(r);
            long postSubmitTime = Instant.now().toEpochMilli();

            TestUtils.sleep(delayMs * 2);
            assert postSubmitTime < startTime.get();

        }

    }

}
