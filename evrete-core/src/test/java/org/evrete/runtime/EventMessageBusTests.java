package org.evrete.runtime;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.RuleSession;
import org.evrete.api.events.*;
import org.evrete.helper.TestUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

class EventMessageBusTests {
    private static KnowledgeService service;

    @BeforeAll
    static void setUpClass() {
        service = new KnowledgeService();
    }

    @AfterAll
    static void shutDownClass() {
        service.shutdown();
    }


    @Test
    void createKnowledgeEventSync() {

        Set<Knowledge> instances = new HashSet<>();
        Events.Subscription subscription = service.subscribe(
                KnowledgeCreatedEvent.class,
                false,
                e -> instances.add(e.getKnowledge())
        );
        Knowledge k1 = service.newKnowledge();
        Knowledge k2 = service.newKnowledge();
        Knowledge k3 = service.newKnowledge();

        assert instances.containsAll(Arrays.asList(k1, k2, k3));
        assert instances.size() == 3;

        // Unsubscribe and test again
        subscription.cancel();

        service.newKnowledge();
        service.newKnowledge();
        service.newKnowledge();
        // The state should be the same
        assert instances.containsAll(Arrays.asList(k1, k2, k3));
        assert instances.size() == 3;

    }

    @Test
    void createKnowledgeEventAsync() {

        KnowledgeService service = new KnowledgeService();
        Set<String> threadNames = Collections.synchronizedSet(new HashSet<>());
        Events.Subscription subscription = service.subscribeAsync(
                KnowledgeCreatedEvent.class,
                e -> {
                    Thread thread = Thread.currentThread();
                    threadNames.add(thread.getName());
                    TestUtils.sleep(new Random().nextInt(100));
                }
        );

        for (int i = 0; i < 16; i++) {
            service.newKnowledge();
        }

        TestUtils.sleep(1000L);

        if (Runtime.getRuntime().availableProcessors() > 1) {
            assert threadNames.size() > 1;
        } else {
            Logger.getAnonymousLogger().warning("Unable to test, available processors: " + Runtime.getRuntime().availableProcessors());
        }

        subscription.cancel();

        service.shutdown();


    }

    @Test
    void createKnowledgeEventSyncMultipleSubscribers() {

        Set<Knowledge> instances1 = new HashSet<>();
        Events.Subscription subscription1 = service.subscribe(
                KnowledgeCreatedEvent.class,
                false,
                e -> instances1.add(e.getKnowledge())
        );

        Set<Knowledge> instances2 = new HashSet<>();
        Events.Subscription subscription2 = service.subscribe(
                KnowledgeCreatedEvent.class,
                false,
                e -> instances2.add(e.getKnowledge())
        );

        Knowledge k1 = service.newKnowledge();
        Knowledge k2 = service.newKnowledge();
        Knowledge k3 = service.newKnowledge();

        assert instances1.containsAll(instances2);
        assert instances1.size() == 3;

        // Unsubscribe only one and test again
        subscription1.cancel();
        subscription1.cancel(); // Make sure no exceptions are thrown
        subscription1.cancel(); // Make sure no exceptions are thrown

        service.newKnowledge();
        service.newKnowledge();

        // The state should be the same
        assert instances1.containsAll(Arrays.asList(k1, k2, k3));
        assert instances1.size() == 3;

        // But not for the second subscription
        assert instances2.size() == 5;

        subscription2.cancel();
        service.newKnowledge();

        // The same state
        assert instances1.size() == 3;
        assert instances2.size() == 5;

    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void separationOfContext(boolean async) {
        KnowledgeService service = new KnowledgeService();

        Collection<RuleSession<?>> createdSessions = Collections.synchronizedList(new LinkedList<>());
        Collection<Knowledge> createdKnowledge = new LinkedList<>();

        // Root level subscription
        Set<RuleSession<?>> instancesRoot = Collections.synchronizedSet(new HashSet<>());
        Events.Subscription rootSub = service.subscribe(
                SessionCreatedEvent.class,
                async,
                e -> instancesRoot.add(e.getSession())
        );

        Knowledge k1 = service.newKnowledge();
        createdKnowledge.add(k1);

        Set<RuleSession<?>> instancesK1 = Collections.synchronizedSet(new HashSet<>());
        Events.Subscription k1Sub = k1.subscribe(
                SessionCreatedEvent.class,
                async,
                e -> instancesK1.add(e.getSession())
        );
        // Create 2 new sessions
        createdSessions.add(k1.newStatelessSession());
        createdSessions.add(k1.newStatelessSession());


        Knowledge k2 = service.newKnowledge();
        createdKnowledge.add(k2);

        Set<RuleSession<?>> instancesK2 = Collections.synchronizedSet(new HashSet<>());
        Events.Subscription k2Sub = k2.subscribe(
                SessionCreatedEvent.class,
                async,
                e -> instancesK2.add(e.getSession())
        );
        // Create 3 new sessions
        createdSessions.add(k2.newStatelessSession());
        createdSessions.add(k2.newStatelessSession());
        createdSessions.add(k2.newStatelessSession());

        TestUtils.sleep(300L);

        assert instancesRoot.size() == 5;
        assert instancesK1.size() == 2;
        assert instancesK2.size() == 3;

        // Cancel the first subscription and try again
        k1Sub.cancel();
        createdSessions.add(k1.newStatelessSession());
        createdSessions.add(k1.newStatelessSession());
        createdSessions.add(k2.newStatelessSession());
        createdSessions.add(k2.newStatelessSession());
        createdSessions.add(k2.newStatelessSession());

        TestUtils.sleep(300L);

        assert instancesK1.size() == 2; // No changes
        assert instancesK2.size() == 6; // Increased by 3
        assert instancesRoot.size() == 10; // Increased by 5

        // Cancel the second subscription and try again
        k2Sub.cancel();
        createdSessions.add(k1.newStatelessSession());
        createdSessions.add(k1.newStatelessSession());

        createdSessions.add(k2.newStatelessSession());
        createdSessions.add(k2.newStatelessSession());
        createdSessions.add(k2.newStatelessSession());

        TestUtils.sleep(300L);

        assert instancesK1.size() == 2; // No changes
        assert instancesK2.size() == 6; // No changes
        assert instancesRoot.size() == 15; // Increased by 5

        // Cancel the root subscription and retry
        rootSub.cancel();
        createdSessions.add(k1.newStatelessSession());
        createdSessions.add(k1.newStatelessSession());
        createdSessions.add(k1.newStatelessSession());

        createdSessions.add(k2.newStatelessSession());
        createdSessions.add(k2.newStatelessSession());
        createdSessions.add(k2.newStatelessSession());

        TestUtils.sleep(300L);

        assert instancesK1.size() == 2; // No changes
        assert instancesK2.size() == 6; // No changes
        assert instancesRoot.size() == 15; // No changes

        // Now that everything's cancelled, we should see no listeners in the event bus's internals.
        // 1. Checking the root message bus
        assertMessageBusHasNoSubscriptions(service.getMessageBus());

        // 2. Checking the ones of created Knowledge instances
        createdKnowledge.forEach(knowledge -> {
            KnowledgeRuntime k = (KnowledgeRuntime) knowledge;
            assertMessageBusHasNoSubscriptions(k.getMessageBus());
        });

        // 3. Checking the ones of created Session instances
        createdSessions.forEach(session -> {
            AbstractRuleSession<?> k = (AbstractRuleSession<?>) session;
            assertMessageBusHasNoSubscriptions(k.getMessageBus());
        });

        // 4. Test the session close event
        Events.Subscription last = service.subscribe(
                SessionClosedEvent.class,
                async,
                event -> createdSessions.remove(event.getSession())
        );

        new ArrayList<>(createdSessions).forEach(session -> {
            AbstractRuleSession<?> s = (AbstractRuleSession<?>) session;
            s.closeInner();
        });

        TestUtils.sleep(500L);

        Assertions.assertEquals(0, createdSessions.size());
        last.cancel();

        // Close the service
        service.shutdown();
    }

    @Test
    void envListenerTest() {
        KnowledgeService service = new KnowledgeService();
        try {
            Knowledge knowledge = service.newKnowledge();
            AtomicInteger value = new AtomicInteger(0);
            knowledge.subscribe(
                    EnvironmentChangeEvent.class,
                    false,
                    event -> event.applyIf(
                            "PROP",
                            Integer.class, value::set
                    )
            );

            

            knowledge.set("PROP", 123);

            assert value.get() == 123;

        } finally {
            service.shutdown();
        }

    }

    static void assertMessageBusHasNoSubscriptions(EventMessageBus bus) {
        bus.getHandlers().forEach((type, handler) -> {
            int count = handler.totalLocalSubscriptions();
            assert count == 0 : "Expected zero subscriptions, actual " + count + " of type: " + type;
        });
    }
}
