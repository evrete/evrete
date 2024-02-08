package org.evrete;

import org.evrete.api.*;
import org.evrete.classes.*;
import org.evrete.helper.IgnoreTestInOSGi;
import org.evrete.util.NextIntSupplier;
import org.evrete.util.RhsAssert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.evrete.api.FactBuilder.fact;

class StatelessBaseTests {
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

            String s = ref.type().getName() + "." + ref.field().getName();
            joiner.add(s);
        }
        return joiner.toString();

    }

    @SuppressWarnings("unchecked")
    private static void randomExpressionsTest(ActivationMode mode, int objectCount, int conditions) {
        Knowledge kn = service.newKnowledge();

        String[] fields = new String[]{"i", "l", "d", "f"};
        Class<Object>[] fieldTypes = new Class[]{int.class, long.class, double.class, float.class};
        Function<Base, Object>[] functions = new Function[]{
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

            int lastDot = type.getName().lastIndexOf('.');
            String factName = type.getName().substring(lastDot + 1);

            NamedType factType = rootGroup.addFactDeclaration("$" + factName, type);

            for (int f = 0; f < fields.length; f++) {
                String fieldName = fields[f];
                Class<Object> fieldType = fieldTypes[f];
                Function<Base, Object> function = functions[f];
                //DeclaredField field = type.resolveField(fields[f]);
                TypeField field = type.declareField(fieldName, fieldType, function);
                if (field == null) {
                    throw new IllegalStateException();
                } else {
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


        StatelessSession s = kn.newStatelessSession(mode);
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

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void plainTest0(ActivationMode mode) {
        RhsAssert rhsAssert = new RhsAssert("$n", Integer.class);
        knowledge.newRule()
                .forEach("$n", Integer.class)
                .execute(rhsAssert);


        StatelessSession session = newSession(mode);
        // Chaining RHS
        RuntimeRule rule = session.getRules().iterator().next();
        NextIntSupplier counter = new NextIntSupplier();
        rule.chainRhs(ctx -> counter.next());

        session.insertAndFire(1, 2);
        rhsAssert.assertCount(2).reset();
        assert counter.get() == 2;
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    @IgnoreTestInOSGi
    void plainTest1(ActivationMode mode) {
        RhsAssert rhsAssert = new RhsAssert("$n", Integer.class);
        knowledge.newRule()
                .forEach("$n", Integer.class)
                .where("$n.intValue >= 0 ")
                .execute(rhsAssert);

        StatelessSession session = newSession(mode);
        session.insertAndFire(1, 2);
        rhsAssert.assertCount(2).reset();
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void createDestroy1(ActivationMode mode) {
        knowledge.newRule("test")
                .forEach(
                        fact("$a1", TypeA.class.getName()),
                        fact("$a2", TypeA.class)
                )
                .where("$a1.id == $a2.id");

        StatelessSession session1 = newSession(mode);
        StatelessSession session2 = newSession(mode);
        session1.newRule();

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
        knowledge.newRule("test")
                .forEach(
                        "$a", TypeA.class
                )
                .execute(rhsAssert);

        StatelessSession s = newSession(mode);

        TypeA a = new TypeA();
        s.insert(a);


        s.fire();
        rhsAssert.assertCount(1).reset();

    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    @IgnoreTestInOSGi
    void testMultiFinal1(ActivationMode mode) {
        knowledge.newRule()
                .forEach(
                        fact("$a", TypeA.class),
                        fact("$b", TypeB.class),
                        fact("$c", TypeC.class)
                )
                .where("$a.i != $b.i")
                .where("$c.l != $b.l")
                .execute();


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
    @IgnoreTestInOSGi
    void testMultiFinal2(ActivationMode mode) {
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
    @IgnoreTestInOSGi
    void testSingleFinalNode1(ActivationMode mode) {

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

        StatelessSession s = newSession(mode);

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

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    @IgnoreTestInOSGi
    void testCircularMultiFinal(ActivationMode mode) {

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
    @IgnoreTestInOSGi
    void randomExpressionsTest(ActivationMode mode) {
        for (int objectCount = 1; objectCount < 8; objectCount++) {
            for (int conditions = 1; conditions < 8; conditions++) {
                randomExpressionsTest(mode, objectCount, conditions);
            }
        }
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    @IgnoreTestInOSGi
    void testMultiFinal2_mini(ActivationMode mode) {
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

        StatelessSession s = newSession(mode);

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
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    @IgnoreTestInOSGi
    void testFields(ActivationMode mode) {
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
    @IgnoreTestInOSGi
    void testSingleFinalNode2(ActivationMode mode) {
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
    @IgnoreTestInOSGi
    void testBeta1(ActivationMode mode) {

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
    @IgnoreTestInOSGi
    void testAlphaBeta1(ActivationMode mode) {
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
    @IgnoreTestInOSGi
    void testSimple1(ActivationMode mode) {
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
    @IgnoreTestInOSGi
    void testSimple2(ActivationMode mode) {
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
    @IgnoreTestInOSGi
    void testSimple3(ActivationMode mode) {
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
    @IgnoreTestInOSGi
    void testSimple4(ActivationMode mode) {
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
    @IgnoreTestInOSGi
    void testUniType1(ActivationMode mode) {
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
    @IgnoreTestInOSGi
    void testUniType3(ActivationMode mode) {
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
    @IgnoreTestInOSGi
    void testUniType4(ActivationMode mode) {
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
    @IgnoreTestInOSGi
    void testAlphaBeta2(ActivationMode mode) {
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
    @IgnoreTestInOSGi
    void testAlpha0(ActivationMode mode) {
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
                .where("$a.l > 4")
                .where("$b.l > 3")
                .where("$b.i > 3")
                .execute(rhsAssert);

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
    @IgnoreTestInOSGi
    void testAlpha3(ActivationMode mode) {
        Configuration conf = knowledge.getConfiguration();
        conf.setProperty(Configuration.WARN_UNKNOWN_TYPES, "false");
        assert !knowledge.getConfiguration().getAsBoolean(Configuration.WARN_UNKNOWN_TYPES);
        RhsAssert rhsAssert1 = new RhsAssert("$a", TypeA.class);
        RhsAssert rhsAssert2 = new RhsAssert("$a", TypeA.class);
        RhsAssert rhsAssert3 = new RhsAssert("$a", TypeA.class);


        Set<String> uniqueEvaluators = new HashSet<>();
        knowledge.addListener((evaluator, values, result) -> uniqueEvaluators.add(evaluator.toString()));

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


        StatelessSession session = newSession(mode);

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

        assert uniqueEvaluators.size() == 3 : "Actual: " + uniqueEvaluators.size();
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    @IgnoreTestInOSGi
    void testAlpha4(ActivationMode mode) {
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
    @IgnoreTestInOSGi
    void testAlpha5(ActivationMode mode) {
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
    @IgnoreTestInOSGi
    void testAlpha6(ActivationMode mode) {
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
    @IgnoreTestInOSGi
    void testMixed1(ActivationMode mode) {
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
    @IgnoreTestInOSGi
    void statefulBeta1(ActivationMode mode) {

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

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    @IgnoreTestInOSGi
    void exceptionHandler1(ActivationMode mode) {
        RhsAssert rhsAssert1 = new RhsAssert("$a", TypeA.class);
        RhsAssert rhsAssert2 = new RhsAssert("$a", TypeA.class);
        RhsAssert rhsAssert3 = new RhsAssert("$a", TypeA.class);

        AtomicInteger exceptionCounter = new AtomicInteger();
        knowledge.setRuleBuilderExceptionHandler((context, builder, exception) -> exceptionCounter.incrementAndGet());

        knowledge.newRule("rule 1")
                .forEach("$a", TypeA.class)
                .where("$a.i > 4")
                .execute(rhsAssert1)
                .newRule("rule 2")
                .forEach("$a", TypeA.class)
                .where("$a.i > five") // Malformed condition
                .execute(rhsAssert2)
                .newRule("rule 3")
                .forEach("$a", TypeA.class)
                .where("$a.i <= 4") // Inverse to rule 1
                .execute(rhsAssert3);


        StatelessSession s = newSession(mode);

        // This insert cycle will result in 5 matching As
        for (int i = 0; i < 10; i++) {
            TypeA a = new TypeA("A" + i);
            a.setI(i);
            s.insert(a);
        }
        s.fire();
        rhsAssert1.assertCount(5);
        rhsAssert2.assertCount(0); // The second rule has been skipped
        rhsAssert3.assertCount(5);

        assert exceptionCounter.get() == 1;
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    @IgnoreTestInOSGi
    void exceptionHandler2(ActivationMode mode) {

        AtomicInteger exceptionCounter = new AtomicInteger();
        AtomicInteger rhsCounter = new AtomicInteger();
        knowledge.setRuleBuilderExceptionHandler((context, builder, exception) -> exceptionCounter.incrementAndGet());

        Consumer<RhsContext> rhs = rhsContext -> rhsCounter.incrementAndGet();

        knowledge.newRule("rule 1")
                .forEach("$a", TypeA.class)
                .where("$a.i > 4")
                .execute(rhs)
                .newRule("rule 2")
                .forEach("$a", TypeA.class)
                .where("$a.i > 5") // Malformed condition
                .execute(rhs)
                .newRule("rule 3")
                .forEach("$a", TypeA.class.getName() + "___")
                .where("$a.i <= 4") // Inverse to rule 1
                .execute(rhs);


        StatelessSession s = newSession(mode);

        // This insert cycle will result in 5 matching As
        for (int i = 0; i < 10; i++) {
            TypeA a = new TypeA("A" + i);
            a.setI(i);
            s.insert(a);
        }
        s.fire();

        assert exceptionCounter.get() == 1;
        assert rhsCounter.get() == 9; // Third rule excluded
    }

}