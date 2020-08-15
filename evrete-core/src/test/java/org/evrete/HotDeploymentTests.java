package org.evrete;

import org.evrete.api.StatefulSession;
import org.evrete.api.Type;
import org.evrete.api.ValuesPredicate;
import org.evrete.classes.TypeA;
import org.evrete.classes.TypeB;
import org.evrete.classes.TypeC;
import org.evrete.classes.TypeD;
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

    @Test
    void testSingleFinalNode1() {

        final Set<TypeA> as1 = new HashSet<>();
        final Set<TypeB> bs1 = new HashSet<>();
        final Set<TypeC> cs1 = new HashSet<>();
        final Set<TypeD> ds1 = new HashSet<>();
        final AtomicInteger total = new AtomicInteger();


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
                .execute(ctx -> {
                    total.getAndIncrement();
                    TypeA a = ctx.get("$a");
                    as1.add(a);
                    TypeB b = ctx.get("$b");
                    bs1.add(b);
                    TypeC c = ctx.get("$c");
                    cs1.add(c);
                    TypeD d = ctx.get("$d");
                    ds1.add(d);
                });


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

        assert as1.size() == ai;
        assert bs1.size() == bi;
        assert cs1.size() == ci;
        assert ds1.size() == di;
        assert total.get() == ai * bi * ci * di;

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

        AtomicInteger totalRows = new AtomicInteger(0);
        session.getRule("test circular").setRhs(ctx -> totalRows.incrementAndGet());

        session.insertAndFire(a, aa, aaa);
        session.insertAndFire(b, bb, bbb);
        session.insertAndFire(c, cc, ccc);

        assert totalRows.get() == 3 : "Actual: " + totalRows.get();
    }

    @Test
    void testMultiFinal2_mini() {
        String ruleName = "testMultiFinal2_mini";

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

        AtomicInteger totalRows = new AtomicInteger(0);
        session.getRule(ruleName)
                .setRhs(null) // RHS can be overridden
                .setRhs(ctx -> totalRows.incrementAndGet());

        session.insertAndFire(a, b, c);
        assert totalRows.get() == 1 : "Actual: " + totalRows.get();

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

        totalRows.set(0);
        session.insertAndFire(aa, bb, cc);
        assert totalRows.get() == 2 : "Actual: " + totalRows.get();

        session.close();
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

        session.newRule(ruleName)
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class
                )
                .where(predicate, "$a.i", "$b.l", "$b.s", "$a.l")
                .execute(null);


        TypeA a1 = new TypeA("A1");
        a1.setI(2);
        a1.setL(30L);

        TypeB b1 = new TypeB("B1");
        b1.setL(3);
        b1.setS((short) 5);

        AtomicInteger totalRows = new AtomicInteger(0);
        session.getRule(ruleName)
                .setRhs(null) // RHS can be overridden
                .setRhs(ctx -> totalRows.incrementAndGet());

        session.insertAndFire(a1, b1);
        assert totalRows.get() == 1 : "Actual: " + totalRows.get();

        //Second insert
        TypeA a2 = new TypeA("A2");
        a2.setI(7);
        a2.setL(693L);

        TypeB b2 = new TypeB("B2");
        b2.setL(9);
        b2.setS((short) 11);

        totalRows.set(0);
        session.insertAndFire(a2, b2);
        assert totalRows.get() == 2 : "Actual: " + totalRows.get();

        session.close();
    }

    @Test
    void testMixed1() {
        AtomicInteger fireCounter = new AtomicInteger();

        Predicate<Object[]> beta = arr -> {
            int a1i = (int) arr[0];
            int b1i = (int) arr[1];
            return a1i != b1i;
        };

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
                .execute(
                        ctx -> fireCounter.incrementAndGet()
                );


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
        assert fireCounter.get() == 0 : "Actual: " + fireCounter.get();

        TypeC c2 = new TypeC("C2");
        c2.setI(1);
        session.insertAndFire(c2);

        assert fireCounter.get() == 4 * 4 : "Actual: " + fireCounter.get();

        TypeC c3 = new TypeC("C3");
        c3.setI(100);
        fireCounter.set(0);
        session.insertAndFire(c3);

        assert fireCounter.get() == 2 * 4 * 4 : "Actual: " + fireCounter.get();

        TypeD d2 = new TypeD("D2");
        fireCounter.set(0);
        session.insertAndFire(d2);

        assert fireCounter.get() == 2 * 2 * 4 * 4 : "Actual: " + fireCounter.get();
    }


    @Test
    void testAlphaBeta1() {
        AtomicInteger fireCounter1 = new AtomicInteger();
        Set<TypeA> setA1 = new HashSet<>();
        Set<TypeB> setB1 = new HashSet<>();

        AtomicInteger fireCounter2 = new AtomicInteger();
        Set<TypeA> setA2 = new HashSet<>();
        Set<TypeB> setB2 = new HashSet<>();


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
                .execute(
                        ctx -> {
                            TypeA a = ctx.get("$a");
                            TypeB b = ctx.get("$b");
                            setA1.add(a);
                            setB1.add(b);
                            fireCounter1.incrementAndGet();
                        }
                )
                .newRule("test alpha 2")
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class
                )
                .where(rule2_1, "$a.i", "$b.i")
                .where(rule2_2, "$a.i")
                .where(rule2_3, "$b.f")
                .execute(
                        ctx -> {
                            TypeA a = ctx.get("$a");
                            TypeB b = ctx.get("$b");
                            setA2.add(a);
                            setB2.add(b);
                            fireCounter2.incrementAndGet();
                        }
                );


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

        assert fireCounter1.get() == 2 : "Actual: " + fireCounter1.get();
        assert setB1.size() == 1 && setB1.contains(bb);
        assert setA1.size() == 2 && setA1.contains(aa) && setA1.contains(aaa);

        assert fireCounter2.get() == 2 : "Actual: " + fireCounter2.get();
        assert setB2.size() == 1 && setB2.contains(b);
        assert setA2.size() == 2 && setA2.contains(a) && setA2.contains(aa);
    }

    @Test
    void testUniType2() {
        AtomicInteger fireCounter = new AtomicInteger();

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
                            fireCounter.incrementAndGet();
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

    @Test
    void testAlpha1() {
        AtomicInteger fireCounter = new AtomicInteger();

        ValuesPredicate p1 = v -> {
            int i1 = (int) v.apply(0);
            return i1 > 4;
        };

        ValuesPredicate p2 = v -> {
            int i1 = (int) v.apply(0);
            return i1 > 3;
        };

        session.newRule()
                .forEach(
                        fact("$a", TypeA.class),
                        fact("$b", TypeB.class),
                        fact("$c", TypeC.class)
                )
                .where(p1, "$a.i")
                .where(p2, "$b.i")
                .execute(
                        ctx -> fireCounter.incrementAndGet()
                );

        // This insert cycle will result in 5x6 = 30 matching pairs of [A,B]
        for (int i = 0; i < 10; i++) {
            TypeA a = new TypeA("A" + i);
            a.setI(i);
            TypeB b = new TypeB("B" + i);
            b.setI(i);
            session.insert(a, b);
        }
        session.fire();
        assert fireCounter.get() == 0;
        session.insert(new TypeC("C"));
        session.fire();
        assert fireCounter.get() == 30 : "Actual:" + fireCounter.get();
    }

    @Test
    void testAlpha2() {
        AtomicInteger fireCounter = new AtomicInteger();
        Set<TypeA> setA = new HashSet<>();
        Set<TypeB> setB = new HashSet<>();
        Set<TypeC> setC = new HashSet<>();

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


        session.newRule()
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class,
                        "$c", TypeC.class
                )
                .where(p1, "$a.i")
                .where(p2, "$b.i")
                .where(p3, "$c.i")
                .execute(
                        ctx -> {
                            setA.add(ctx.get("$a"));
                            setB.add(ctx.get("$b"));
                            setC.add(ctx.get("$c"));
                            fireCounter.incrementAndGet();
                        }
                );


        // This insert cycle will result in 5x6 = 30 matching pairs of [A,B]
        for (int i = 0; i < 10; i++) {
            TypeA a = new TypeA("A" + i);
            a.setI(i);
            TypeB b = new TypeB("B" + i);
            b.setI(i);
            session.insert(a, b);
        }
        session.fire();
        assert fireCounter.get() == 0; // No instances of TypeC, no rhs

        // This insert cycle will result in 3 matching pairs of C (7,8,9)
        for (int i = 0; i < 10; i++) {
            TypeC c = new TypeC("C" + i);
            c.setI(i);
            session.insert(c);
        }
        session.fire();
        assert fireCounter.get() == 30 * 3;
        assert setA.size() == 5;
        assert setB.size() == 6;
        assert setC.size() == 3;
    }

    @Test
    void testAlpha3() {
        Configuration conf = session.getConfiguration();
        conf.setWarnUnknownTypes(false);
        assert !session.getConfiguration().isWarnUnknownTypes();

        AtomicInteger counter1 = new AtomicInteger();
        AtomicInteger counter2 = new AtomicInteger();
        AtomicInteger counter3 = new AtomicInteger();

        ValuesPredicate p1 = v -> {
            String s = (String) v.apply(0);
            return s.equals("A5");
        };

        ValuesPredicate p2 = v -> {
            String s = (String) v.apply(0);
            return s.equals("A7");
        };

        ValuesPredicate p3 = v -> {
            String s = (String) v.apply(0);
            return !s.equals("A5");
        };

        session
                .newRule("rule 1")
                .forEach(fact("$a", TypeA.class))
                .where(p1, "$a.id")
                .execute(ctx -> counter1.incrementAndGet())
                .newRule("rule 2")
                .forEach(
                        fact("$a", TypeA.class)
                )
                .where(p2, "$a.id")
                .execute(ctx -> counter2.incrementAndGet())
                .newRule("rule 3")
                .forEach(
                        fact("$a", TypeA.class)
                )
                .where(p3, "$a.id") // Inverse to rule 1
                .execute(ctx -> counter3.incrementAndGet());

        Type aType = session.getTypeResolver().getType(TypeA.class.getName());

        for (int i = 0; i < 10; i++) {
            session.insert(new TypeA("A" + i));
        }
        session.fire();
        assert ((StatefulSessionImpl) session).get(aType).knownFieldSets().size() == 0;

        assert counter1.get() == 1;
        assert counter2.get() == 1;
        assert counter3.get() == 9;

    }

    @Test
    void testAlpha4() {

        // Rule 1
        AtomicInteger ruleCounter1 = new AtomicInteger();

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
                .execute(
                        ctx -> ruleCounter1.incrementAndGet()
                );

        // This insert cycle will result in 5x6 = 30 matching pairs of [A,B]
        for (int i = 0; i < 10; i++) {
            TypeA a = new TypeA("A" + i);
            a.setI(i);
            TypeB b = new TypeB("B" + i);
            b.setI(i);
            session.insert(a, b);
        }
        session.fire();
        assert ruleCounter1.get() == 0;

        TypeC c = new TypeC();

        session.insert(c);
        session.fire();
        assert ruleCounter1.get() == 30 : "Actual:" + ruleCounter1.get();


        // Adding another rule
        AtomicInteger ruleCounter2 = new AtomicInteger();

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
                .execute(
                        ctx -> ruleCounter2.incrementAndGet()
                );

        session.fire();
        assert ruleCounter2.get() == 7 * 6;

        ruleCounter1.set(0);
        ruleCounter2.set(0);
        session.deleteAndFire(c);
        assert ruleCounter1.get() == 0;
        assert ruleCounter2.get() == 0;

        ruleCounter1.set(0);
        ruleCounter2.set(0);
        session.insertAndFire(c);
        assert ruleCounter1.get() == 6 * 5;
        assert ruleCounter2.get() == 7 * 6;
    }


    @Test
    void testAlpha5() {
        AtomicInteger ruleCounter1 = new AtomicInteger();

        session.newRule("rule 1")
                .forEach("$i", Integer.class)
                .where("$i.intValue > 2")
                .execute(
                        ctx -> ruleCounter1.incrementAndGet()
                );

        for (int i = 0; i < 10; i++) {
            session.insert(i);
        }
        session.fire();
        assert ruleCounter1.get() == 7; //3,4,5,6,7,8,9

        // Another rule w/o alpha
        AtomicInteger ruleCounter2 = new AtomicInteger();
        session.newRule("rule 2")
                .forEach("$i", Integer.class)
                .execute(
                        ctx -> ruleCounter2.incrementAndGet()
                );
        session.fire();
        assert ruleCounter2.get() == 10;

    }

    @Test
        // An "inverse" version of the previous test
    void testAlpha6() {
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
        assert ruleCounter2.get() == 7; //3,4,5,6,7,8,9

    }
}
