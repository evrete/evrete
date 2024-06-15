package org.evrete.runtime;

import org.evrete.Configuration;
import org.evrete.KnowledgeService;
import org.evrete.api.*;
import org.evrete.api.builders.RuleSetBuilder;
import org.evrete.classes.*;
import org.evrete.helper.FactEntry;
import org.evrete.helper.RhsAssert;
import org.evrete.helper.TestUtils;
import org.evrete.util.NextIntSupplier;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static org.evrete.api.FactBuilder.fact;

class StatefulBaseTests {
    private static KnowledgeService service;
    private Knowledge knowledge;

    @BeforeAll
    static void setUpClass() {
        service = new KnowledgeService();
    }

    @AfterAll
    static void shutDownClass() {
        service.shutdown();
    }


    private static void randomExpressionsTest(ActivationMode mode, int objectCount, int conditions) {
        Knowledge kn = service.newKnowledge();

        String RULE_NAME = "random";
        RuleSetBuilder<Knowledge> ruleSetBuilder = TestUtils.applyRandomConditions(kn.builder(), RULE_NAME, conditions);
        ruleSetBuilder.build();

        try (StatefulSession s = kn.newStatefulSession(mode)) {
            AtomicInteger callCounter = new AtomicInteger();
            Objects.requireNonNull(s.getRule(RULE_NAME))
                    .setRhs(rhsContext -> callCounter.incrementAndGet());
            for (int i = 0; i < objectCount; i++) {
                TypeA a = new TypeA();
                TypeB b = new TypeB();
                TypeC c = new TypeC();
                TypeD d = new TypeD();

                Base[] arr = new Base[]{a, b, c, d};
                for (int j = 0; j < arr.length; j++) {
                    Base o = arr[j];
                    int val = (j + 1) * 10;
                    o.setI(val);
                    o.setL(val);
                    o.setD(val);
                    o.setF(val);
                }

                s.insert(a, b, c, d);
            }
            s.fire();
            assert callCounter.get() == (int) Math.pow(objectCount, 4);

            Collection<FactEntry> sessionObjects = TestUtils.sessionFacts(s);
            assert sessionObjects.size() == objectCount * 4 : "Actual: " + sessionObjects.size() + ", expected: " + objectCount * 4;

            for (FactEntry e : sessionObjects) {
                s.delete(e.getHandle());
            }
            s.fire();
            sessionObjects = TestUtils.sessionFacts(s);
            assert sessionObjects.isEmpty() : "Actual: " + sessionObjects.size();
        }
    }

    private StatefulSession newSession() {
        return knowledge.newStatefulSession();
    }

    private StatefulSession newSession(ActivationMode mode) {
        return knowledge.newStatefulSession(mode);
    }

    @BeforeEach
    void init() {
        knowledge = service.newKnowledge();
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void emptyRulesTest(ActivationMode mode) {
        Knowledge k = knowledge.builder().build();
        try (StatefulSession session = k.newStatefulSession(mode)) {
            session.fire();
        }
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void emptyActionTest(ActivationMode mode) {
        Knowledge k = knowledge.builder()
                .newRule().forEach("$i", Integer.class)
                .execute()
                .build();

        try (StatefulSession session = k.newStatefulSession(mode)) {
            session.fire();
        }
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void factExistenceUponInsertTest(ActivationMode mode) {
        knowledge.builder()
                .newRule()
                .forEach("$a", TypeA.class)
                .execute()
                .build();

        try (StatefulSession session = knowledge.newStatefulSession(mode)) {
            TypeA a1 = new TypeA();
            TypeA a2 = new TypeA();
            FactHandle h1 = session.insert(a1);
            FactHandle h2 = session.insert(a2);

            assert session.getFact(h1) == a1;
            assert session.getFact(h2) == a2;

            FactHandle unknown = () -> Long.MAX_VALUE;

            Assertions.assertThrows(IllegalArgumentException.class, () -> session.getFact(unknown));
            Assertions.assertThrows(IllegalArgumentException.class, () -> session.delete(unknown));

            // Test deletion
            assert session.delete(h1);
            assert !session.delete(h1); // Subsequent delete calls must return false
            assert !session.delete(h1); // Subsequent delete calls must return false

            assert session.getFact(h1) == null;
            assert session.getFact(h2) == a2;

        }
    }


    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void basicRuleSortingOrder(ActivationMode mode) {
        RuleSetBuilder<Knowledge> ruleSetBuilder = knowledge.builder();

        List<String> ruleNames = new ArrayList<>();
        List<String> ruleActivationSequence = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            String ruleName = "rule " + i;
            ruleNames.add(ruleName);
            ruleSetBuilder.newRule(ruleName).forEach("$a", TypeA.class)
                    .where("$a.i > 0")
                    .execute(rhsContext -> ruleActivationSequence.add(rhsContext.getRule().getName()));
        }

        Knowledge k = ruleSetBuilder.build();

        try (StatefulSession session = k.newStatefulSession(mode)) {
            TypeA a = new TypeA();
            a.setI(999); // Will match the condition of each rule

            session.insert(a);
            session.fire();

            // Testing the sequence
            assert ruleActivationSequence.size() == ruleNames.size();
            for (int i = 0; i < ruleActivationSequence.size(); i++) {
                String ruleName1 = ruleNames.get(i);
                String ruleName2 = ruleActivationSequence.get(i);
                assert ruleName1.equals(ruleName2) : "Mismatch: '" + ruleName1 + "' != '" + ruleName2 + "' at index " + i;
            }
        }
    }


    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void plainTest0(ActivationMode mode) {
        RhsAssert rhsAssert = new RhsAssert("$n", Integer.class);
        knowledge
                .builder()
                .newRule()
                .forEach("$n", Integer.class)
                .execute(rhsAssert)
                .build();

        try (StatefulSession session = newSession(mode)) {
            // Chaining RHS
            RuntimeRule rule = session.getRules().iterator().next();
            NextIntSupplier counter = new NextIntSupplier();
            rule.chainRhs(ctx -> counter.incrementAndGet());

            session.insertAndFire(1, 2);
            rhsAssert.assertCount(2).reset();
            assert counter.get() == 2;
            session.insertAndFire(3);
            rhsAssert.assertCount(1).assertContains("$n", 3);
        }
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void plainTest1(ActivationMode mode) {
        RhsAssert rhsAssert = new RhsAssert("$n", Integer.class);
        knowledge
                .builder()
                .newRule()
                .forEach("$n", Integer.class)
                .where("$n.intValue >= 0 ")
                .execute(rhsAssert)
                .build();

        try (StatefulSession session = newSession(mode)) {
            session.insertAndFire(1, 2);
            rhsAssert.assertCount(2).reset();
            session.insertAndFire(3);
            rhsAssert.assertCount(1).assertContains("$n", 3).reset();

            session.fire();
            rhsAssert.assertCount(0);
            session.insertAndFire(-1);
            rhsAssert.assertCount(0);
        }
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void createDestroy1(ActivationMode mode) {
        knowledge
                .builder()
                .newRule("test")
                .forEach(
                        fact("$a1", TypeA.class.getName()),
                        fact("$a2", TypeA.class)
                )
                .where("$a1.id == $a2.id")
                .execute()
                .build();

        StatefulSession session1 = newSession(mode);
        StatefulSession session2 = newSession(mode);
        //noinspection resource
        session1
                .builder()
                .newRule()
                .forEach("$a", TypeA.class)
                .execute()
                .build()
        ;

        assert knowledge.getSessions().size() == 2;
        session2.close();
        assert knowledge.getSessions().size() == 1;
        session2.close(); // Second close has no effect
        session1.close();
        assert knowledge.getSessions().isEmpty();
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void testMultiFinal1(ActivationMode mode) {
        knowledge
                .builder()
                .newRule()
                .forEach(
                        fact("$a", TypeA.class),
                        fact("$b", TypeB.class),
                        fact("$c", TypeC.class)
                )
                .where("$a.i != $b.i")
                .where("$c.l != $b.l")
                .execute()
                .build();

        try (StatefulSession s = newSession(mode)) {
            assert s.getParentContext() == knowledge;
            RhsAssert rhsAssert = new RhsAssert(s);
            TypeA a = new TypeA("A");
            a.setAllNumeric(1);

            TypeA aa = new TypeA("AA");
            aa.setAllNumeric(11);

            TypeA aaa = new TypeA("AAA");
            aaa.setAllNumeric(111);

            TypeB b = new TypeB("B");
            b.setAllNumeric(2);

            TypeB bb = new TypeB("BB");
            bb.setAllNumeric(22);

            TypeB bbb = new TypeB("BBB");
            bbb.setAllNumeric(222);

            TypeC c = new TypeC("C");
            c.setAllNumeric(3);

            TypeC cc = new TypeC("CC");
            cc.setAllNumeric(33);

            TypeC ccc = new TypeC("CCC");
            ccc.setAllNumeric(333);

            s.insertAndFire(a, aa, aaa, b, bb, bbb, c, cc, ccc);
            rhsAssert.assertCount(27);
        }
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void testMultiFinal2(ActivationMode mode) {
        String ruleName = "testMultiFinal2";

        knowledge
                .builder()
                .newRule(ruleName)
                .forEach(
                        fact("$a", TypeA.class),
                        fact("$b", TypeB.class),
                        fact("$c", TypeC.class)
                )
                .where(
                        "$a.i == $b.i",
                        "$c.l == $b.l"
                )
                .execute()
                .build();

        try (StatefulSession s = newSession(mode)) {
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

            RhsAssert rhsAssert = new RhsAssert(s, ruleName);

            s.insertAndFire(a, aa, aaa, b, bb, bbb, c, cc, ccc);
            rhsAssert.assertCount(3);
        }
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void testSingleFinalNode1(ActivationMode mode) {

        knowledge
                .builder()
                .newRule("testSingleFinalNode1")
                .forEach(
                        fact("$a", TypeA.class),
                        fact("$b", TypeB.class),
                        fact("$c", TypeC.class),
                        fact("$d", TypeD.class)
                )
                .where("$a.i != $b.i")
                .where("$a.i != $c.i")
                .where("$a.i != $d.i")
                .execute()
                .build();

        try (StatefulSession s = newSession(mode)) {
            RhsAssert rhsAssert = new RhsAssert(s);

            int ai = new Random().nextInt(10) + 1;
            int bi = new Random().nextInt(10) + 1;
            int ci = new Random().nextInt(10) + 1;
            int di = new Random().nextInt(10) + 1;

            int id = 0;

            for (int i = 0; i < ai; i++) {
                int n = id++;
                TypeA obj = new TypeA(String.valueOf(n));
                obj.setI(n);
                s.insert(obj);
            }

            for (int i = 0; i < bi; i++) {
                int n = id++;
                TypeB obj = new TypeB(String.valueOf(n));
                obj.setI(n);
                s.insert(obj);
            }

            for (int i = 0; i < ci; i++) {
                int n = id++;
                TypeC obj = new TypeC(String.valueOf(n));
                obj.setI(n);
                s.insert(obj);
            }

            for (int i = 0; i < di; i++) {
                int n = id++;
                TypeD obj = new TypeD(String.valueOf(n));
                obj.setI(n);
                s.insert(obj);
            }

            s.fire();

            rhsAssert
                    .assertCount(ai * bi * ci * di)
                    .assertUniqueCount("$a", ai)
                    .assertUniqueCount("$b", bi)
                    .assertUniqueCount("$c", ci)
                    .assertUniqueCount("$d", di)
            ;
        }
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void testCircularMultiFinal(ActivationMode mode) {

        knowledge
                .builder()
                .newRule("test circular")
                .forEach(
                        fact("$a", TypeA.class),
                        fact("$b", TypeB.class),
                        fact("$c", TypeC.class)
                ).where(
                        "$a.i == $b.i",
                        "$c.l == $b.l",
                        "$c.i == $a.l")
                .execute()
                .build();

        try (StatefulSession s = newSession(mode)) {
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
    }


    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void randomExpressionsTest(ActivationMode mode) {
        for (int conditions = 8; conditions > 0; conditions--) {
            for (int objectCount = 8; objectCount > 0; objectCount--) {
                randomExpressionsTest(mode, objectCount, conditions);
            }
        }
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void testMultiFinal2_mini(ActivationMode mode) {
        String ruleName = "testMultiFinal2_mini";

        knowledge
                .builder()
                .newRule(ruleName)
                .forEach(
                        fact("$a", TypeA.class),
                        fact("$b", TypeB.class),
                        fact("$c", TypeC.class)
                )
                .where(
                        "$a.i == $b.i",
                        "$c.l == $b.l"
                )
                .execute()
                .build();

        try (StatefulSession s = newSession(mode)) {
            TypeA a = new TypeA("AA");
            a.setI(1);

            TypeB b = new TypeB("BB");
            b.setI(1);
            b.setL(1);

            TypeC c = new TypeC("CC");
            c.setL(1);

            RhsAssert rhsAssert = new RhsAssert(s);

            s.getRule(ruleName)
                    .setRhs(rhsAssert); // RHS can be overridden

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
        }
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void testFields(ActivationMode mode) {
        String ruleName = "testMultiFields";

        knowledge
                .builder()
                .newRule(ruleName)
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class
                )
                .where(
                        "$a.i * $b.l * $b.s == $a.l"
                )
                .execute()
                .build();

        try (StatefulSession s = newSession(mode)) {
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
        }
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void testSingleFinalNode2(ActivationMode mode) {
        knowledge
                .builder()
                .newRule("testSingleFinalNode2")
                .forEach(
                        fact("$a", TypeA.class),
                        fact("$b", TypeB.class),
                        fact("$c", TypeC.class),
                        fact("$d", TypeD.class)
                )
                .where("$a.i == $b.i")
                .where("$a.i == $c.i")
                .where("$a.i == $d.i")
                .execute()
                .build();

        try (StatefulSession s = newSession(mode)) {
            RhsAssert rhsAssert = new RhsAssert(s);

            int count = new Random().nextInt(100) + 1;

            int id = 0;

            for (int i = 0; i < count; i++) {
                String stringId = String.valueOf(id++);
                TypeA a = new TypeA(stringId);
                a.setI(i);
                s.insert(a);

                TypeB b = new TypeB(stringId);
                b.setI(i);
                s.insert(b);

                TypeC c = new TypeC(stringId);
                c.setI(i);
                s.insert(c);

                TypeD d = new TypeD(stringId);
                d.setI(i);
                s.insert(d);

            }
            s.fire();

            rhsAssert
                    .assertCount(count)
                    .assertUniqueCount("$a", count)
                    .assertUniqueCount("$b", count)
                    .assertUniqueCount("$c", count)
                    .assertUniqueCount("$d", count);
        }
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void testBeta1(ActivationMode mode) {

        RhsAssert rhsAssert = new RhsAssert(
                "$a", TypeA.class,
                "$b", TypeB.class
        );

        knowledge
                .builder()
                .newRule()
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class
                )
                .where("$a.i == $b.i")
                .execute(rhsAssert)
                .build();

        TypeB b1;
        TypeA a1_1;
        try (StatefulSession s = newSession(mode)) {

            TypeA a1 = new TypeA("A1");
            a1.setAllNumeric(1);

            TypeA a2 = new TypeA("A2");
            a2.setAllNumeric(2);

            b1 = new TypeB("B1");
            b1.setAllNumeric(1);

            TypeB b2 = new TypeB("B2");
            b2.setAllNumeric(2);

            s.insertAndFire(a1, a2, b1, b2);
            rhsAssert.assertCount(2).reset();

            a1_1 = new TypeA("A1_1");
            a1_1.setAllNumeric(1);
            s.insertAndFire(a1_1);
        }
        rhsAssert.assertCount(1);
        rhsAssert.assertContains("$a", a1_1);
        rhsAssert.assertContains("$b", b1);
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void testAlphaBeta1(ActivationMode mode) {
        RhsAssert rhsAssert1 = new RhsAssert(
                "$a", TypeA.class,
                "$b", TypeB.class
        );

        RhsAssert rhsAssert2 = new RhsAssert(
                "$a", TypeA.class,
                "$b", TypeB.class
        );

        knowledge
                .builder()
                .newRule("test alpha 1")
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class
                )
                .where("$a.i != $b.i")
                .where("$a.d > 1")
                .where("$b.i > 10")
                .execute(rhsAssert1)
                .newRule("test alpha 2")
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class
                )
                .where("$a.i != $b.i")
                .where("$a.i < 3")
                .where("$b.f < 10")
                .execute(rhsAssert2)
                .build()
        ;

        TypeA a;
        TypeA aa;
        TypeA aaa;
        TypeB b;
        TypeB bb;
        try (StatefulSession s = newSession(mode)) {

            a = new TypeA("A");
            a.setAllNumeric(0);

            aa = new TypeA("AA");
            aa.setAllNumeric(2);

            aaa = new TypeA("AAA");
            aaa.setAllNumeric(3);

            b = new TypeB("B");
            b.setAllNumeric(9);

            bb = new TypeB("BB");
            bb.setAllNumeric(100);

            s.insertAndFire(a, aa, aaa, b, bb);
        }

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
    void testSimple1(ActivationMode mode) {
        RhsAssert rhsAssert = new RhsAssert(
                "$a", TypeA.class,
                "$b", TypeB.class
        );

        knowledge
                .builder()
                .newRule()
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class
                )
                .where("$a.i < $b.d")
                .execute(rhsAssert)
                .build();


        try (StatefulSession s = newSession(mode)) {

            TypeA a1 = new TypeA("A1");
            a1.setI(1);

            TypeB b1 = new TypeB("B1");
            b1.setD(10.0);

            TypeA a2 = new TypeA("A2");
            a2.setI(1);

            TypeB b2 = new TypeB("B2");
            b2.setD(10.0);

            s.insertAndFire(a1, b1, a2, b2);
        }
        rhsAssert.assertCount(4);
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void testSimple2(ActivationMode mode) {
        RhsAssert rhsAssert = new RhsAssert(
                "$a", TypeA.class,
                "$b", TypeB.class
        );


        knowledge
                .builder()
                .newRule()
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class
                )
                .where("$a.i < $b.d")
                .where("$a.f < $b.l")
                .execute(rhsAssert)
                .build();

        try (StatefulSession s = newSession(mode)) {

            TypeA a1 = new TypeA("A1");
            a1.setI(1);
            a1.setF(1.0f);

            TypeB b1 = new TypeB("B1");
            b1.setD(10.0);
            b1.setL(10L);

            TypeA a2 = new TypeA("A2");
            a2.setI(1);
            a2.setF(1.0f);

            TypeB b2 = new TypeB("B2");
            b2.setD(10.0);
            b2.setL(10L);

            s.insertAndFire(a1, b1, a2, b2);
        }
        rhsAssert.assertCount(4);


    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void testSimple3(ActivationMode mode) {
        RhsAssert rhsAssert = new RhsAssert(
                "$a", TypeA.class,
                "$b", TypeB.class,
                "$c", TypeC.class,
                "$d", TypeD.class
        );


        knowledge
                .builder()
                .newRule()
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class,
                        "$c", TypeC.class,
                        "$d", TypeD.class
                )
                .where("$a.i < $b.d")
                .where("$a.f < $b.l")
                .where("$c.i < $d.d")
                .where("$c.f < $d.l")
                .execute(rhsAssert)
                .build();

        try (StatefulSession s = newSession(mode)) {

            TypeA a1 = new TypeA("A1");
            a1.setI(1);
            a1.setF(1.0f);

            TypeA a2 = new TypeA("A2");
            a2.setI(1);
            a2.setF(1.0f);

            TypeB b1 = new TypeB("B1");
            b1.setD(2.0);
            b1.setL(2L);

            TypeB b2 = new TypeB("B2");
            b2.setD(2.0);
            b2.setL(2L);

            TypeC c1 = new TypeC("C1");
            c1.setI(10);
            c1.setF(10.0f);

            TypeC c2 = new TypeC("C2");
            c2.setI(10);
            c2.setF(10.0f);

            TypeD d1 = new TypeD("D1");
            d1.setD(20.0);
            d1.setL(20L);

            TypeD d2 = new TypeD("D2");
            d2.setD(20.0);
            d2.setL(20L);

            s.insertAndFire(a1, b1, c1, d1, a2, b2, c2, d2);
        }
        rhsAssert.assertCount(16);
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void testSimple4(ActivationMode mode) {
        RhsAssert rhsAssert = new RhsAssert(
                "$a", TypeA.class,
                "$b", TypeB.class
        );

        knowledge
                .builder()
                .newRule()
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class
                )
                .where("$a.i != $b.i")
                .execute(rhsAssert)
                .build();

        try (StatefulSession s = newSession(mode)) {

            TypeA a1 = new TypeA("A1");
            a1.setI(1);

            TypeA a2 = new TypeA("A2");
            a2.setI(1);

            TypeB b1 = new TypeB("B1");
            b1.setI(10);

            TypeB b2 = new TypeB("B2");
            b2.setI(10);

            s.insertAndFire(a1, b1, a2, b2);
        }
        rhsAssert.assertCount(4);
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void testUniType1(ActivationMode mode) {
        RhsAssert rhsAssert = new RhsAssert(
                "$a1", TypeA.class,
                "$a2", TypeA.class
        );

        knowledge
                .builder()
                .newRule("test alpha 1")
                .forEach(
                        "$a1", TypeA.class,
                        "$a2", TypeA.class
                )
                .where("$a1.i != $a2.i")
                .execute(rhsAssert)
                .build();

        try (StatefulSession s = newSession(mode)) {

            TypeA a1 = new TypeA("A1");
            a1.setI(1);

            TypeA a2 = new TypeA("A2");
            a2.setI(10);

            s.insertAndFire(a1, a2);
        }
        rhsAssert.assertCount(2); // [a1, a2], [a2, a1]
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void testUniType2(ActivationMode mode) {
        Set<String> collectedJoinedIds = new HashSet<>();
        knowledge
                .builder()
                .newRule("test uni 2")
                .forEach(
                        "$a1", TypeA.class,
                        "$a2", TypeA.class,
                        "$a3", TypeA.class
                )
                .where("$a1.i * $a2.i == $a3.i")
                .execute(
                        ctx -> {
                            TypeA a1 = ctx.get("$a1");
                            TypeA a2 = ctx.get("$a2");
                            TypeA a3 = ctx.get("$a3");
                            collectedJoinedIds.add(a1.getId() + a2.getId() + a3.getId());
                        }
                )
                .build();

        try (StatefulSession s = newSession(mode)) {

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
        }
        assert collectedJoinedIds.size() == 4 : "Actual " + collectedJoinedIds;
        assert collectedJoinedIds.contains("A3A5A15");
        assert collectedJoinedIds.contains("A5A3A15");
        assert collectedJoinedIds.contains("A7A11A77");
        assert collectedJoinedIds.contains("A11A7A77");

    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void testUniType3(ActivationMode mode) {
        int rule = 0;
        RhsAssert rhsAssert = new RhsAssert(
                "$a1", TypeA.class,
                "$a2", TypeA.class,
                "$a3", TypeA.class
        );

        knowledge
                .builder()
                .newRule("test uni " + rule)
                .forEach(
                        "$a1", TypeA.class,
                        "$a2", TypeA.class,
                        "$a3", TypeA.class
                )
                .where("$a1.i + $a2.i == $a3.i")
                .where("$a3.i > $a2.i")
                .where("$a2.i > $a1.i")
                .execute(rhsAssert)
                .build();

        try (StatefulSession s = newSession(mode)) {

            TypeA a1 = new TypeA("A1");
            a1.setI(1);

            TypeA a2 = new TypeA("A2");
            a2.setI(10);

            s.insertAndFire(a1, a2);
            rhsAssert.assertCount(0).reset();

            TypeA a3 = new TypeA("A3");
            a3.setI(11);

            s.insertAndFire(a3);
        }
        rhsAssert.assertCount(1); //[a1, a2, a3]
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void testUniType4(ActivationMode mode) {
        RhsAssert rhsAssert = new RhsAssert(
                "$a1", TypeA.class,
                "$a2", TypeA.class,
                "$a3", TypeA.class
        );

        knowledge
                .builder()
                .newRule("test uni 2")
                .forEach(
                        "$a1", TypeA.class,
                        "$a2", TypeA.class,
                        "$a3", TypeA.class
                )
                .where("$a1.i + $a2.i == $a3.i")
                .where("$a2.i > $a1.i")
                .execute(rhsAssert)
                .build();

        try (StatefulSession s = newSession(mode)) {

            TypeA a1 = new TypeA("A1-1");
            a1.setI(1);

            TypeA a2 = new TypeA("A2-1");
            a2.setI(10);

            TypeA a3 = new TypeA("A3-1");
            a3.setI(11);

            TypeA a11 = new TypeA("A1-2");
            a11.setI(1);

            TypeA a22 = new TypeA("A2-2");
            a22.setI(10);

            TypeA a33 = new TypeA("A3-2");
            a33.setI(11);

            s.insertAndFire(a1, a2, a3, a11, a22, a33);
        }

        rhsAssert.assertCount(8);

    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void testAlphaBeta2(ActivationMode mode) {
        RhsAssert rhsAssert = new RhsAssert(
                "$a", TypeA.class,
                "$b", TypeB.class,
                "$c", TypeC.class
        );

        knowledge
                .builder()
                .newRule()
                .forEach(
                        fact("$a", TypeA.class),
                        fact("$b", TypeB.class),
                        fact("$c", TypeC.class)
                )
                .where("$a.i == $b.i")
                .where("$a.i > 4")
                .where("$b.i > 3")
                .execute(rhsAssert)
                .build();

        try (StatefulSession s = newSession(mode)) {

            // This insert cycle will result in 5 matching pairs of [A,B] with i=5,6,7,8,9
            for (int i = 0; i < 10; i++) {
                TypeA a = new TypeA("A" + i);
                a.setI(i);
                TypeB b = new TypeB("B" + i);
                b.setI(i);
                s.insert(a, b);
            }
            s.fire();
            rhsAssert.assertCount(0).reset();
            s.insertAndFire(new TypeC("C"));
            rhsAssert.assertCount(5).reset();

            s.fire();
            rhsAssert.assertCount(0).reset();

            s.insertAndFire(new TypeC("C"));
        }
        rhsAssert.assertCount(5);

    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void testAlpha0(ActivationMode mode) {
        RhsAssert rhsAssert = new RhsAssert(
                "$a", TypeA.class,
                "$b", TypeB.class
        );

        knowledge
                .builder()
                .newRule()
                .forEach(
                        fact("$a", TypeA.class),
                        fact("$b", TypeB.class)
                )
                .where("$a.i > 4")
                .where("$a.l > 4")
                .where("$b.l > 3")
                .where("$b.i > 3")
                .execute(rhsAssert)
                .build();

        try (StatefulSession s = newSession(mode)) {

            // This insert cycle will result in 5x6 = 30 matching pairs of [A,B]
            for (int i = 0; i < 10; i++) {
                TypeA a = new TypeA("A" + i);
                a.setAllNumeric(i);
                TypeB b = new TypeB("B" + i);
                b.setAllNumeric(i);
                s.insert(a, b);
            }
            s.fire();
        }
        rhsAssert.assertCount(30);
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void testAlpha1(ActivationMode mode) {
        RhsAssert rhsAssert = new RhsAssert(
                "$a", TypeA.class,
                "$b", TypeB.class,
                "$c", TypeC.class
        );

        knowledge
                .builder()
                .newRule("test rule 1")
                .forEach(
                        fact("$a", TypeA.class),
                        fact("$b", TypeB.class),
                        fact("$c", TypeC.class)
                )
                .where("$a.i > 4")
                .where("$b.i > 3")
                .execute(rhsAssert)
                .build();

        TypeC c1;
        TypeC c2;
        try (StatefulSession s = newSession(mode)) {

            // This insert cycle will result in 5x6 = 30 matching pairs of [A,B]
            for (int i = 0; i < 10; i++) {
                TypeA a = new TypeA("A" + i);
                a.setI(i);
                TypeB b = new TypeB("B" + i);
                b.setI(i);
                s.insert(a, b);
            }
            s.fire();
            rhsAssert.assertCount(0).reset();
            c1 = new TypeC("C");
            c2 = new TypeC("C");
            s.insert(c1);
            s.fire();
            rhsAssert
                    .assertCount(30)
                    .assertContains("$c", c1)
                    .reset();
            s.insert(c2);
            s.fire();
        }
        rhsAssert
                .assertCount(30)
                .assertNotContains("$c", c1)
                .assertContains("$c", c2);
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void testAlpha2(ActivationMode mode) {
        RhsAssert rhsAssert = new RhsAssert(
                "$a", TypeA.class,
                "$b", TypeB.class,
                "$c", TypeC.class
        );

        knowledge
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

        try (StatefulSession session = newSession(mode)) {

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
        }
        rhsAssert
                .assertCount(30 * 3)
                .assertUniqueCount("$a", 5)
                .assertUniqueCount("$b", 6)
                .assertUniqueCount("$c", 3);
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void testAlpha3(ActivationMode mode) {
        Configuration conf = knowledge.getConfiguration();
        conf.setProperty(Configuration.WARN_UNKNOWN_TYPES, "false");
        assert !knowledge.getConfiguration().getAsBoolean(Configuration.WARN_UNKNOWN_TYPES);
        RhsAssert rhsAssert1 = new RhsAssert("$a", TypeA.class);
        RhsAssert rhsAssert2 = new RhsAssert("$a", TypeA.class);
        RhsAssert rhsAssert3 = new RhsAssert("$a", TypeA.class);

        knowledge
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


        try (StatefulSession session = newSession(mode)) {

            // This insert cycle will result in 5 matching As
            for (int i = 0; i < 10; i++) {
                TypeA a = new TypeA("A" + i);
                a.setI(i);
                session.insert(a);
            }
            session.fire();
        }
        rhsAssert1.assertCount(5);
        rhsAssert2.assertCount(4);
        rhsAssert3.assertCount(3);
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void testAlpha4(ActivationMode mode) {
        RhsAssert rhsAssert1 = new RhsAssert("$a", TypeA.class);
        RhsAssert rhsAssert2 = new RhsAssert("$a", TypeA.class);
        RhsAssert rhsAssert3 = new RhsAssert("$a", TypeA.class);

        knowledge
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
                .where("$a.i <= 4") // Inverse to rule 1
                .execute(rhsAssert3)
                .build();


        try (StatefulSession s = newSession(mode)) {

            // This insert cycle will result in 5 matching As
            for (int i = 0; i < 10; i++) {
                TypeA a = new TypeA("A" + i);
                a.setI(i);
                s.insert(a);
            }
            s.fire();
        }
        rhsAssert1.assertCount(5);
        rhsAssert2.assertCount(4);
        rhsAssert3.assertCount(5);
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void testAlpha5(ActivationMode mode) {
        RhsAssert rhsAssert1 = new RhsAssert("$a", TypeA.class);
        RhsAssert rhsAssert2 = new RhsAssert("$a", TypeA.class);
        RhsAssert rhsAssert3 = new RhsAssert("$a", TypeA.class);

        knowledge
                .builder()
                .newRule("rule 1")
                .forEach(fact("$a", TypeA.class))
                .where("$a.id.equals('A5')")
                .execute(rhsAssert1)
                .newRule("rule 2")
                .forEach("$a", TypeA.class)
                .where("$a.id.equals('A7')")
                .execute(rhsAssert2)
                .newRule("rule 3")
                .forEach("$a", TypeA.class)
                .where("!$a.id.equals('A5')") // Inverse to rule 1
                .execute(rhsAssert3)
                .build();

        try (StatefulSession s = newSession(mode)) {

            for (int i = 0; i < 10; i++) {
                String id = "A" + i;
                s.insert(new TypeA(id));
            }
            s.fire();
        }

        rhsAssert1.assertCount(1);
        rhsAssert2.assertCount(1);
        rhsAssert3.assertCount(9);
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void testAlpha6(ActivationMode mode) {
        RhsAssert rhsAssert = new RhsAssert(
                "$a", TypeA.class,
                "$b", TypeB.class
        );

        knowledge
                .builder()
                .newRule()
                .forEach(
                        fact("$a", TypeA.class),
                        fact("$b", TypeB.class)
                )
                .where("$a.i > 4")
                .where("$b.i > 3")
                .execute(rhsAssert)
                .build();

        TypeA $a1;
        TypeA $a2;
        TypeB $b3;
        try (StatefulSession s = newSession(mode)) {

            $a1 = new TypeA("A1");
            $a2 = new TypeA("A2");
            $a1.setI(5);
            $a2.setI(5);
            TypeB $b1 = new TypeB("B1");
            $b1.setI(4);
            s.insertAndFire($a1, $a2, $b1);

            rhsAssert.assertCount(2).reset();
            // Assert that the rules never fires again unless there's a new data
            s.fire();
            rhsAssert.assertCount(0).reset();

            TypeB $b2 = new TypeB("B1");
            $b2.setI(1);
            s.insertAndFire($b2);
            rhsAssert.assertCount(0).reset();

            $b3 = new TypeB("B1");
            $b3.setI(4);
            s.insertAndFire($b3);
        }
        rhsAssert
                .assertCount(2)
                .assertUniqueCount("$b", 1)
                .assertContains("$b", $b3)
                .assertUniqueCount("$a", 2)
                .assertContains("$a", $a1)
                .assertContains("$a", $a2);
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void testMixed1(ActivationMode mode) {
        RhsAssert rhsAssert = new RhsAssert(
                "$a1", TypeA.class,
                "$a2", TypeA.class,
                "$b1", TypeB.class,
                "$b2", TypeB.class,
                "$c", TypeC.class,
                "$d", TypeD.class
        );

        knowledge
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

        try (StatefulSession s = newSession(mode)) {

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

            s.insertAndFire(a1, b1, a2, b2, c1, d1);
            rhsAssert.assertCount(0).reset();

            TypeC c2 = new TypeC("C2");
            c2.setI(1);
            s.insertAndFire(c2);

            rhsAssert.assertCount(4 * 4).reset();

            TypeC c3 = new TypeC("C3");
            c3.setI(100);
            s.insertAndFire(c3);
            rhsAssert.assertCount(4 * 4).reset();

            TypeD d2 = new TypeD("D2");
            s.insertAndFire(d2);
        }
        rhsAssert.assertCount(2 * 4 * 4);

    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void statefulBeta1(ActivationMode mode) {

        RhsAssert rhsAssert = new RhsAssert(
                "$a", TypeA.class,
                "$b", TypeB.class
        );
        knowledge
                .builder()
                .newRule("test alpha 1")
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class
                )
                .where("$a.i != $b.i")
                .execute(rhsAssert)
                .build();

        try (StatefulSession s = newSession(mode)) {


            TypeA a1 = new TypeA();
            a1.setAllNumeric(-1);
            a1.setId("a1");

            TypeB b1 = new TypeB();
            b1.setAllNumeric(2);
            b1.setId("b1");

            s.insertAndFire(b1, a1);
            rhsAssert.assertCount(1).reset();

            // Second batch

            TypeA a2 = new TypeA();
            a2.setAllNumeric(-1);
            a2.setId("a2");

            TypeB b2 = new TypeB();
            b2.setAllNumeric(3);
            b2.setId("b2");

            s.insertAndFire(b2, a2);
        }
        rhsAssert.assertCount(3).reset();

    }

    @Test
    void multiStep1() {
        Predicate<Object[]> beta = arr -> {
            int a1i = (int) arr[0];
            int b1i = (int) arr[1];
            return a1i != b1i;
        };


        RhsAssert rhsAssert = new RhsAssert(
                "$a", TypeA.class,
                "$b", TypeB.class
        );
        knowledge
                .builder()
                .newRule("test alpha 1")
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class
                )
                .where(beta, "$a.i", "$b.i")
                .execute(rhsAssert)
                .build();

        try (StatefulSession s1 = newSession()) {

            TypeA a1 = new TypeA("a1");
            a1.setAllNumeric(1);

            TypeA a2 = new TypeA("a2");
            a2.setAllNumeric(1);

            s1.insertAndFire(a1, a2);
            rhsAssert.assertCount(0);
            rhsAssert.reset();

            TypeA a3 = new TypeA("a3");
            a3.setAllNumeric(1);
            TypeB b1 = new TypeB("b1");
            b1.setAllNumeric(-1);

            s1.insertAndFire(a3, b1);
        }
        rhsAssert.assertCount(3);
        rhsAssert.reset();

    }


}
