package org.evrete.spi.minimal;

import org.evrete.KnowledgeService;
import org.evrete.api.FactHandle;
import org.evrete.api.KeyMode;
import org.evrete.classes.TypeA;
import org.evrete.runtime.KnowledgeRuntime;
import org.evrete.runtime.StatefulSessionImpl;
import org.evrete.runtime.TypeMemory;
import org.evrete.util.NextIntSupplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultFactStorageTest {
    private static KnowledgeService service;
    private KnowledgeRuntime knowledge;

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
        knowledge = (KnowledgeRuntime) service.newKnowledge();
    }

    @Test
    void update() {
        NextIntSupplier counter = new NextIntSupplier();
        StatefulSessionImpl session = (StatefulSessionImpl) knowledge
                .newRule()
                .forEach("$a", TypeA.class)
                .where("$a.i >= 0")
                .execute(ctx -> counter.next())
                .createSession();

        TypeA a = new TypeA();
        a.setAllNumeric(0);
        FactHandle h = session.insert(a);
        session.fire();
        int updates = 1;
        for (int i = 0; i < updates; i++) {
            a.setI(i);
            session.update(h, a);
            session.fire();
        }

        TypeMemory tm = session.getMemory().get(0);

        // Checking fact storage
        DefaultFactStorage<?> factStorage = (DefaultFactStorage<?>) tm.getFactStorage();
        assert factStorage.size() == 1; // only initial instance should be in the storage
        assert counter.get() == updates + 1;

        // Memory key storage
        SharedAlphaData bucket = (SharedAlphaData) tm.getMemoryBuckets().get(0).getFieldData();

        LinkedFactHandles main = bucket.get(KeyMode.OLD_OLD);
        LinkedFactHandles delta1 = bucket.get(KeyMode.OLD_NEW);
        LinkedFactHandles delta2 = bucket.get(KeyMode.NEW_NEW);

        assert main.iterator().reset() == 1 : " Actual: " + main;
        assert delta1.size() == 0;
        assert delta2.size() == 0;

    }
}