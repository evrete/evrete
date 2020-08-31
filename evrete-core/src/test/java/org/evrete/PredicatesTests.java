package org.evrete;

import org.evrete.api.StatefulSession;
import org.evrete.api.ValuesPredicate;
import org.evrete.classes.TypeA;
import org.evrete.classes.TypeB;
import org.evrete.classes.TypeC;
import org.evrete.classes.TypeD;
import org.evrete.helper.RhsAssert;
import org.evrete.runtime.KnowledgeImpl;
import org.evrete.runtime.StatefulSessionImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static org.evrete.api.FactBuilder.fact;

class PredicatesTests {
    private static KnowledgeService service;
    private KnowledgeImpl knowledge;

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
        knowledge = (KnowledgeImpl) service.newKnowledge();
    }

    @Test
    void testSingleFinalNode1() {

        Predicate<Object[]> sharedPredicate = objects -> {
            int i1 = (int) objects[0];
            int i2 = (int) objects[1];
            return i1 != i2;
        };

        knowledge.newRule("testSingleFinalNode1")
                .forEach(
                        fact("$a", TypeA.class),
                        fact("$b", TypeB.class),
                        fact("$c", TypeC.class),
                        fact("$d", TypeD.class)
                )
                .where(sharedPredicate, "$a.i", "$b.i")
                .where(sharedPredicate, "$a.i", "$c.i")
                .where(sharedPredicate, "$a.i", "$d.i")
                .execute();


        StatefulSession s = knowledge.createSession();
        RhsAssert rhsAssert = new RhsAssert(s);

        int ai = new Random().nextInt(20) + 1;
        int bi = new Random().nextInt(20) + 1;
        int ci = new Random().nextInt(20) + 1;
        int di = new Random().nextInt(20) + 1;

        AtomicInteger id = new AtomicInteger(0);

        for (int i = 0; i < ai; i++) {
            int n = id.incrementAndGet();
            TypeA obj = new TypeA(String.valueOf(n));
            obj.setI(n);
            s.insert(obj);
        }

        for (int i = 0; i < bi; i++) {
            int n = id.incrementAndGet();
            TypeB obj = new TypeB(String.valueOf(n));
            obj.setI(n);
            s.insert(obj);
        }

        for (int i = 0; i < ci; i++) {
            int n = id.incrementAndGet();
            TypeC obj = new TypeC(String.valueOf(n));
            obj.setI(n);
            s.insert(obj);
        }

        for (int i = 0; i < di; i++) {
            int n = id.incrementAndGet();
            TypeD obj = new TypeD(String.valueOf(n));
            obj.setI(n);
            s.insert(obj);
        }

        s.fire();

        rhsAssert
                .assertUniqueCount("$a", ai)
                .assertUniqueCount("$b", bi)
                .assertUniqueCount("$c", ci)
                .assertUniqueCount("$d", di)
                .assertCount(ai * bi * ci * di);

    }

    @Test
    void testCircularMultiFinal() {

        ValuesPredicate p1 = values -> {
            int ai = (int) values.apply(0);
            int bi = (int) values.apply(1);
            return ai == bi;
        };

        Predicate<Object[]> p2 = values -> {
            long cl = (long) values[0];
            long bl = (long) values[1];
            return cl == bl;
        };

        ValuesPredicate p3 = values -> {
            int ci = (int) values.apply(0);
            long al = (long) values.apply(1);
            return ci == al;
        };


        knowledge.newRule("test circular")
                .forEach(
                        fact("$a", TypeA.class),
                        fact("$b", TypeB.class),
                        fact("$c", TypeC.class)
                )
                .where("$a.i == $b.i")
                .where(p1, "$a.i", "$b.i")
                .where(p2, "$c.l", "$b.l")
                .where(p3, "$c.i", "$a.l")
                .execute();

        StatefulSessionImpl s = knowledge.createSession();

        TypeA a = new TypeA("A");
        a.setI(1);
        a.setL(1);

        TypeA aa = new TypeA("AA");
        aa.setI(2);
        aa.setL(2);

        TypeA aaa = new TypeA("AAA");
        aaa.setI(3);
        aaa.setL(3);

        TypeB b = new TypeB("B");
        b.setI(1);
        b.setL(1);

        TypeB bb = new TypeB("BB");
        bb.setI(2);
        bb.setL(2);

        TypeB bbb = new TypeB("BBB");
        bbb.setI(3);
        bbb.setL(3);

        TypeC c = new TypeC("C");
        c.setI(1);
        c.setL(1);

        TypeC cc = new TypeC("CC");
        cc.setI(2);
        cc.setL(2);

        TypeC ccc = new TypeC("CCC");
        ccc.setI(3);
        ccc.setL(3);

        RhsAssert rhsAssert = new RhsAssert(s, "test circular");

        s.insertAndFire(a, aa, aaa);
        s.insertAndFire(b, bb, bbb);
        s.insertAndFire(c, cc, ccc);

        rhsAssert.assertCount(3);
    }

    @Test
    void testMultiFinal2_mini() {
        String ruleName = "testMultiFinal2_mini";

        knowledge.newRule(ruleName)
                .forEach(
                        fact("$a", TypeA.class),
                        fact("$b", TypeB.class),
                        fact("$c", TypeC.class)
                )
                .where((Predicate<Object[]>) values -> {
                    int i1 = (int) values[0];
                    int i2 = (int) values[1];
                    return i1 == i2;
                }, "$a.i", "$b.i")
                .where((Predicate<Object[]>) values -> {
                    long i1 = (long) values[0];
                    long i2 = (long) values[1];
                    return i1 == i2;
                }, "$c.l", "$b.l")
                .execute();

        StatefulSession s = knowledge.createSession();

        TypeA a = new TypeA("AA");
        a.setI(1);

        TypeB b = new TypeB("BB");
        b.setI(1);
        b.setL(1);

        TypeC c = new TypeC("CC");
        c.setL(1);


        RhsAssert rhsAssert = new RhsAssert(s);

        s.getRule(ruleName)
                .setRhs(null) // RHS can be overridden
                .setRhs(rhsAssert);

        s.insertAndFire(a, b, c);
        rhsAssert.assertCount(1).reset();

        //Second insert
        TypeA aa = new TypeA("A");
        aa.setI(2);
        aa.setL(2);

        TypeB bb = new TypeB("B");
        bb.setI(2);
        bb.setL(2);

        TypeC cc = new TypeC("C");
        cc.setI(2);
        cc.setL(2);

        s.insertAndFire(aa, bb, cc);
        rhsAssert.assertCount(1);

        s.close();
    }

    @Test
    void testFields() {
        String ruleName = "testMultiFields";

        // "$a.i * $b.l * $b.s == $a.l"
        ValuesPredicate predicate = v -> {
            int ai = (int) v.apply(0);
            long bl = (long) v.apply(1);
            short bs = (short) v.apply(2);
            long al = (long) v.apply(3);
            return ai * bl * bs == al;
        };

        knowledge.newRule(ruleName)
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class
                )
                .where(predicate, "$a.i", "$b.l", "$b.s", "$a.l")
                .execute(null);

        StatefulSession s = knowledge.createSession();

        TypeA a1 = new TypeA("A1");
        a1.setI(2);
        a1.setL(30L);

        TypeB b1 = new TypeB("B1");
        b1.setL(3);
        b1.setS((short) 5);

        RhsAssert rhsAssert = new RhsAssert(s, ruleName);

        s.insertAndFire(a1, b1);
        rhsAssert.assertCount(1).reset();

        //Second insert
        TypeA a2 = new TypeA("A2");
        a2.setI(7);
        a2.setL(693L);

        TypeB b2 = new TypeB("B2");
        b2.setL(9);
        b2.setS((short) 11);

        s.insertAndFire(a2, b2);
        rhsAssert.assertCount(1);

        s.close();
    }


    @Test
    void testAlphaBeta1() {
        RhsAssert rhsAssert1 = new RhsAssert(
                "$a", TypeA.class,
                "$b", TypeB.class
        );

        RhsAssert rhsAssert2 = new RhsAssert(
                "$a", TypeA.class,
                "$b", TypeB.class
        );


        ValuesPredicate rule1_1 = v -> {
            int ai = (int) v.apply(0);
            int bi = (int) v.apply(1);
            return ai != bi;
        };
        ValuesPredicate rule1_2 = v -> {
            double ad = (double) v.apply(0);
            return ad > 1;
        };
        ValuesPredicate rule1_3 = v -> {
            int bi = (int) v.apply(0);
            return bi > 10;
        };


        ValuesPredicate rule2_1 = v -> {
            int ai = (int) v.apply(0);
            int bi = (int) v.apply(1);
            return ai != bi;
        };

        ValuesPredicate rule2_2 = v -> {
            int ai = (int) v.apply(0);
            return ai < 3;
        };

        ValuesPredicate rule2_3 = v -> {
            float bf = (float) v.apply(0);
            return bf < 10;
        };

        knowledge.newRule("test alpha 1")
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class
                )
                .where(rule1_1, "$a.i", "$b.i")
                .where(rule1_2, "$a.d")
                .where(rule1_3, "$b.i")
                .execute(rhsAssert1)
                .newRule("test alpha 2")
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class
                )
                .where(rule2_1, "$a.i", "$b.i")
                .where(rule2_2, "$a.i")
                .where(rule2_3, "$b.f")
                .execute(rhsAssert2);

        StatefulSessionImpl s = knowledge.createSession();

        TypeA a = new TypeA("A");
        a.setAllNumeric(0);

        TypeA aa = new TypeA("AA");
        aa.setAllNumeric(2);

        TypeA aaa = new TypeA("AAA");
        aaa.setAllNumeric(3);

        TypeB b = new TypeB("B");
        b.setAllNumeric(9);

        TypeB bb = new TypeB("BB");
        bb.setAllNumeric(100);

        s.insertAndFire(a, aa, aaa, b, bb);

        rhsAssert1
                .assertCount(2)
                .assertUniqueCount("$b", 1)
                .assertContains("$b", bb)
                .assertUniqueCount("$a", 2)
                .assertContains("$a", aa)
                .assertContains("$a", aaa)
                .reset();

        rhsAssert2
                .assertCount(2)
                .assertUniqueCount("$b", 1)
                .assertContains("$b", b)
                .assertUniqueCount("$a", 2)
                .assertContains("$a", a)
                .assertContains("$a", aa)
                .reset();
    }


    @Test
    void testUniType2() {
        Set<String> collectedJoinedIds = new HashSet<>();

        ValuesPredicate predicate = v -> {
            int i1 = (int) v.apply(0);
            int i2 = (int) v.apply(1);
            int i3 = (int) v.apply(2);

            return i1 * i2 == i3;
        };
        knowledge.newRule("test uni 2")
                .forEach(
                        "$a1", TypeA.class,
                        "$a2", TypeA.class,
                        "$a3", TypeA.class
                )
                .where(predicate, "$a1.i", "$a2.i", "$a3.i")
                .execute(
                        ctx -> {
                            TypeA a1 = ctx.get("$a1");
                            TypeA a2 = ctx.get("$a2");
                            TypeA a3 = ctx.get("$a3");
                            collectedJoinedIds.add(a1.getId() + a2.getId() + a3.getId());
                        }
                );

        StatefulSessionImpl s = knowledge.createSession();

        TypeA a1 = new TypeA("A3");
        a1.setI(3);

        TypeA a2 = new TypeA("A5");
        a2.setI(5);

        s.insertAndFire(a1, a2);
        assert collectedJoinedIds.isEmpty();


        for (int i = 0; i < 20; i++) {
            TypeA misc = new TypeA();
            misc.setI(i + 1000);
            s.insertAndFire(misc);
        }

        assert collectedJoinedIds.isEmpty();

        TypeA a3 = new TypeA("A15");
        a3.setI(15);

        s.insertAndFire(a3);
        assert collectedJoinedIds.size() == 2 : "Actual data: " + collectedJoinedIds;
        assert collectedJoinedIds.contains("A3A5A15") : "Actual: " + collectedJoinedIds;
        assert collectedJoinedIds.contains("A5A3A15");


        TypeA a7 = new TypeA("A7");
        a7.setI(7);

        TypeA a11 = new TypeA("A11");
        a11.setI(11);

        TypeA a77 = new TypeA("A77");
        a77.setI(77);

        s.insertAndFire(a7, a11, a77);
        assert collectedJoinedIds.size() == 4 : "Actual " + collectedJoinedIds;
        assert collectedJoinedIds.contains("A3A5A15");
        assert collectedJoinedIds.contains("A5A3A15");
        assert collectedJoinedIds.contains("A7A11A77");
        assert collectedJoinedIds.contains("A11A7A77");

    }


    @Test
    void testAlpha1() {
        RhsAssert rhsAssert = new RhsAssert(
                "$a", TypeA.class,
                "$b", TypeB.class,
                "$c", TypeC.class
        );

        ValuesPredicate p1 = v -> {
            int i1 = (int) v.apply(0);
            return i1 > 4;
        };

        ValuesPredicate p2 = v -> {
            int i1 = (int) v.apply(0);
            return i1 > 3;
        };

        knowledge.newRule()
                .forEach(
                        fact("$a", TypeA.class),
                        fact("$b", TypeB.class),
                        fact("$c", TypeC.class)
                )
                .where(p1, "$a.i")
                .where(p2, "$b.i")
                .execute(rhsAssert);

        StatefulSession s = knowledge.createSession();

        // This insert cycle will result in 5x6 = 30 matching pairs of [A,B]
        for (int i = 0; i < 10; i++) {
            TypeA a = new TypeA("A" + i);
            a.setI(i);
            TypeB b = new TypeB("B" + i);
            b.setI(i);
            s.insert(a, b);
        }
        s.fire();
        TypeC c1 = new TypeC("C");
        TypeC c2 = new TypeC("C");
        rhsAssert.assertCount(0).reset();
        s.insert(c1);
        s.fire();
        rhsAssert
                .assertCount(30)
                .assertContains("$c", c1)
                .reset();
        s.insert(c2);
        s.fire();
        rhsAssert
                .assertCount(30)
                .assertNotContains("$c", c1)
                .assertContains("$c", c2);
    }

    @Test
    void testAlpha2() {
        RhsAssert rhsAssert = new RhsAssert(
                "$a", TypeA.class,
                "$b", TypeB.class,
                "$c", TypeC.class
        );

        ValuesPredicate p1 = v -> {
            int i1 = (int) v.apply(0);
            return i1 > 4;
        };

        ValuesPredicate p2 = v -> {
            int i1 = (int) v.apply(0);
            return i1 > 3;
        };

        ValuesPredicate p3 = v -> {
            int i1 = (int) v.apply(0);
            return i1 > 6;
        };


        knowledge.newRule()
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class,
                        "$c", TypeC.class
                )
                .where(p1, "$a.i")
                .where(p2, "$b.i")
                .where(p3, "$c.i")
                .execute(rhsAssert)
                ;

        StatefulSession s = knowledge.createSession();

        // This insert cycle will result in 5x6 = 30 matching pairs of [A,B]
        for (int i = 0; i < 10; i++) {
            TypeA a = new TypeA("A" + i);
            a.setI(i);
            TypeB b = new TypeB("B" + i);
            b.setI(i);
            s.insert(a, b);
        }
        s.fire();
        rhsAssert.assertCount(0).reset(); // No instances of TypeC

        // This insert cycle will result in 3 matching pairs of C (7,8,9)
        for (int i = 0; i < 10; i++) {
            TypeC c = new TypeC("C" + i);
            c.setI(i);
            s.insert(c);
        }
        s.fire();
        rhsAssert
                .assertCount(30 * 3)
                .assertUniqueCount("$a", 5)
                .assertUniqueCount("$b", 6)
                .assertUniqueCount("$c", 3);
    }
}
