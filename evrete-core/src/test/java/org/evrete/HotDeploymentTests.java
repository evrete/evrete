package org.evrete;

import org.evrete.api.ActivationMode;
import org.evrete.api.RuntimeRule;
import org.evrete.api.StatefulSession;
import org.evrete.api.ValuesPredicate;
import org.evrete.classes.TypeA;
import org.evrete.classes.TypeB;
import org.evrete.classes.TypeC;
import org.evrete.classes.TypeD;
import org.evrete.helper.RhsAssert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static org.evrete.api.FactBuilder.fact;

class HotDeploymentTests {
    private static KnowledgeService service;
    private StatefulSession session;

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
        session = service.newKnowledge().createSession();
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void plainTest0(ActivationMode mode) {
        session.setActivationMode(mode);
        RhsAssert rhsAssert = new RhsAssert("$n", Integer.class);
        session.newRule()
                .forEach("$n", Integer.class)
                .execute(rhsAssert);

        session.insertAndFire(1, 2);
        rhsAssert.assertCount(2).reset();
        session.insertAndFire(3);
        rhsAssert.assertCount(1).assertContains("$n", 3);
        session.close();
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void plainTest1(ActivationMode mode) {
        session.setActivationMode(mode);
        RhsAssert rhsAssert = new RhsAssert("$n", Integer.class);
        session.newRule()
                .forEach("$n", Integer.class)
                .where("$n.intValue >= 0 ")
                .execute(rhsAssert);

        session.insertAndFire(1, 2);
        rhsAssert.assertCount(2).reset();
        session.insertAndFire(3);
        rhsAssert.assertCount(1).assertContains("$n", 3).reset();

        session.fire();
        rhsAssert.assertCount(0);
        session.insertAndFire(-1);
        rhsAssert.assertCount(0);
        session.close();
    }


    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void namingTest(ActivationMode mode) {
        session.setActivationMode(mode);
        session.newRule("A").forEach("$a", String.class).execute();
        RuntimeRule a = session.getRule("A");
        assert a != null;
        Assertions.assertThrows(RuntimeException.class,
                () -> session
                        .newRule("A") // Same name
                        .forEach("$a", String.class)
                        .execute()
        );
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void testSingleFinalNode1(ActivationMode mode) {
        session.setActivationMode(mode);

        Predicate<Object[]> sharedPredicate = objects -> {
            int i1 = (int) objects[0];
            int i2 = (int) objects[1];
            return i1 != i2;
        };

        session.newRule("testSingleFinalNode1")
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

        RhsAssert rhsAssert = new RhsAssert(session);

        int ai = new Random().nextInt(20) + 1;
        int bi = new Random().nextInt(20) + 1;
        int ci = new Random().nextInt(20) + 1;
        int di = new Random().nextInt(20) + 1;

        AtomicInteger id = new AtomicInteger(0);

        for (int i = 0; i < ai; i++) {
            int n = id.incrementAndGet();
            TypeA obj = new TypeA(String.valueOf(n));
            obj.setI(n);
            session.insert(obj);
        }

        for (int i = 0; i < bi; i++) {
            int n = id.incrementAndGet();
            TypeB obj = new TypeB(String.valueOf(n));
            obj.setI(n);
            session.insert(obj);
        }

        for (int i = 0; i < ci; i++) {
            int n = id.incrementAndGet();
            TypeC obj = new TypeC(String.valueOf(n));
            obj.setI(n);
            session.insert(obj);
        }

        for (int i = 0; i < di; i++) {
            int n = id.incrementAndGet();
            TypeD obj = new TypeD(String.valueOf(n));
            obj.setI(n);
            session.insert(obj);
        }

        session.fire();

        rhsAssert
                .assertUniqueCount("$a", ai)
                .assertUniqueCount("$b", bi)
                .assertUniqueCount("$c", ci)
                .assertUniqueCount("$d", di)
                .assertCount(ai * bi * ci * di);

    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void testCircularMultiFinal(ActivationMode mode) {
        session.setActivationMode(mode);

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


        session.newRule("test circular")
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

        RhsAssert rhsAssert = new RhsAssert(session, "test circular");

        session.insertAndFire(a, aa, aaa);
        session.insertAndFire(b, bb, bbb);
        session.insertAndFire(c, cc, ccc);

        rhsAssert.assertCount(3);
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void testMultiFinal2_mini(ActivationMode mode) {
        String ruleName = "testMultiFinal2_mini";
        session.setActivationMode(mode);

        session.newRule(ruleName)
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


        TypeA a = new TypeA("AA");
        a.setI(1);

        TypeB b = new TypeB("BB");
        b.setI(1);
        b.setL(1);

        TypeC c = new TypeC("CC");
        c.setL(1);

        RhsAssert rhsAssert = new RhsAssert(session);

        session.getRule(ruleName)
                .setRhs(rhsAssert); // RHS can be overridden

        session.insertAndFire(a, b, c);
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

        session.insertAndFire(aa, bb, cc);
        rhsAssert.assertCount(1);

        session.close();
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void testFields(ActivationMode mode) {
        String ruleName = "testMultiFields";
        session.setActivationMode(mode);

        // "$a.i * $b.l * $b.s == $a.l"
        ValuesPredicate predicate = v -> {
            int ai = (int) v.apply(0);
            long bl = (long) v.apply(1);
            short bs = (short) v.apply(2);
            long al = (long) v.apply(3);
            return ai * bl * bs == al;
        };

        session.newRule(ruleName)
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class
                )
                .where(predicate, "$a.i", "$b.l", "$b.s", "$a.l")
                .execute();


        TypeA a1 = new TypeA("A1");
        a1.setI(2);
        a1.setL(30L);

        TypeB b1 = new TypeB("B1");
        b1.setL(3);
        b1.setS((short) 5);

        RhsAssert rhsAssert = new RhsAssert(session, ruleName);
        session.insertAndFire(a1, b1);
        rhsAssert.assertCount(1).reset();

        //Second insert
        TypeA a2 = new TypeA("A2");
        a2.setI(7);
        a2.setL(693L);

        TypeB b2 = new TypeB("B2");
        b2.setL(9);
        b2.setS((short) 11);

        session.insertAndFire(a2, b2);
        rhsAssert.assertCount(1);

        session.close();
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void testMixed1(ActivationMode mode) {
        session.setActivationMode(mode);

        RhsAssert rhsAssert = new RhsAssert(
                "$a1", TypeA.class,
                "$a2", TypeA.class,
                "$b1", TypeB.class,
                "$b2", TypeB.class,
                "$c", TypeC.class,
                "$d", TypeD.class
        );

        session.newRule("test alpha 1")
                .forEach(
                        "$a1", TypeA.class,
                        "$b1", TypeB.class,
                        "$a2", TypeA.class,
                        "$b2", TypeB.class,
                        "$c", TypeC.class,
                        "$d", TypeD.class
                )
                .where("$a1.i != $b1.i")
                .where("$a2.i != $b2.i")
                .where("$c.i > 0")
                .execute(rhsAssert);


        TypeA a1 = new TypeA("A1");
        a1.setI(1);

        TypeA a2 = new TypeA("A2");
        a2.setI(1);

        TypeB b1 = new TypeB("B1");
        b1.setI(10);

        TypeB b2 = new TypeB("B2");
        b2.setI(10);

        TypeC c1 = new TypeC("C1");
        c1.setI(-1);

        TypeD d1 = new TypeD("D1");

        session.insertAndFire(a1, b1, a2, b2, c1, d1);
        rhsAssert.assertCount(0).reset();

        TypeC c2 = new TypeC("C2");
        c2.setI(1);
        session.insertAndFire(c2);

        rhsAssert.assertCount(4 * 4).reset();

        TypeC c3 = new TypeC("C3");
        c3.setI(100);
        session.insertAndFire(c3);
        rhsAssert.assertCount(4 * 4).reset();

        TypeD d2 = new TypeD("D2");
        session.insertAndFire(d2);
        rhsAssert.assertCount(2 * 4 * 4);
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void testAlphaBeta1(ActivationMode mode) {
        session.setActivationMode(mode);

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

        session.newRule("test alpha 1")
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
                .execute(rhsAssert2)
        ;

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


        session.insertAndFire(a, aa, aaa, b, bb);

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

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void testUniType2(ActivationMode mode) {
        session.setActivationMode(mode);

        Set<String> collectedJoinedIds = new HashSet<>();

        ValuesPredicate predicate = v -> {
            int i1 = (int) v.apply(0);
            int i2 = (int) v.apply(1);
            int i3 = (int) v.apply(2);

            return i1 * i2 == i3;
        };
        session.newRule("test uni 2")
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


        TypeA a1 = new TypeA("A3");
        a1.setI(3);

        TypeA a2 = new TypeA("A5");
        a2.setI(5);

        session.insertAndFire(a1, a2);
        assert collectedJoinedIds.isEmpty();


        for (int i = 0; i < 20; i++) {
            TypeA misc = new TypeA();
            misc.setI(i + 1000);
            session.insertAndFire(misc);
        }

        assert collectedJoinedIds.isEmpty();

        TypeA a3 = new TypeA("A15");
        a3.setI(15);

        session.insertAndFire(a3);
        assert collectedJoinedIds.size() == 2 : "Actual data: " + collectedJoinedIds;
        assert collectedJoinedIds.contains("A3A5A15") : "Actual: " + collectedJoinedIds;
        assert collectedJoinedIds.contains("A5A3A15");


        TypeA a7 = new TypeA("A7");
        a7.setI(7);

        TypeA a11 = new TypeA("A11");
        a11.setI(11);

        TypeA a77 = new TypeA("A77");
        a77.setI(77);

        session.insertAndFire(a7, a11, a77);
        assert collectedJoinedIds.size() == 4 : "Actual " + collectedJoinedIds;
        assert collectedJoinedIds.contains("A3A5A15");
        assert collectedJoinedIds.contains("A5A3A15");
        assert collectedJoinedIds.contains("A7A11A77");
        assert collectedJoinedIds.contains("A11A7A77");

    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void testAlpha1(ActivationMode mode) {
        session.setActivationMode(mode);

        RhsAssert rhsAssert = new RhsAssert(
                "$a", TypeA.class,
                "$b", TypeB.class,
                "$c", TypeC.class
        );

        session.newRule()
                .forEach(
                        fact("$a", TypeA.class),
                        fact("$b", TypeB.class),
                        fact("$c", TypeC.class)
                )
                .where("$a.i > 4")
                .where("$b.i > 3")
                .execute(rhsAssert);


        // This insert cycle will result in 5x6 = 30 matching pairs of [A,B]
        for (int i = 0; i < 10; i++) {
            TypeA a = new TypeA("A" + i);
            a.setI(i);
            TypeB b = new TypeB("B" + i);
            b.setI(i);
            session.insert(a, b);
        }
        session.fire();
        TypeC c1 = new TypeC("C");
        TypeC c2 = new TypeC("C");
        rhsAssert.assertCount(0).reset();
        session.insert(c1);
        session.fire();
        rhsAssert
                .assertCount(30)
                .assertContains("$c", c1)
                .reset();
        session.insert(c2);
        session.fire();
        rhsAssert
                .assertCount(30)
                .assertNotContains("$c", c1)
                .assertContains("$c", c2);
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void testAlpha2(ActivationMode mode) {
        session.setActivationMode(mode);

        RhsAssert rhsAssert = new RhsAssert(
                "$a", TypeA.class,
                "$b", TypeB.class,
                "$c", TypeC.class
        );

        session.newRule()
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class,
                        "$c", TypeC.class
                )
                .where("$a.i > 4")
                .where("$b.i > 3")
                .where("$c.i > 6")
                .execute(rhsAssert);

        // This insert cycle will result in 5x6 = 30 matching pairs of [A,B]
        for (int i = 0; i < 10; i++) {
            TypeA a = new TypeA("A" + i);
            a.setI(i);
            TypeB b = new TypeB("B" + i);
            b.setI(i);
            session.insert(a, b);
        }
        session.fire();
        rhsAssert.assertCount(0).reset();

        // This insert cycle will result in 3 matching pairs of C (7,8,9)
        for (int i = 0; i < 10; i++) {
            TypeC c = new TypeC("C" + i);
            c.setI(i);
            session.insert(c);
        }
        session.fire();
        rhsAssert
                .assertCount(30 * 3)
                .assertUniqueCount("$a", 5)
                .assertUniqueCount("$b", 6)
                .assertUniqueCount("$c", 3);
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void testAlpha3(ActivationMode mode) {
        session.setActivationMode(mode);

        RhsAssert rhsAssert1 = new RhsAssert("$a", TypeA.class);
        RhsAssert rhsAssert2 = new RhsAssert("$a", TypeA.class);
        RhsAssert rhsAssert3 = new RhsAssert("$a", TypeA.class);

        session.newRule("rule 1")
                .forEach("$a", TypeA.class)
                .where("$a.i > 4")
                .execute(rhsAssert1)
                .newRule("rule 2")
                .forEach("$a", TypeA.class)
                .where("$a.i > 5")
                .execute(rhsAssert2)
                .newRule("rule 3")
                .forEach("$a", TypeA.class)
                .where("$a.i > 6")
                .execute(rhsAssert3);


        // This insert cycle will result in 5 matching As
        for (int i = 0; i < 10; i++) {
            TypeA a = new TypeA("A" + i);
            a.setI(i);
            session.insert(a);
        }
        session.fire();
        rhsAssert1.assertCount(5);
        rhsAssert2.assertCount(4);
        rhsAssert3.assertCount(3);
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void testAlpha4(ActivationMode mode) {
        session.setActivationMode(mode);

        // Rule 1
        ValuesPredicate p1_1 = v -> {
            int i1 = (int) v.apply(0);
            return i1 > 4;
        };

        ValuesPredicate p1_2 = v -> {
            int i1 = (int) v.apply(0);
            return i1 > 3;
        };

        session.newRule("rule 1")
                .forEach(
                        fact("$a", TypeA.class),
                        fact("$b", TypeB.class),
                        fact("$c", TypeC.class)
                )
                .where(p1_1, "$a.i")
                .where(p1_2, "$b.i")
                .execute();

        // This insert cycle will result in 5x6 = 30 matching pairs of [A,B]
        for (int i = 0; i < 10; i++) {
            TypeA a = new TypeA("A" + i);
            a.setI(i);
            TypeB b = new TypeB("B" + i);
            b.setI(i);
            session.insert(a, b);
        }
        RhsAssert rhsAssert1 = new RhsAssert(session, "rule 1");

        session.fire();
        rhsAssert1.assertCount(0).reset();

        TypeC c = new TypeC();

        session.insert(c);
        session.fire();
        rhsAssert1.assertCount(30).reset();


        // Adding another rule
        ValuesPredicate p2_1 = v -> {
            int i1 = (int) v.apply(0);
            return i1 > 3;
        };

        ValuesPredicate p2_2 = v -> {
            int i1 = (int) v.apply(0);
            return i1 > 2;
        };

        session.newRule("rule 2")
                .forEach(
                        fact("$a", TypeA.class),
                        fact("$b", TypeB.class),
                        fact("$c", TypeC.class)
                )
                .where(p2_1, "$a.i")
                .where(p2_2, "$b.i")
                .execute();
        RhsAssert rhsAssert2 = new RhsAssert(session, "rule 2");

        session.fire();
        rhsAssert2.assertCount(0).reset();

        session.deleteAndFire(c);
        rhsAssert1.assertCount(0).reset();
        rhsAssert2.assertCount(0).reset();

        session.insertAndFire(c);
        rhsAssert1.assertCount(6 * 5);
        rhsAssert2.assertCount(7 * 6);
    }


    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void testAlpha5(ActivationMode mode) {
        session.setActivationMode(mode);

        RhsAssert rhsAssert1 = new RhsAssert("$i", Integer.class);

        session.newRule("rule 1")
                .forEach("$i", Integer.class)
                .where("$i.intValue > 2")
                .execute(rhsAssert1);

        for (int i = 0; i < 10; i++) {
            session.insert(i);
        }
        session.fire();
        rhsAssert1.assertCount(7); //3,4,5,6,7,8,9

        // Another rule w/o alpha
        RhsAssert rhsAssert2 = new RhsAssert("$i", Integer.class);
        session.newRule("rule 2")
                .forEach("$i", Integer.class)
                .execute(rhsAssert2);
        session.fire();
        rhsAssert2.assertCount(0);

    }

    // An "inverse" version of the previous test
    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void testAlpha6(ActivationMode mode) {
        session.setActivationMode(mode);

        AtomicInteger ruleCounter1 = new AtomicInteger();

        session.newRule("rule 1")
                .forEach("$i", Integer.class)
                .execute(
                        ctx -> ruleCounter1.incrementAndGet()
                );

        for (int i = 0; i < 10; i++) {
            session.insert(i);
        }
        session.fire();
        assert ruleCounter1.get() == 10;

        // Another rule w/o alpha
        AtomicInteger ruleCounter2 = new AtomicInteger();
        session.newRule("rule 2")
                .forEach("$i", Integer.class)
                .where("$i.intValue > 2")
                .execute(
                        ctx -> ruleCounter2.incrementAndGet()
                );
        session.fire();
        assert ruleCounter2.get() == 0; //3,4,5,6,7,8,9
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void testMixedMulti(ActivationMode mode) {
        session.setActivationMode(mode);

        RhsAssert rhsAssert1 = new RhsAssert(
                "$a1", TypeA.class,
                "$b1", TypeB.class,
                "$a2", TypeA.class,
                "$b2", TypeB.class,
                "$c", TypeC.class,
                "$d", TypeD.class
        );

        RhsAssert rhsAssert2 = rhsAssert1.copyOf();
        RhsAssert rhsAssert3 = rhsAssert1.copyOf();
        // A shared predicate between $a1.i != $b1.i and $a2.i != $b2.i
        Predicate<Object[]> beta = arr -> {
            int a1i = (int) arr[0];
            int b1i = (int) arr[1];
            return a1i != b1i;
        };

        // $c.i > 0
        Predicate<Object[]> alpha = arr -> {
            int i = (int) arr[0];
            return i > 0;
        };

        session.newRule("test alpha 1")
                .forEach(
                        "$a1", TypeA.class,
                        "$b1", TypeB.class,
                        "$a2", TypeA.class,
                        "$b2", TypeB.class,
                        "$c", TypeC.class,
                        "$d", TypeD.class
                )
                .where(beta, "$a1.i", "$b1.i")
                .where(beta, "$a2.i", "$b2.i")
                .where(alpha, "$c.i")
                .execute(rhsAssert1);


        TypeA a1 = new TypeA("A1");
        a1.setI(1);

        TypeA a2 = new TypeA("A2");
        a2.setI(2);

        TypeB b1 = new TypeB("B1");
        b1.setI(3);

        TypeB b2 = new TypeB("B2");
        b2.setI(4);

        TypeC c1 = new TypeC("C1");
        c1.setI(-300);

        TypeD d1 = new TypeD("D1");
        d1.setI(400);

        session.insertAndFire(a1, b1, a2, b2, c1, d1);
        rhsAssert1.assertCount(0);

        TypeC c2 = new TypeC("C2");
        c2.setI(301);
        session.insertAndFire(c2);

        rhsAssert1.assertCount(4 * 4).reset();

        TypeC c3 = new TypeC("C3");
        c3.setI(302);
        session.insertAndFire(c3);

        rhsAssert1.assertCount(4 * 4).reset();

        TypeD d2 = new TypeD("D2");
        d2.setI(4000);
        session.insertAndFire(d2);

        rhsAssert1
                .assertCount(2 * 4 * 4)
                .assertUniqueCount("$d", 1)
                .assertUniqueCount("$a1", 2)
                .assertUniqueCount("$b1", 2)
                .assertUniqueCount("$a2", 2)
                .assertUniqueCount("$b2", 2)
                .assertUniqueCount("$c", 2)
                .reset();


        // Creating the same rule under another name
        session.newRule("test alpha 2")
                .forEach(
                        "$a1", TypeA.class,
                        "$b1", TypeB.class,
                        "$a2", TypeA.class,
                        "$b2", TypeB.class,
                        "$c", TypeC.class,
                        "$d", TypeD.class
                )
                .where(beta, "$a1.i", "$b1.i")
                .where(beta, "$a2.i", "$b2.i")
                .where(alpha, "$c.i")
                .execute(rhsAssert2);

        session.fire();
        rhsAssert1.assertCount(0).reset();
        rhsAssert2.assertCount(0).reset();

        // Another rule  with extra alpha predicates
        session.newRule("test alpha 3")
                .forEach(
                        "$a1", TypeA.class,
                        "$b1", TypeB.class,
                        "$a2", TypeA.class,
                        "$b2", TypeB.class,
                        "$c", TypeC.class,
                        "$d", TypeD.class
                )
                .where(beta, "$a1.i", "$b1.i")
                .where(beta, "$a2.i", "$b2.i")
                .where(alpha, "$c.i")
                .where("$d.i > 100")
                .execute(rhsAssert3);

        session.fire();

        rhsAssert1.assertCount(0);
        rhsAssert2.assertCount(0);
        rhsAssert3.assertCount(0);

        // Test the new ruleset as a whole
        TypeD d3 = new TypeD("D3");
        d3.setI(-4000);

        session.insertAndFire(d3);
        rhsAssert1.assertCount(2 * 4 * 4).reset();
        rhsAssert2.assertCount(2 * 4 * 4).reset();
        rhsAssert3.assertCount(0).reset();

        TypeD d4 = new TypeD("D4");
        d4.setI(4000);
        session.insertAndFire(d4);
        rhsAssert1.assertCount(2 * 4 * 4).reset();
        rhsAssert2.assertCount(2 * 4 * 4).reset();
        rhsAssert3.assertCount(2 * 4 * 4).reset();


        session.close();
    }

}
