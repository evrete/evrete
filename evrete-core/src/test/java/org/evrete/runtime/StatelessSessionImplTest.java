package org.evrete.runtime;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.StatelessSession;
import org.junit.jupiter.api.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

class StatelessSessionImplTest {
    private static KnowledgeService service;
    private Knowledge knowledge;
    private final AtomicInteger rhsActionCounter = new AtomicInteger();

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
        knowledge = service.newKnowledge();
    }

    private StatelessSession simpleSession() {
        rhsActionCounter.set(0);
        return knowledge.builder()
                .newRule()
                .forEach("$i", Integer.class, "$s", String.class)
                .execute(ctx -> rhsActionCounter.incrementAndGet())
                .build()
                .newStatelessSession();

    }

    @Test
    void streamAllFactsTest() {
        StatelessSession session = simpleSession();

        session.insert(1);
        session.insert(2);
        session.insert("1");
        session.insert("2");

        Set<Object> memoryObjects = Collections.synchronizedSet(new HashSet<>());
        session.streamFacts().forEach(memoryObjects::add);

        Assertions.assertEquals(4, rhsActionCounter.get());
        Assertions.assertEquals(4, memoryObjects.size());

        assert memoryObjects.contains("1");
        assert memoryObjects.contains("2");
        assert memoryObjects.contains(1);
        assert memoryObjects.contains(2);

    }

    @Test
    void streamTypedExactTest() {
        StatelessSession session = simpleSession();

        session.insert(1);
        session.insert(2);
        session.insert("1");
        session.insert("2");

        Set<Object> memoryObjects = Collections.synchronizedSet(new HashSet<>());
        session.streamFacts(String.class).forEach(memoryObjects::add);

        Assertions.assertEquals(4, rhsActionCounter.get());
        Assertions.assertEquals(2, memoryObjects.size());

        assert memoryObjects.contains("1");
        assert memoryObjects.contains("2");

    }

    @Test
    void streamTypedSuperTest() {
        StatelessSession session = simpleSession();

        session.insert(1);
        session.insert(2);
        session.insert("1");
        session.insert("2");

        Set<Object> memoryObjects = Collections.synchronizedSet(new HashSet<>());
        session.streamFacts(CharSequence.class).forEach(memoryObjects::add);

        Assertions.assertEquals(4, rhsActionCounter.get());
        Assertions.assertEquals(2, memoryObjects.size());

        assert memoryObjects.contains("1");
        assert memoryObjects.contains("2");
    }

    @Test
    void streamFailureTest() {
        StatelessSession session = simpleSession();

        session.insert(1);
        session.insert(2);
        session.insert("1");
        session.insert("2");

        Set<Object> memoryObjects = Collections.synchronizedSet(new HashSet<>());
        session.streamFacts(CharSequence.class).forEach(memoryObjects::add);
        Assertions.assertThrows(IllegalStateException.class, () -> session.streamFacts(String.class));
    }
}