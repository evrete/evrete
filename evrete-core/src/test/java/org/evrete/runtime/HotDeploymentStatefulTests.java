package org.evrete.runtime;

import org.evrete.KnowledgeService;
import org.evrete.api.*;
import org.evrete.classes.TypeA;
import org.evrete.classes.TypeB;
import org.evrete.classes.TypeC;
import org.evrete.classes.TypeD;
import org.evrete.helper.RhsAssert;
import org.evrete.util.NextIntSupplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.HashSet;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.evrete.api.FactBuilder.fact;
import static org.evrete.runtime.MemoryInspectionUtils.*;

@SuppressWarnings({"resource", "ExtractMethodRecommender"})
class HotDeploymentStatefulTests {
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
        session = service.newStatefulSession();
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void plainTest0(ActivationMode mode) {
        session.setActivationMode(mode);
        RhsAssert rhsAssert = new RhsAssert("$n", Integer.class);
        session
                .builder()
                .newRule()
                .forEach("$n", Integer.class)
                .execute(rhsAssert)
                .build();

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
        session
                .builder()
                .newRule()
                .forEach("$n", Integer.class)
                .where("$n.intValue >= 0 ")
                .execute(rhsAssert)
                .build();

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
        session
                .builder()
                .newRule("A")
                .forEach("$a", String.class)
                .execute()
                .build();
        RuntimeRule a = session.getRule("A");
        assert a != null;
        Assertions.assertThrows(RuntimeException.class,
                () -> session
                        .builder()
                        .newRule("A") // Same name
                        .forEach("$a", String.class)
                        .execute()
                        .build()
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

        session
                .builder()
                .newRule("testSingleFinalNode1")
                .forEach(
                        fact("$a", TypeA.class),
                        fact("$b", TypeB.class),
                        fact("$c", TypeC.class),
                        fact("$d", TypeD.class)
                )
                .where(sharedPredicate, "$a.i", "$b.i")
                .where(sharedPredicate, "$a.i", "$c.i")
                .where(sharedPredicate, "$a.i", "$d.i")
                .execute()
                .build();

        RhsAssert rhsAssert = new RhsAssert(session);

        int ai = new Random().nextInt(10) + 1;
        int bi = new Random().nextInt(10) + 1;
        int ci = new Random().nextInt(10) + 1;
        int di = new Random().nextInt(10) + 1;

        NextIntSupplier id = new NextIntSupplier();

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
            int ai = values.get(0);
            int bi = values.get(1);
            return ai == bi;
        };

        Predicate<Object[]> p2 = values -> {
            long cl = (long) values[0];
            long bl = (long) values[1];
            return cl == bl;
        };

        ValuesPredicate p3 = values -> {
            int ci = values.get(0);
            long al = values.get(1);
            return ci == al;
        };


        session
                .builder()
                .newRule("test circular")
                .forEach(
                        fact("$a", TypeA.class),
                        fact("$b", TypeB.class),
                        fact("$c", TypeC.class)
                )
                .where("$a.i == $b.i")
                .where(p1, "$a.i", "$b.i")
                .where(p2, "$c.l", "$b.l")
                .where(p3, "$c.i", "$a.l")
                .execute()
                .build();

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

        session
                .builder()
                .newRule(ruleName)
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
                .execute()
                .build();


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
            int ai = v.get(0);
            long bl = v.get(1);
            short bs = v.get(2);
            long al = v.get(3);
            return ai * bl * bs == al;
        };

        session
                .builder()
                .newRule(ruleName)
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class
                )
                .where(predicate, "$a.i", "$b.l", "$b.s", "$a.l")
                .execute()
                .build();


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

        session
                .builder()
                .newRule("test alpha 1")
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
                .execute(rhsAssert)
                .build();


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
            int ai = v.get(0);
            int bi = v.get(1);
            return ai != bi;
        };
        ValuesPredicate rule1_2 = v -> {
            double ad = v.get(0);
            return ad > 1;
        };
        ValuesPredicate rule1_3 = v -> {
            int bi = v.get(0);
            return bi > 10;
        };


        ValuesPredicate rule2_1 = v -> {
            int ai = v.get(0);
            int bi = v.get(1);
            return ai != bi;
        };

        ValuesPredicate rule2_2 = v -> {
            int ai = v.get(0);
            return ai < 3;
        };

        ValuesPredicate rule2_3 = v -> {
            float bf = v.get(0);
            return bf < 10;
        };

        session
                .builder()
                .newRule("test alpha 1")
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
                .build()
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
            int i1 = v.get(0);
            int i2 = v.get(1);
            int i3 = v.get(2);

            return i1 * i2 == i3;
        };
        session
                .builder()
                .newRule("test uni 2")
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
                )
                .build();


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

        session
                .builder()
                .newRule()
                .forEach(
                        fact("$a", TypeA.class),
                        fact("$b", TypeB.class),
                        fact("$c", TypeC.class)
                )
                .where("$a.i > 4")
                .where("$b.i > 3")
                .execute(rhsAssert)
                .build();


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

        session
                .builder()
                .newRule()
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class,
                        "$c", TypeC.class
                )
                .where("$a.i > 4")
                .where("$b.i > 3")
                .where("$c.i > 6")
                .execute(rhsAssert)
                .build();

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

        session
                .builder()
                .newRule("rule 1")
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
                .execute(rhsAssert3)
                .build();


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

        AtomicInteger rule1RhsCounter = new AtomicInteger();
        // Rule 1
        ValuesPredicate p1_1 = args -> args.get(0, int.class) > 4;
        ValuesPredicate p1_2 = args -> args.get(0, int.class) > 3;

        session
                .builder()
                .newRule("rule 1")
                .forEach(
                        fact("$a", TypeA.class),
                        fact("$b", TypeB.class),
                        fact("$c", TypeC.class)
                )
                .where(p1_1, "$a.i")
                .where(p1_2, "$b.i")
                .execute(ctx -> rule1RhsCounter.incrementAndGet())
                .build();

        // This insert cycle will result in 5x6 = 30 matching pairs of [A,B]
        for (int i = 0; i < 10; i++) {
            TypeA a = new TypeA("A" + i);
            a.setI(i);
            TypeB b = new TypeB("B" + i);
            b.setI(i);
            session.insert(a, b);
        }
        //RhsAssert rhsAssert1 = new RhsAssert(session, "rule 1");

        session.fire();
        Assertions.assertEquals(0, rule1RhsCounter.getAndSet(0));
        //rhsAssert1.assertCount(0).reset();

        TypeC trigger = new TypeC();

        FactHandle triggerHandle = session.insert(trigger);
        session.fire();
        // (5,6,7,8,9) x (4,5,6,7,8,9) = 5 x 6 = 30
        Assertions.assertEquals(30, rule1RhsCounter.getAndSet(0));


        // Adding another rule
        ValuesPredicate p2_1 = args -> args.get(0, int.class) > 3;
        ValuesPredicate p2_2 = args -> args.get(0, int.class) > 2;

        AtomicInteger rule2RhsCounter = new AtomicInteger();

        session
                .builder()
                .newRule("rule 2")
                .forEach(
                        fact("$a", TypeA.class),
                        fact("$b", TypeB.class),
                        fact("$c", TypeC.class)
                )
                .where(p2_1, "$a.i")
                .where(p2_2, "$b.i")
                .execute(ctx -> rule2RhsCounter.incrementAndGet())
                .build();

        session.fire();
        // There were no changes since the last fire, hence no effect
        Assertions.assertEquals(0, rule2RhsCounter.get());

        session.delete(triggerHandle);
        session.fire();
        Assertions.assertEquals(0, rule1RhsCounter.getAndSet(0));
        Assertions.assertEquals(0, rule2RhsCounter.getAndSet(0));

        // Re-inserting the trigger object should make the two rules fire
        session.insertAndFire(trigger);
        Assertions.assertEquals(6 * 5, rule1RhsCounter.getAndSet(0));
        Assertions.assertEquals(7 * 6, rule2RhsCounter.getAndSet(0));
    }


    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void testAlpha5(ActivationMode mode) {
        session.setActivationMode(mode);

        RhsAssert rhsAssert1 = new RhsAssert("$i", Integer.class);

        session
                .builder()
                .newRule("rule 1")
                .forEach("$i", Integer.class)
                .where("$i.intValue > 2")
                .execute(rhsAssert1)
                .build();

        for (int i = 0; i < 10; i++) {
            session.insert(i);
        }
        session.fire();
        rhsAssert1.assertCount(7); //3,4,5,6,7,8,9

        // Another rule w/o alpha
        RhsAssert rhsAssert2 = new RhsAssert("$i", Integer.class);
        session
                .builder()
                .newRule("rule 2")
                .forEach("$i", Integer.class)
                .execute(rhsAssert2)
                .build();
        session.fire();
        rhsAssert2.assertCount(0);

    }

    // An "inverse" version of the previous test
    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void testAlpha6(ActivationMode mode) {
        session.setActivationMode(mode);

        NextIntSupplier ruleCounter1 = new NextIntSupplier();

        session
                .builder()
                .newRule("rule 1")
                .forEach("$i", Integer.class)
                .execute(
                        ctx -> ruleCounter1.incrementAndGet()
                )
                .build();

        for (int i = 0; i < 10; i++) {
            session.insert(i);
        }
        session.fire();
        assert ruleCounter1.get() == 10;

        // Another rule w/ alpha
        NextIntSupplier ruleCounter2 = new NextIntSupplier();
        session
                .builder()
                .newRule("rule 2")
                .forEach("$i", Integer.class)
                .where("$i.intValue > 2")
                .execute(
                        ctx -> ruleCounter2.incrementAndGet()
                )
                .build();
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
        Predicate<Object[]> notTheSame = arr -> {
            int a1i = (int) arr[0];
            int b1i = (int) arr[1];
            return a1i != b1i;
        };

        // $c.i > 0
        Predicate<Object[]> positive = arr -> {
            int i = (int) arr[0];
            return i > 0;
        };

        session
                .builder()
                .newRule("rule1")
                .forEach(
                        "$a1", TypeA.class,
                        "$b1", TypeB.class,
                        "$a2", TypeA.class,
                        "$b2", TypeB.class,
                        "$c", TypeC.class,
                        "$d", TypeD.class
                )
                .where(notTheSame, "$a1.i", "$b1.i")
                .where(notTheSame, "$a2.i", "$b2.i")
                .where(positive, "$c.i")
                .execute(rhsAssert1)
                .build();

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
        rhsAssert1.assertCount(0); // Not fired because the "$c1.i" is negative

        // No delta states upon fire
        assertNoDeltaStates(session);

        /*
        As we have two independent beta fact groups, inserting a valid "c2" will result in the following
        RHS output (16 in total):

        [a1, b1, a1, b1, c2, d1]
        [a1, b2, a1, b1, c2, d1]
        [a2, b1, a1, b1, c2, d1]
        [a2, b2, a1, b1, c2, d1]

        [a1, b1, a1, b2, c2, d1]
        [a1, b2, a1, b2, c2, d1]
        [a2, b1, a1, b2, c2, d1]
        [a2, b2, a1, b2, c2, d1]

        [a1, b1, a2, b1, c2, d1]
        [a1, b2, a2, b1, c2, d1]
        [a2, b1, a2, b1, c2, d1]
        [a2, b2, a2, b1, c2, d1]

        [a1, b1, a2, b2, c2, d1]
        [a1, b2, a2, b2, c2, d1]
        [a2, b1, a2, b2, c2, d1]
        [a2, b2, a2, b2, c2, d1]
         */
        TypeC c2 = new TypeC("C2");
        c2.setI(301);
        session.insertAndFire(c2);

        rhsAssert1.assertCount(4 * 4).reset();
        // No delta states upon fire
        assertNoDeltaStates(session);


        /*
        A similar output will produce inserting another valid "c3" instance:

        [a1, b1, a1, b1, c3, d1]
        [a1, b2, a1, b1, c3, d1]
        [a2, b1, a1, b1, c3, d1]
        [a2, b2, a1, b1, c3, d1]

        [a1, b1, a1, b2, c3, d1]
        [a1, b2, a1, b2, c3, d1]
        [a2, b1, a1, b2, c3, d1]
        [a2, b2, a1, b2, c3, d1]

        [a1, b1, a2, b1, c3, d1]
        [a1, b2, a2, b1, c3, d1]
        [a2, b1, a2, b1, c3, d1]
        [a2, b2, a2, b1, c3, d1]

        [a1, b1, a2, b2, c3, d1]
        [a1, b2, a2, b2, c3, d1]
        [a2, b1, a2, b2, c3, d1]
        [a2, b2, a2, b2, c3, d1]

        */
        TypeC c3 = new TypeC("C3");
        c3.setI(302);
        session.insertAndFire(c3);
        rhsAssert1.assertCount(4 * 4).reset();

        /*
        Inserting another "d2" instance will produce twice the previous output with additional
        iteration over "c2"

        [a1, b1, a1, b1, c3, d2]
        [a1, b2, a1, b1, c3, d2]
        [a2, b1, a1, b1, c3, d2]
        [a2, b2, a1, b1, c3, d2]

        [a1, b1, a1, b2, c3, d2]
        [a1, b2, a1, b2, c3, d2]
        [a2, b1, a1, b2, c3, d2]
        [a2, b2, a1, b2, c3, d2]

        [a1, b1, a2, b1, c3, d2]
        [a1, b2, a2, b1, c3, d2]
        [a2, b1, a2, b1, c3, d2]
        [a2, b2, a2, b1, c3, d2]

        [a1, b1, a2, b2, c3, d2]
        [a1, b2, a2, b2, c3, d2]
        [a2, b1, a2, b2, c3, d2]
        [a2, b2, a2, b2, c3, d2]

        [a1, b1, a1, b1, c2, d2]
        [a1, b2, a1, b1, c2, d2]
        [a2, b1, a1, b1, c2, d2]
        [a2, b2, a1, b1, c2, d2]

        [a1, b1, a1, b2, c2, d2]
        [a1, b2, a1, b2, c2, d2]
        [a2, b1, a1, b2, c2, d2]
        [a2, b2, a1, b2, c2, d2]

        [a1, b1, a2, b1, c2, d2]
        [a1, b2, a2, b1, c2, d2]
        [a2, b1, a2, b1, c2, d2]
        [a2, b2, a2, b1, c2, d2]

        [a1, b1, a2, b2, c2, d2]
        [a1, b2, a2, b2, c2, d2]
        [a2, b1, a2, b2, c2, d2]
        [a2, b2, a2, b2, c2, d2]
         */

        TypeD d2 = new TypeD("D2");
        d2.setI(4000);
        session.insertAndFire(d2);
        // No delta states upon fire
        assertNoDeltaStates(session);

        rhsAssert1
                .assertCount(2 * 4 * 4)
                .assertUniqueCount("$d", 1)
                .assertUniqueCount("$a1", 2)
                .assertUniqueCount("$b1", 2)
                .assertUniqueCount("$a2", 2)
                .assertUniqueCount("$b2", 2)
                .assertUniqueCount("$c", 2)
                .reset();


        /*
         * *******************************
         * Now the "hot deployment" part *
         * *******************************
         */

        // We will append a new rule with the same logic but under a different name and make sure that the rule's
        // memory allocation is performed properly

        SessionFactType a1_ft_rule1 = factDeclaration(session, "rule1", "$a1");
        SessionFactType a2_ft_rule1 = factDeclaration(session, "rule1", "$a2");
        SessionFactType b1_ft_rule1 = factDeclaration(session, "rule1", "$b1");
        SessionFactType b2_ft_rule1 = factDeclaration(session, "rule1", "$b2");
        SessionFactType c_ft_rule1 = factDeclaration(session, "rule1", "$c");
        SessionFactType d_ft_rule1 = factDeclaration(session, "rule1", "$d");

        session
                .builder()
                .newRule("rule2")
                .forEach(
                        "$a1", TypeA.class,
                        "$b1", TypeB.class,
                        "$a2", TypeA.class,
                        "$b2", TypeB.class,
                        "$c", TypeC.class,
                        "$d", TypeD.class
                )
                .where(notTheSame, "$a1.i", "$b1.i")
                .where(notTheSame, "$a2.i", "$b2.i")
                .where(positive, "$c.i")
                .execute(rhsAssert2)
                .build();

        // Getting fact declarations for the new rule
        SessionFactType a1_ft_rule2 = factDeclaration(session, "rule2", "$a1");
        SessionFactType a2_ft_rule2 = factDeclaration(session, "rule2", "$a2");
        SessionFactType b1_ft_rule2 = factDeclaration(session, "rule2", "$b1");
        SessionFactType b2_ft_rule2 = factDeclaration(session, "rule2", "$b2");
        SessionFactType c_ft_rule2 = factDeclaration(session, "rule2", "$c");
        SessionFactType d_ft_rule2 = factDeclaration(session, "rule2", "$d");

        // The declarations must have the same alpha memory locations
        Assertions.assertEquals(a1_ft_rule1.getAlphaAddress(), a1_ft_rule2.getAlphaAddress());
        Assertions.assertEquals(a2_ft_rule1.getAlphaAddress(), a2_ft_rule2.getAlphaAddress());
        Assertions.assertEquals(b1_ft_rule1.getAlphaAddress(), b1_ft_rule2.getAlphaAddress());
        Assertions.assertEquals(b2_ft_rule1.getAlphaAddress(), b2_ft_rule2.getAlphaAddress());
        Assertions.assertEquals(c_ft_rule1.getAlphaAddress(), c_ft_rule2.getAlphaAddress());
        //Assertions.assertEquals(d_ft_rule1.getAlphaAddress(), d_ft_rule2.getAlphaAddress());

        // Inspecting alpha memory for TypeD
        Set<AlphaAddress> alphaAddressesOfTypeD1 = getAlphaConditions(session, TypeD.class);
        Assertions.assertEquals(1, alphaAddressesOfTypeD1.size());
        AlphaAddress alphaAddress_d_no_Conditions = alphaAddressesOfTypeD1.iterator().next();

        Stream<FactHolder> d_facts_main_1 = alphaMemoryContents(session, alphaAddress_d_no_Conditions);
        AtomicInteger d_fact_count_main1 = new AtomicInteger();
        d_facts_main_1.forEach(factHolder -> {
            FactFieldValues fieldValues = MemoryInspectionUtils.fieldValues(factHolder.getHandle(), session);
            Assertions.assertEquals(0, Objects.requireNonNull(fieldValues).size());
            TypeD fact = (TypeD) factHolder.getFact();
            d_fact_count_main1.incrementAndGet();
            assert fact.getId().equals("D1") || fact.getId().equals("D2"): "Unexpected results";
        });

        Assertions.assertEquals(2, d_fact_count_main1.get());

        session.fire();
        rhsAssert1.assertCount(0).reset();
        rhsAssert2.assertCount(0).reset();

        // Another rule with extra alpha predicates
        session
                .builder()
                .newRule("rule3")
                .forEach(
                        "$a1", TypeA.class,
                        "$b1", TypeB.class,
                        "$a2", TypeA.class,
                        "$b2", TypeB.class,
                        "$c", TypeC.class,
                        "$d", TypeD.class
                )
                .where(notTheSame, "$a1.i", "$b1.i")
                .where(notTheSame, "$a2.i", "$b2.i")
                .where(positive, "$c.i")
                .where("$d.i > 100")
                .execute(rhsAssert3)
                .build();

        // Now, the TypeD type must have two alpha memories
        Set<AlphaAddress> alphaAddressesOfTypeD2 = getAlphaConditions(session, TypeD.class);
        Assertions.assertEquals(2, alphaAddressesOfTypeD2.size());

        // And the previous memory inspection must show non-empty field values
        Stream<FactHolder> d_facts_main_2 = alphaMemoryContents(session, alphaAddress_d_no_Conditions);
        AtomicInteger d_fact_count_main2 = new AtomicInteger();
        d_facts_main_2.forEach(factHolder -> {
            d_fact_count_main2.incrementAndGet();
            FactFieldValues fieldValues = MemoryInspectionUtils.fieldValues(factHolder.getHandle(), session);
            Assertions.assertEquals(1, Objects.requireNonNull(fieldValues).size());
            TypeD fact = (TypeD) factHolder.getFact();
            assert fact.getId().equals("D1") || fact.getId().equals("D2"): "Unexpected results";
        });

        Assertions.assertEquals(2, d_fact_count_main2.get());


        // No changes in delta memory - no rule activations
        session.fire();
        rhsAssert1.assertCount(0);
        rhsAssert2.assertCount(0);
        rhsAssert3.assertCount(0);

        // Inserting a new TypeD instance that doesn't match the last rule's criteria ...
        TypeD d3 = new TypeD("D3");
        d3.setI(-4000);

        session.insertAndFire(d3);
        rhsAssert1.assertCount(2 * 4 * 4).reset(); // The expected count
        rhsAssert2.assertCount(2 * 4 * 4).reset(); // The expected count
        rhsAssert3.assertCount(0).reset(); // // The expected ZERO activation count ($d.i < 100)

        TypeD d4 = new TypeD("D4");
        d4.setI(4000);
        session.insertAndFire(d4);
        rhsAssert1.assertCount(2 * 4 * 4).reset();
        rhsAssert2.assertCount(2 * 4 * 4).reset();
        rhsAssert3.assertCount(2 * 4 * 4).reset();


        session.close();
    }

}
