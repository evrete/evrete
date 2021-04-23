package org.evrete.spi.minimal;

import org.evrete.KnowledgeService;
import org.evrete.runtime.KnowledgeRuntime;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

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

/*
    @Test
    void update() {
        NextIntSupplier counter = new NextIntSupplier();
        StatefulSessionImpl session = (StatefulSessionImpl) knowledge
                .newRule()
                .forEach("$a", TypeA.class)
                .where("$a.i >= 0")
                .execute(new Consumer<RhsContext>() {
                    @Override
                    public void accept(RhsContext ctx) {
                        counter.next();
                    }
                })
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
        SharedAlphaData bucket = (SharedAlphaData) tm.getBetaMemories().get(0).getAlphaBuckets().get(0).getFieldData();

        LinkedFactHandles main = bucket.get(KeyMode.MAIN);
        LinkedFactHandles delta1 = bucket.get(KeyMode.KNOWN_UNKNOWN);
        LinkedFactHandles delta2 = bucket.get(KeyMode.UNKNOWN_UNKNOWN);

        assert main.iterator().reset() == 1 : " Actual: " + main.size();
        assert delta1.size() == 0;
        assert delta2.size() == 0;
    }
*/
}