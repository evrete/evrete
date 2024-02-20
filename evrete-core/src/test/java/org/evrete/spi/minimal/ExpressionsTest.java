package org.evrete.spi.minimal;

import org.evrete.Configuration;
import org.evrete.KnowledgeService;
import org.evrete.api.StatefulSession;
import org.evrete.runtime.KnowledgeRuntime;
import org.evrete.util.NextIntSupplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("ALL")
class ExpressionsTest {
    private static KnowledgeService service;
    private KnowledgeRuntime knowledge;

    @BeforeAll
    static void setUpClass() {
        service = new KnowledgeService(new Configuration());
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
    void testNestedFields1() {
        NextIntSupplier counter = new NextIntSupplier();
        StatefulSession session = knowledge.builder()
                .newRule()
                .forEach("$o", Nested1.class)
                .where("$o.parent.parent.id > 0")
                .where("$o.id > 2")
                .execute(ctx -> counter.next())
                .build()
                .newStatefulSession();


        Nested1 level1 = new Nested1(10);
        Nested1 level2 = new Nested1(level1, 10);
        Nested1 level3_1 = new Nested1(level2, 5);
        Nested1 level3_2 = new Nested1(level2, 1);

        session.insertAndFire(level3_1, level3_2);

        assert counter.get() == 1 : "Actual: " + counter.get();
    }

    @Test
    void testNestedFields2() {
        NextIntSupplier counter = new NextIntSupplier();
        StatefulSession session = knowledge.builder()
                .newRule()
                .forEach("$o", NestedB.class)
                .where("$o.parent.ida > 0")
                .where("$o.idb > 2")
                .execute(ctx -> counter.next())
                .build()
                .newStatefulSession();


        NestedA level1 = new NestedA(10);
        NestedB level2_1 = new NestedB(level1, 5);
        NestedB level2_2 = new NestedB(level1, 1);

        session.insertAndFire(level2_1, level2_2);

        assert counter.get() == 1 : "Actual: " + counter.get();
    }

    @Test
    void testThisFields2() {
        NextIntSupplier counter = new NextIntSupplier();
        StatefulSession session = knowledge
                .builder()
                .newRule()
                .forEach(
                        "$i1", Integer.class,
                        "$i2", Integer.class
                )
                .where("$i1 > $i2")
                .execute(ctx -> counter.next())
                .build()
                .newStatefulSession();


        session.insertAndFire(1, 2);

        assert counter.get() == 1 : "Actual: " + counter.get();
    }

    @Test
    void testRepeatedReference() {
        NextIntSupplier counter = new NextIntSupplier();
        StatefulSession session = knowledge.builder()
                .newRule()
                .forEach(
                        "$i", Integer.class
                )
                .where("$i > 0 || $i < 0")
                .execute(ctx -> counter.next())
                .build()
                .newStatefulSession();


        session.insertAndFire(1, 2);

        assert counter.get() == 2 : "Actual: " + counter.get();
    }


    public static class Nested1 {
        public final Nested1 parent;
        public final int id;

        public Nested1(Nested1 parent, int id) {
            this.parent = parent;
            this.id = id;
        }

        public Nested1(int id) {
            this(null, id);
        }
    }

    public static class NestedA {
        public final int ida;

        public NestedA(int id) {
            this.ida = id;
        }
    }

    public static class NestedB {
        public final NestedA parent;
        public final int idb;

        public NestedB(NestedA parent, int id) {
            this.parent = parent;
            this.idb = id;
        }
    }

}
