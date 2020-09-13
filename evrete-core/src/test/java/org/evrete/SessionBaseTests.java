package org.evrete;

import org.evrete.api.*;
import org.evrete.classes.*;
import org.evrete.helper.RhsAssert;
import org.evrete.helper.TestUtils;
import org.evrete.runtime.KnowledgeImpl;
import org.evrete.runtime.StatefulSessionImpl;
import org.evrete.runtime.builder.FactTypeBuilder;
import org.evrete.runtime.builder.FieldReference;
import org.evrete.runtime.builder.LhsBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.evrete.api.FactBuilder.fact;

class SessionBaseTests {
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

    private static String randomCondition(FieldReference[][] references) {
        Random random = new Random(System.nanoTime());

        int varCount = 0;

        while (varCount > references.length || varCount < 2) {
            varCount = 1 + random.nextInt(references.length);
        }
        FieldReference[] descriptor = new FieldReference[varCount];
        for (int i = 0; i < varCount; i++) {
            boolean exists;
            FieldReference ref;
            do {
                exists = false;
                int type = random.nextInt(2048) % references.length;
                int field = random.nextInt(2048) % references[type].length;
                ref = references[type][field];
                for (int k = 0; k < i; k++) {
                    FieldReference test = descriptor[k];
                    if (test.type().equals(ref.type())) {
                        exists = true;
                        break;
                    }
                }
            } while (exists);
            descriptor[i] = ref;
        }

        StringJoiner joiner = new StringJoiner(" + ", "", " >= 0");

        for (FieldReference ref : descriptor) {

            String s = ref.type().getVar() + "." + ref.field().getName();
            joiner.add(s);
        }
        return joiner.toString();

    }

    @SuppressWarnings("unchecked")
    private static void randomExpressionsTest(int objectCount, int conditions) {
        Knowledge kn = service.newKnowledge();

        String[] fields = new String[]{"i", "l", "d", "f"};
        Class<?>[] fieldTypes = new Class[]{int.class, long.class, double.class, float.class};
        Function<Base, ?>[] functions = new Function[]{
                o -> ((Base) o).getI(),
                o -> ((Base) o).getL(),
                o -> ((Base) o).getD(),
                o -> ((Base) o).getF()
        };
        TypeResolver typeResolver = kn.getTypeResolver();

        Type<TypeA> aType = typeResolver.declare(TypeA.class);
        Type<TypeB> bType = typeResolver.declare(TypeB.class);
        Type<TypeC> cType = typeResolver.declare(TypeC.class);
        Type<TypeD> dType = typeResolver.declare(TypeD.class);

        Type<Base>[] allTypes = new Type[]{aType, bType, cType, dType};
        FieldReference[][] references = new FieldReference[allTypes.length][fields.length];


        RuleBuilder<Knowledge> builder = kn.newRule("random");

        LhsBuilder<Knowledge> rootGroup = builder.getLhs();

        for (int t = 0; t < allTypes.length; t++) {
            Type<Base> type = allTypes[t];

            int lastDot = type.getName().lastIndexOf(".");
            String factName = type.getName().substring(lastDot + 1);

            FactTypeBuilder factType = rootGroup.buildLhs("$" + factName, type);

            for (int f = 0; f < fields.length; f++) {
                String fieldName = fields[f];
                Class<?> fieldType = fieldTypes[f];
                Function<Base, ?> function = functions[f];
                //DeclaredField field = type.resolveField(fields[f]);
                TypeField field = type.declareField(fieldName, fieldType, function::apply);
                if (field == null) {
                    throw new IllegalStateException();
                } else {
                    //references[t][f] = factType.getCreateReference(field);
                    references[t][f] = new FieldReference() {
                        @Override
                        public TypeField field() {
                            return field;
                        }

                        @Override
                        public NamedType type() {
                            return factType;
                        }
                    };
                }
            }
        }

        for (int c = 0; c < conditions; c++) {
            rootGroup.where(randomCondition(references));
        }

        rootGroup.execute();


        StatefulSession s = kn.createSession();
        RhsAssert rhsAssert = new RhsAssert(s);
        s.getRule("random").setRhs(rhsAssert);
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
        //assert callCount.get() == Math.pow(objectCount, 4) : "Actual: " + callCount.get();
        rhsAssert.assertCount((int) Math.pow(objectCount, 4));

        Collection<Object> sessionObjects = TestUtils.sessionObjects(s);
        assert sessionObjects.size() == objectCount * 4;

        s.deleteAndFire(sessionObjects);
        assert TestUtils.sessionObjects(s).size() == 0;

        s.close();

    }

    @BeforeEach
    void init() {
        knowledge = (KnowledgeImpl) service.newKnowledge();
    }

    @Test
    void plainTest0() {
        RhsAssert rhsAssert = new RhsAssert("$n", Integer.class);
        knowledge.newRule()
                .forEach("$n", Integer.class)
                .execute(rhsAssert);

        StatefulSession session = knowledge.createSession();
        session.insertAndFire(1, 2);
        rhsAssert.assertCount(2).reset();
        session.insertAndFire(3);
        rhsAssert.assertCount(1).assertContains("$n", 3);
        session.close();
    }

    @Test
    void plainTest1() {
        RhsAssert rhsAssert = new RhsAssert("$n", Integer.class);
        knowledge.newRule()
                .forEach("$n", Integer.class)
                .where("$n.intValue >= 0 ")
                .execute(rhsAssert);

        StatefulSession session = knowledge.createSession();
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

    @Test
    void createDestroy1() {
        knowledge.newRule("test")
                .forEach(
                        fact("$a1", TypeA.class.getName()),
                        fact("$a2", TypeA.class)
                )
                .where("$a1.id == $a2.id");

        StatefulSessionImpl session1 = knowledge.createSession();
        StatefulSessionImpl session2 = knowledge.createSession();
        session1.newRule();

        assert knowledge.getSessions().size() == 2;
        session2.close();
        assert knowledge.getSessions().size() == 1;
        session2.close(); // Second close has no effect
        session1.close();
        assert knowledge.getSessions().isEmpty();
    }

    @Test
    void testMultiFinal1() {
        knowledge.newRule()
                .forEach(
                        fact("$a", TypeA.class),
                        fact("$b", TypeB.class),
                        fact("$c", TypeC.class)
                )
                .where("$a.i != $b.i")
                .where("$c.l != $b.l")
                .execute();

        StatefulSession s = knowledge.createSession();
        RhsAssert rhsAssert = new RhsAssert(s);
        TypeA a = new TypeA("A");
        a.setI(1);
        a.setL(1);

        TypeA aa = new TypeA("AA");
        aa.setI(11);
        aa.setL(11);

        TypeA aaa = new TypeA("AAA");
        aaa.setI(111);
        aaa.setL(111);

        TypeB b = new TypeB("B");
        b.setI(2);
        b.setL(2);

        TypeB bb = new TypeB("BB");
        bb.setI(22);
        bb.setL(22);

        TypeB bbb = new TypeB("BBB");
        bbb.setI(222);
        bbb.setL(222);

        TypeC c = new TypeC("C");
        c.setI(3);
        c.setL(3);

        TypeC cc = new TypeC("CC");
        cc.setI(33);
        cc.setL(33);

        TypeC ccc = new TypeC("CCC");
        ccc.setI(333);
        ccc.setL(333);

        s.insertAndFire(a, aa, aaa, b, bb, bbb, c, cc, ccc);
        rhsAssert.assertCount(27);
    }

    @Test
    void testMultiFinal2() {
        String ruleName = "testMultiFinal2";

        knowledge.newRule(ruleName)
                .forEach(
                        fact("$a", TypeA.class),
                        fact("$b", TypeB.class),
                        fact("$c", TypeC.class)
                )
                .where(
                        "$a.i == $b.i",
                        "$c.l == $b.l"
                ).execute();

        StatefulSession s = knowledge.createSession();

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

    @Test
    void testSingleFinalNode1() {

        knowledge.newRule("testSingleFinalNode1")
                .forEach(
                        fact("$a", TypeA.class),
                        fact("$b", TypeB.class),
                        fact("$c", TypeC.class),
                        fact("$d", TypeD.class)
                )
                .where("$a.i != $b.i")
                .where("$a.i != $c.i")
                .where("$a.i != $d.i")
                .execute();


        StatefulSession s = knowledge.createSession();

        RhsAssert rhsAssert = new RhsAssert(s);

        int ai = new Random().nextInt(20) + 1;
        int bi = new Random().nextInt(20) + 1;
        int ci = new Random().nextInt(20) + 1;
        int di = new Random().nextInt(20) + 1;

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
                .assertUniqueCount("$a", ai)
                .assertUniqueCount("$b", bi)
                .assertUniqueCount("$c", ci)
                .assertUniqueCount("$d", di)
                .assertCount(ai * bi * ci * di);
    }

    @Test
    void testCircularMultiFinal() {

        knowledge.newRule("test circular")
                .forEach(
                        fact("$a", TypeA.class),
                        fact("$b", TypeB.class),
                        fact("$c", TypeC.class)
                ).where(
                "$a.i == $b.i",
                "$c.l == $b.l",
                "$c.i == $a.l")
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
    void randomExpressionsTest() {
        for (int objectCount = 1; objectCount < 10; objectCount++) {
            for (int conditions = 1; conditions < 10; conditions++) {
                randomExpressionsTest(objectCount, conditions);
            }
        }
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
                .where(
                        "$a.i == $b.i",
                        "$c.l == $b.l"
                )
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

        s.close();
    }

    @Test
    void testFields() {
        String ruleName = "testMultiFields";

        knowledge.newRule(ruleName)
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class
                )
                .where(
                        "$a.i * $b.l * $b.s == $a.l"
                )
                .execute();

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
    void testSingleFinalNode2() {
        knowledge.newRule("testSingleFinalNode2")
                .forEach(
                        fact("$a", TypeA.class),
                        fact("$b", TypeB.class),
                        fact("$c", TypeC.class),
                        fact("$d", TypeD.class)
                )
                .where("$a.i == $b.i")
                .where("$a.i == $c.i")
                .where("$a.i == $d.i")
                .execute();
        StatefulSession s = knowledge.createSession();
        RhsAssert rhsAssert = new RhsAssert(s);

        int count = new Random().nextInt(200) + 1;

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


    @Test
    void testBeta1() {

        RhsAssert rhsAssert = new RhsAssert(
                "$a", TypeA.class,
                "$b", TypeB.class
        );

        knowledge.newRule()
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class
                )
                .where("$a.i == $b.i")
                .execute(rhsAssert);

        StatefulSessionImpl s = knowledge.createSession();

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

        TypeA a1_1 = new TypeA("A1_1");
        a1_1.setAllNumeric(1);
        s.insertAndFire(a1_1);
        rhsAssert.assertCount(1);
        rhsAssert.assertContains("$a", a1_1);
        rhsAssert.assertContains("$b", b1);
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

        knowledge.newRule("test alpha 1")
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
        ;

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
    void testSimple1() {
        RhsAssert rhsAssert = new RhsAssert(
                "$a", TypeA.class,
                "$b", TypeB.class
        );

        knowledge.newRule()
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class
                )
                .where("$a.i < $b.d")
                .execute(rhsAssert);


        StatefulSessionImpl s = knowledge.createSession();

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

    @Test
    void testSimple2() {
        RhsAssert rhsAssert = new RhsAssert(
                "$a", TypeA.class,
                "$b", TypeB.class
        );


        knowledge.newRule()
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class
                )
                .where("$a.i < $b.d")
                .where("$a.f < $b.l")
                .execute(rhsAssert);

        StatefulSessionImpl s = knowledge.createSession();

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

    @Test
    void testSimple3() {
        RhsAssert rhsAssert = new RhsAssert(
                "$a", TypeA.class,
                "$b", TypeB.class,
                "$c", TypeC.class,
                "$d", TypeD.class
        );


        knowledge.newRule()
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
                .execute(rhsAssert);

        StatefulSessionImpl s = knowledge.createSession();

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

    @Test
    void testSimple4() {
        RhsAssert rhsAssert = new RhsAssert(
                "$a", TypeA.class,
                "$b", TypeB.class
        );

        knowledge.newRule("test alpha 1")
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class
                )
                .where("$a.i != $b.i")
                .execute(rhsAssert);

        StatefulSessionImpl s = knowledge.createSession();

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

    @Test
    void testUniType1() {
        RhsAssert rhsAssert = new RhsAssert(
                "$a1", TypeA.class,
                "$a2", TypeA.class
        );

        knowledge.newRule("test alpha 1")
                .forEach(
                        "$a1", TypeA.class,
                        "$a2", TypeA.class
                )
                .where("$a1.i != $a2.i")
                .execute(rhsAssert);

        StatefulSessionImpl s = knowledge.createSession();

        TypeA a1 = new TypeA("A1");
        a1.setI(1);

        TypeA a2 = new TypeA("A2");
        a2.setI(10);

        s.insertAndFire(a1, a2);
        rhsAssert.assertCount(2); // [a1, a2], [a2, a1]
    }

    @Test
    void testUniType2() {
        Set<String> collectedJoinedIds = new HashSet<>();
        knowledge.newRule("test uni 2")
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
    void testUniType3() {
        int rule = 0;
        RhsAssert rhsAssert = new RhsAssert(
                "$a1", TypeA.class,
                "$a2", TypeA.class,
                "$a3", TypeA.class
        );

        knowledge.newRule("test uni " + rule)
                .forEach(
                        "$a1", TypeA.class,
                        "$a2", TypeA.class,
                        "$a3", TypeA.class
                )
                .where("$a1.i + $a2.i == $a3.i")
                .where("$a3.i > $a2.i")
                .where("$a2.i > $a1.i")
                .execute(rhsAssert);

        StatefulSessionImpl s = knowledge.createSession();

        TypeA a1 = new TypeA("A1");
        a1.setI(1);

        TypeA a2 = new TypeA("A2");
        a2.setI(10);

        s.insertAndFire(a1, a2);
        rhsAssert.assertCount(0).reset();

        TypeA a3 = new TypeA("A3");
        a3.setI(11);

        s.insertAndFire(a3);
        rhsAssert.assertCount(1); //[a1, a2, a3]
    }

    @Test
    void testUniType4() {
        RhsAssert rhsAssert = new RhsAssert(
                "$a1", TypeA.class,
                "$a2", TypeA.class,
                "$a3", TypeA.class
        );

        knowledge.newRule("test uni 2")
                .forEach(
                        "$a1", TypeA.class,
                        "$a2", TypeA.class,
                        "$a3", TypeA.class
                )
                .where("$a1.i + $a2.i == $a3.i")
                .where("$a2.i > $a1.i")
                .execute(rhsAssert);

        StatefulSessionImpl s = knowledge.createSession();

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


    @Test
    void testAlphaBeta2() {
        RhsAssert rhsAssert = new RhsAssert(
                "$a", TypeA.class,
                "$b", TypeB.class,
                "$c", TypeC.class
        );

        knowledge.newRule()
                .forEach(
                        fact("$a", TypeA.class),
                        fact("$b", TypeB.class),
                        fact("$c", TypeC.class)
                )
                .where("$a.i == $b.i")
                .where("$a.i > 4")
                .where("$b.i > 3")
                .execute(rhsAssert);

        StatefulSession s = knowledge.createSession();

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
        rhsAssert.assertCount(5);

    }

    @Test
    void testAlpha0() {
        RhsAssert rhsAssert = new RhsAssert(
                "$a", TypeA.class,
                "$b", TypeB.class
        );

        knowledge.newRule()
                .forEach(
                        fact("$a", TypeA.class),
                        fact("$b", TypeB.class)
                )
                .where("$a.i > 4")
                .where("$b.i > 3")
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
        rhsAssert.assertCount(30);
    }

    @Test
    void testAlpha1() {
        RhsAssert rhsAssert = new RhsAssert(
                "$a", TypeA.class,
                "$b", TypeB.class,
                "$c", TypeC.class
        );

        knowledge.newRule()
                .forEach(
                        fact("$a", TypeA.class),
                        fact("$b", TypeB.class),
                        fact("$c", TypeC.class)
                )
                .where("$a.i > 4")
                .where("$b.i > 3")
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

        knowledge.newRule()
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class,
                        "$c", TypeC.class
                )
                .where("$a.i > 4")
                .where("$b.i > 3")
                .where("$c.i > 6")
                .execute(rhsAssert);

        StatefulSession session = knowledge.createSession();

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

    @Test
    void testAlpha3() {
        Configuration conf = knowledge.getConfiguration();
        conf.setWarnUnknownTypes(false);
        assert !knowledge.getConfiguration().isWarnUnknownTypes();
        RhsAssert rhsAssert1 = new RhsAssert("$a", TypeA.class);
        RhsAssert rhsAssert2 = new RhsAssert("$a", TypeA.class);
        RhsAssert rhsAssert3 = new RhsAssert("$a", TypeA.class);

        knowledge.newRule("rule 1")
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


        Type<TypeA> aType = knowledge.getTypeResolver().getType(TypeA.class.getName());
        StatefulSessionImpl session = knowledge.createSession();

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

        assert session.get(aType).knownFieldSets().size() == 0;
        assert session.getAlphaConditions().size(aType) == 3;
    }

    @Test
    void testAlpha4() {
        Configuration conf = knowledge.getConfiguration();
        conf.setWarnUnknownTypes(false);
        assert !knowledge.getConfiguration().isWarnUnknownTypes();

        RhsAssert rhsAssert1 = new RhsAssert("$a", TypeA.class);
        RhsAssert rhsAssert2 = new RhsAssert("$a", TypeA.class);
        RhsAssert rhsAssert3 = new RhsAssert("$a", TypeA.class);

        knowledge.newRule("rule 1")
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
                .execute(rhsAssert3);


        Type<TypeA> aType = knowledge.getTypeResolver().getType(TypeA.class.getName());
        StatefulSessionImpl s = knowledge.createSession();

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

        assert s.get(aType).knownFieldSets().size() == 0;
    }

    @Test
    void testAlpha5() {
        Configuration conf = knowledge.getConfiguration();
        conf.setWarnUnknownTypes(false);
        assert !knowledge.getConfiguration().isWarnUnknownTypes();

        RhsAssert rhsAssert1 = new RhsAssert("$a", TypeA.class);
        RhsAssert rhsAssert2 = new RhsAssert("$a", TypeA.class);
        RhsAssert rhsAssert3 = new RhsAssert("$a", TypeA.class);

        knowledge
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
                .execute(rhsAssert3);

        Type<TypeA> aType = knowledge.getTypeResolver().getType(TypeA.class.getName());
        StatefulSessionImpl s = knowledge.createSession();

        for (int i = 0; i < 10; i++) {
            s.insert(new TypeA("A" + i));
        }
        s.fire();
        assert s.get(aType).knownFieldSets().size() == 0;

        rhsAssert1.assertCount(1);
        rhsAssert2.assertCount(1);
        rhsAssert3.assertCount(9);
    }


    @Test
    void testAlpha6() {
        RhsAssert rhsAssert = new RhsAssert(
                "$a", TypeA.class,
                "$b", TypeB.class
        );

        knowledge.newRule()
                .forEach(
                        fact("$a", TypeA.class),
                        fact("$b", TypeB.class)
                )
                .where("$a.i > 4")
                .where("$b.i > 3")
                .execute(rhsAssert);

        StatefulSession s = knowledge.createSession();

        TypeA $a1 = new TypeA("A1");
        TypeA $a2 = new TypeA("A2");
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

        TypeB $b3 = new TypeB("B1");
        $b3.setI(4);
        s.insertAndFire($b3);
        rhsAssert
                .assertCount(2)
                .assertUniqueCount("$b", 1)
                .assertContains("$b", $b3)
                .assertUniqueCount("$a", 2)
                .assertContains("$a", $a1)
                .assertContains("$a", $a2);
    }


    @Test
    void primeNumbers() {
        knowledge.newRule("prime numbers")
                .forEach(
                        "$i1", Integer.class,
                        "$i2", Integer.class,
                        "$i3", Integer.class
                )
                .where("$i1.intValue * $i2.intValue == $i3.intValue")
                .execute(
                        ctx -> {
                            Integer i3 = ctx.get("$i3");
                            ctx.delete(i3);
                        }
                );

        StatefulSession s = knowledge.createSession();

        for (int i = 2; i <= 100; i++) {
            s.insert(i);
        }

        s.fire();

        AtomicInteger primeCounter = new AtomicInteger();
        s.forEachMemoryObject(o -> primeCounter.incrementAndGet());

        assert primeCounter.get() == 25; // There are 25 prime numbers in the range [2...100]
        s.close();
    }

    @Test
    void testMixed1() {
        //TODO !!!! continue with non-unique keys
        RhsAssert rhsAssert = new RhsAssert(
                "$a1", TypeA.class,
                "$a2", TypeA.class,
                "$b1", TypeB.class,
                "$b2", TypeB.class,
                "$c", TypeC.class,
                "$d", TypeD.class
        );

        knowledge.newRule("test alpha 1")
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

        StatefulSessionImpl s = knowledge.createSession();

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
        rhsAssert.assertCount(2 * 4 * 4);

    }
}
