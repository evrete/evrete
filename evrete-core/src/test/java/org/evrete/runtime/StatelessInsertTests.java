package org.evrete.runtime;

import org.evrete.Configuration;
import org.evrete.KnowledgeService;
import org.evrete.api.*;
import org.evrete.api.builders.RuleSetBuilder;
import org.evrete.classes.*;
import org.evrete.helper.RhsAssert;
import org.evrete.helper.TestUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.evrete.api.FactBuilder.fact;

class StatelessInsertTests {
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

        StatelessSession s = kn.newStatelessSession(mode);
        RhsAssert rhsAssert = new RhsAssert(s);
        Objects.requireNonNull(s.getRule(RULE_NAME)).setRhs(rhsAssert);
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
        Collection<FactHandle> sessionObjects = new LinkedList<>();
        s.fire((factHandle, o) -> sessionObjects.add(factHandle));
        rhsAssert.assertCount((int) Math.pow(objectCount, 4));

        assert sessionObjects.size() == objectCount * 4 : "Actual: " + sessionObjects.size() + ", expected: " + objectCount * 4;

    }

    private StatelessSession newSession(ActivationMode mode) {
        return knowledge.newStatelessSession(mode);
    }

    @BeforeEach
    void init() {
        knowledge = service.newKnowledge();
    }


    @Test
    void emptyRulesTest() {
        StatelessSession session = knowledge.builder().build().newStatelessSession();
        session.fire();
    }

    @Test
    void emptyActionTest() {
        StatelessSession session = knowledge.builder()
                .newRule().forEach("$i", Integer.class)
                .execute()
                .build()
                .newStatelessSession();
        session.fire();
    }

    @Test
    void nonBlockingInvalidFieldTest() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            StatelessSession session = knowledge.builder()
                    .newRule().forEach("$i", Integer.class)
                    .where("$i.noSuchField")
                    .execute()
                    .build()
                    .newStatelessSession();
            session.fire();
        });
    }

    @Test
    void nonBlockingInvalidFactTest() {
        Assertions.assertThrows(NoSuchElementException.class, () -> {
            StatelessSession session = knowledge.builder()
                    .newRule().forEach("$i", Integer.class)
                    .where("$noSuchFact.noSuchField")
                    .execute()
                    .build()
                    .newStatelessSession();
            session.fire();
        });
    }


    @Test
    void factExistenceUponInsertTest() {
        StatelessSession session = knowledge.builder()
                .newRule()
                .forEach("$a", TypeA.class)
                .execute()
                .build()
                .newStatelessSession();

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
        session.delete(h1);
        session.delete(h1); // Subsequent delete calls must not throw anything

        assert session.getFact(h1) == null;
        assert session.getFact(h2) == a2;
    }


    @Test
    void basicRuleSortingOrder() {
        RuleSetBuilder<Knowledge> ruleSetBuilder = knowledge.builder();

        List<String> ruleNames = new ArrayList<>();
        List<String> ruleActivationSequence = new ArrayList<>();


        for (int i = 0; i < 256; i++) {
            String ruleName = "rule" + i;
            ruleNames.add(ruleName);
            ruleSetBuilder.newRule(ruleName).forEach("$a", TypeA.class)
                    .where("$a.i > 0")
                    .execute(rhsContext -> ruleActivationSequence.add(rhsContext.getRule().getName()));
        }
        Knowledge k = ruleSetBuilder.build();

        StatelessSession session = k.newStatelessSession();
        TypeA a = new TypeA();
        a.setI(999); // Will match the condition of each rule

        session.insert(a);
        session.fire();

        // Testing the sequence
        Assertions.assertEquals(ruleNames.size(), ruleActivationSequence.size());
        for (int i = 0; i < ruleActivationSequence.size(); i++) {
            String ruleName1 = ruleNames.get(i);
            String ruleName2 = ruleActivationSequence.get(i);
            Assertions.assertEquals(ruleName1, ruleName2);
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


        StatelessSession session = newSession(mode);
        // Chaining RHS
        RuntimeRule rule = session.getRules().iterator().next();
        AtomicInteger counter = new AtomicInteger();
        rule.chainRhs(ctx -> counter.incrementAndGet());

        session.insertAndFire(1, 2);
        rhsAssert.assertCount(2).reset();
        assert counter.get() == 2;
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

        StatelessSession session = newSession(mode);
        session.insertAndFire(1, 2, -1);
        rhsAssert.assertCount(2).reset();
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

        StatelessSession session1 = newSession(mode);
        StatelessSession session2 = newSession(mode);

        session1
                .builder()
                .newRule()
                .forEach("$a", TypeA.class)
                .execute()
                .build()
                ;

        assert knowledge.getSessions().size() == 2;
        session2.fire();
        assert knowledge.getSessions().size() == 1;
        session1.fire(); // Second close has no effect
        assert knowledge.getSessions().isEmpty();
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void factHandles(ActivationMode mode) {
        RhsAssert rhsAssert = new RhsAssert(
                "$a", TypeA.class
        );
        knowledge
                .builder()
                .newRule("test")
                .forEach(
                        "$a", TypeA.class
                )
                .execute(rhsAssert)
                .build();

        StatelessSession s = newSession(mode);

        TypeA a = new TypeA();
        s.insert(a);


        s.fire();
        rhsAssert.assertCount(1).reset();

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


        StatelessSession s = newSession(mode);
        RhsAssert rhsAssert = new RhsAssert(s);

        assert s.getParentContext() == knowledge;
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

        StatelessSession s = newSession(mode);

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

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void testSingleFinalNode1(ActivationMode mode) {

        AtomicInteger counter = new AtomicInteger();
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
                .execute(
                        ctx->{
                            ctx.get(TypeA.class, "$a");
                            ctx.get(TypeB.class, "$b");
                            ctx.get(TypeC.class, "$c");
                            ctx.get(TypeD.class, "$d");
                            counter.incrementAndGet();
                        }
                )
                .build();

        StatelessSession s = newSession(mode);


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

        Assertions.assertEquals(ai * bi * ci * di, counter.get());
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

        StatelessSession s = newSession(mode);

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

        s.insert(a, aa, aaa);
        s.insert(b, bb, bbb);
        s.insertAndFire(c, cc, ccc);

        rhsAssert.assertCount(3);
    }


    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void randomExpressionsTest(ActivationMode mode) {
        for (int objectCount = 1; objectCount < 8; objectCount++) {
            for (int conditions = 1; conditions < 8; conditions++) {
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

        StatelessSession s = newSession(mode);

        TypeA a = new TypeA("AA");
        a.setI(1);

        TypeB b = new TypeB("BB");
        b.setI(1);
        b.setL(1);

        TypeC c = new TypeC("CC");
        c.setL(1);

        RhsAssert rhsAssert = new RhsAssert(s);

        Objects.requireNonNull(s.getRule(ruleName))
                .setRhs(rhsAssert); // RHS can be overridden

        s.insertAndFire(a, b, c);
        rhsAssert.assertCount(1).reset();
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

        StatelessSession s = newSession(mode);

        TypeA a1 = new TypeA("A1");
        a1.setI(2);
        a1.setL(30L);

        TypeB b1 = new TypeB("B1");
        b1.setL(3);
        b1.setS((short) 5);

        RhsAssert rhsAssert = new RhsAssert(s, ruleName);

        s.insertAndFire(a1, b1);
        rhsAssert.assertCount(1).reset();
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

        StatelessSession s = newSession(mode);
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

        StatelessSession s = newSession(mode);

        TypeA a1 = new TypeA("A1");
        a1.setAllNumeric(1);

        TypeA a2 = new TypeA("A2");
        a2.setAllNumeric(2);

        TypeB b1 = new TypeB("B1");
        b1.setAllNumeric(1);

        TypeB b2 = new TypeB("B2");
        b2.setAllNumeric(2);

        s.insertAndFire(a1, a2, b1, b2);
        rhsAssert.assertCount(2).reset();
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

        StatelessSession s = newSession(mode);

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


        StatelessSession s = newSession(mode);

        TypeA a1 = new TypeA("A1");
        a1.setI(1);

        TypeB b1 = new TypeB("B1");
        b1.setD(10.0);

        TypeA a2 = new TypeA("A2");
        a2.setI(1);

        TypeB b2 = new TypeB("B2");
        b2.setD(10.0);

        s.insertAndFire(a1, b1, a2, b2);
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

        StatelessSession s = newSession(mode);

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

        StatelessSession s = newSession(mode);

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
                .newRule("test alpha 1")
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class
                )
                .where("$a.i != $b.i")
                .execute(rhsAssert)
                .build();

        StatelessSession s = newSession(mode);

        TypeA a1 = new TypeA("A1");
        a1.setI(1);

        TypeA a2 = new TypeA("A2");
        a2.setI(1);

        TypeB b1 = new TypeB("B1");
        b1.setI(10);

        TypeB b2 = new TypeB("B2");
        b2.setI(10);

        s.insertAndFire(a1, b1, a2, b2);
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

        StatelessSession s = newSession(mode);

        TypeA a1 = new TypeA("A1");
        a1.setI(1);

        TypeA a2 = new TypeA("A2");
        a2.setI(10);

        s.insertAndFire(a1, a2);
        rhsAssert.assertCount(2); // [a1, a2], [a2, a1]
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

        StatelessSession s = newSession(mode);

        TypeA a1 = new TypeA("A1");
        a1.setI(1);

        TypeA a2 = new TypeA("A2");
        a2.setI(10);

        s.insert(a1, a2);

        TypeA a3 = new TypeA("A3");
        a3.setI(11);

        s.insertAndFire(a3);
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

        StatelessSession s = newSession(mode);

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

        StatelessSession s = newSession(mode);

        // This insert cycle will result in 5 matching pairs of [A,B] with i=5,6,7,8,9
        for (int i = 0; i < 10; i++) {
            TypeA a = new TypeA("A" + i);
            a.setI(i);
            TypeB b = new TypeB("B" + i);
            b.setI(i);
            s.insert(a, b);
        }
        rhsAssert.assertCount(0).reset();
        s.insertAndFire(new TypeC("C"));
        rhsAssert.assertCount(5).reset();
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

        StatelessSession s = newSession(mode);

        // This insert cycle will result in 5x6 = 30 matching pairs of [A,B]
        for (int i = 0; i < 10; i++) {
            TypeA a = new TypeA("A" + i);
            a.setAllNumeric(i);
            TypeB b = new TypeB("B" + i);
            b.setAllNumeric(i);
            s.insert(a, b);
        }
        s.fire();
        rhsAssert.assertCount(30);
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

        StatelessSession session = newSession(mode);

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


        StatelessSession s = newSession(mode);

        // This insert cycle will result in 5 matching As
        for (int i = 0; i < 10; i++) {
            TypeA a = new TypeA("A" + i);
            a.setI(i);
            s.insert(a);
        }
        s.fire();
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

        StatelessSession s = newSession(mode);

        for (int i = 0; i < 10; i++) {
            String id = "A" + i;
            s.insert(new TypeA(id));
        }
        s.fire();

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

        StatelessSession s = newSession(mode);

        TypeA $a1 = new TypeA("A1");
        TypeA $a2 = new TypeA("A2");
        $a1.setI(5);
        $a2.setI(5);
        TypeB $b1 = new TypeB("B1");
        $b1.setI(4);
        s.insertAndFire($a1, $a2, $b1);

        rhsAssert.assertCount(2).reset();
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

        StatelessSession s = newSession(mode);

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

        StatelessSession s = newSession(mode);


        TypeA a1 = new TypeA();
        a1.setAllNumeric(-1);
        a1.setId("a1");

        TypeB b1 = new TypeB();
        b1.setAllNumeric(2);
        b1.setId("b1");

        s.insertAndFire(b1, a1);
        rhsAssert.assertCount(1).reset();

    }
}
