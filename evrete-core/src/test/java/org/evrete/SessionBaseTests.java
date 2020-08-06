package org.evrete;

import org.evrete.api.*;
import org.evrete.classes.*;
import org.evrete.helper.TestUtils;
import org.evrete.runtime.KnowledgeImpl;
import org.evrete.runtime.StatefulSessionImpl;
import org.evrete.runtime.builder.FactTypeBuilder;
import org.evrete.runtime.builder.FieldReference;
import org.evrete.runtime.builder.RootLhsBuilder;
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

        Type aType = typeResolver.declare(TypeA.class);
        Type bType = typeResolver.declare(TypeB.class);
        Type cType = typeResolver.declare(TypeC.class);
        Type dType = typeResolver.declare(TypeD.class);

        Type[] allTypes = new Type[]{aType, bType, cType, dType};
        FieldReference[][] references = new FieldReference[allTypes.length][fields.length];


        RuleBuilder<Knowledge> builder = kn.newRule("random");

        RootLhsBuilder<Knowledge> rootGroup = builder.getOutputGroup();

        for (int t = 0; t < allTypes.length; t++) {
            Type type = allTypes[t];

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

        AtomicInteger callCount = new AtomicInteger(0);
        builder.execute(ctx -> {
            assert ctx.get("$TypeA").getClass().equals(TypeA.class);
            assert ctx.get("$TypeB").getClass().equals(TypeB.class);
            assert ctx.get("$TypeC").getClass().equals(TypeC.class);
            assert ctx.get("$TypeD").getClass().equals(TypeD.class);
            callCount.incrementAndGet();
        });


        StatefulSession s = kn.createSession();

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
        assert callCount.get() == Math.pow(objectCount, 4) : "Actual: " + callCount.get();

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
        String ruleName = "testMultiFinal1";
        AtomicInteger totalRows = new AtomicInteger(0);

        knowledge.newRule(ruleName)
                .forEach(
                        fact("$a", TypeA.class),
                        fact("$b", TypeB.class),
                        fact("$c", TypeC.class)
                )
                .where("$a.i != $b.i")
                .where("$c.l != $b.l")
                .execute(ctx -> totalRows.incrementAndGet());

        StatefulSession s = knowledge.createSession();

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
        assert totalRows.get() == 27;
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
                ).execute(null);

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

        AtomicInteger totalRows = new AtomicInteger(0);
        s.getRule(ruleName)
                .setRhs(ctx -> totalRows.incrementAndGet());

        s.insertAndFire(a, aa, aaa, b, bb, bbb, c, cc, ccc);
        assert totalRows.get() == 3 : "Actual: " + totalRows.get();
    }

    @Test
    void testSingleFinalNode1() {

        final Set<TypeA> as1 = new HashSet<>();
        final Set<TypeB> bs1 = new HashSet<>();
        final Set<TypeC> cs1 = new HashSet<>();
        final Set<TypeD> ds1 = new HashSet<>();
        final AtomicInteger total = new AtomicInteger();

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


        StatefulSession s = knowledge.createSession();

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

        assert as1.size() == ai;
        assert bs1.size() == bi;
        assert cs1.size() == ci;
        assert ds1.size() == di;
        assert total.get() == ai * bi * ci * di;

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
                "$c.i == $a.l"
        )
                .execute(null);

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

        AtomicInteger totalRows = new AtomicInteger(0);
        s.getRule("test circular").setRhs(ctx -> totalRows.incrementAndGet());

        s.insertAndFire(a, aa, aaa);
        s.insertAndFire(b, bb, bbb);
        s.insertAndFire(c, cc, ccc);

        assert totalRows.get() == 3 : "Actual: " + totalRows.get();
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
                .execute(null);

        StatefulSession s = knowledge.createSession();

        TypeA a = new TypeA("AA");
        a.setI(1);

        TypeB b = new TypeB("BB");
        b.setI(1);
        b.setL(1);

        TypeC c = new TypeC("CC");
        c.setL(1);

        AtomicInteger totalRows = new AtomicInteger(0);
        s.getRule(ruleName)
                .setRhs(null) // RHS can be overridden
                .setRhs(ctx -> totalRows.incrementAndGet());

        s.insertAndFire(a, b, c);
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
        s.insertAndFire(aa, bb, cc);
        assert totalRows.get() == 2 : "Actual: " + totalRows.get();

        s.close();
    }

    @Test
    void testFields() {
        String ruleName = "testMultiFields";

        knowledge.newRule(ruleName)
                .forEach(
                        fact("$a", TypeA.class),
                        fact("$b", TypeB.class)
                )
                .where(
                        "$a.i * $b.l * $b.s == $a.l"
                )
                .execute(null);

        StatefulSession s = knowledge.createSession();

        TypeA a1 = new TypeA("A1");
        a1.setI(2);
        a1.setL(30L);

        TypeB b1 = new TypeB("B1");
        b1.setL(3);
        b1.setS((short) 5);

        AtomicInteger totalRows = new AtomicInteger(0);
        s.getRule(ruleName)
                .setRhs(null) // RHS can be overridden
                .setRhs(ctx -> totalRows.incrementAndGet());

        s.insertAndFire(a1, b1);
        assert totalRows.get() == 1 : "Actual: " + totalRows.get();

        //Second insert
        TypeA a2 = new TypeA("A2");
        a2.setI(7);
        a2.setL(693L);

        TypeB b2 = new TypeB("B2");
        b2.setL(9);
        b2.setS((short) 11);

        totalRows.set(0);
        s.insertAndFire(a2, b2);
        assert totalRows.get() == 2 : "Actual: " + totalRows.get();

        s.close();
    }

    @Test
    void testSingleFinalNode2() {
        final AtomicInteger total = new AtomicInteger();

        final Set<TypeA> as1 = new HashSet<>();
        final Set<TypeB> bs1 = new HashSet<>();
        final Set<TypeC> cs1 = new HashSet<>();
        final Set<TypeD> ds1 = new HashSet<>();

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
        StatefulSession s = knowledge.createSession();

        int count = new Random().nextInt(200) + 1;

        AtomicInteger id = new AtomicInteger(0);

        for (int i = 0; i < count; i++) {
            String stringId = String.valueOf(id.incrementAndGet());
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

        assert as1.size() == count : "Actual: " + as1.size();
        assert bs1.size() == count;
        assert cs1.size() == count;
        assert ds1.size() == count;
        assert total.get() == count : "Actual " + total.get();

    }


    @Test
    void testAlphaBeta1() {
        AtomicInteger fireCounter1 = new AtomicInteger();
        Set<TypeA> setA1 = new HashSet<>();
        Set<TypeB> setB1 = new HashSet<>();

        AtomicInteger fireCounter2 = new AtomicInteger();
        Set<TypeA> setA2 = new HashSet<>();
        Set<TypeB> setB2 = new HashSet<>();

        knowledge.newRule("test alpha 1")
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class
                )
                .where("$a.i != $b.i")
                .where("$a.d > 1")
                .where("$b.i > 10")
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
                .where("$a.i != $b.i")
                .where("$a.i < 3")
                .where("$b.f < 10")
                .execute(
                        ctx -> {
                            TypeA a = ctx.get("$a");
                            TypeB b = ctx.get("$b");
                            setA2.add(a);
                            setB2.add(b);
                            fireCounter2.incrementAndGet();
                        }
                );

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

        assert fireCounter1.get() == 2 : "Actual: " + fireCounter1.get();
        assert setB1.size() == 1 && setB1.contains(bb);
        assert setA1.size() == 2 && setA1.contains(aa) && setA1.contains(aaa);

        assert fireCounter2.get() == 2 : "Actual: " + fireCounter2.get();
        assert setB2.size() == 1 && setB2.contains(b);
        assert setA2.size() == 2 && setA2.contains(a) && setA2.contains(aa);
    }

    @Test
    void testSimple1() {
        AtomicInteger fireCounter = new AtomicInteger();

        knowledge.newRule()
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class
                )
                .where("$a.i < $b.d")
                .execute(
                        ctx -> {
                            assert ctx.get("$a") instanceof TypeA;
                            assert ctx.get("$b") instanceof TypeB;
                            fireCounter.incrementAndGet();
                        }
                );


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
        assert fireCounter.get() == 4 : "Actual: " + fireCounter.get();
    }

    @Test
    void testSimple2() {
        AtomicInteger fireCounter = new AtomicInteger();

        knowledge.newRule()
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class
                )
                .where("$a.i < $b.d")
                .where("$a.f < $b.l")
                .execute(
                        ctx -> {
                            assert ctx.get("$a") instanceof TypeA;
                            assert ctx.get("$b") instanceof TypeB;
                            fireCounter.incrementAndGet();
                        }
                );

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
        assert fireCounter.get() == 4 : "Actual: " + fireCounter.get();
    }

    @Test
    void testSimple3() {
        AtomicInteger fireCounter = new AtomicInteger();

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
                .execute(
                        ctx -> {
                            assert ctx.get("$a") instanceof TypeA;
                            assert ctx.get("$b") instanceof TypeB;
                            assert ctx.get("$c") instanceof TypeC;
                            assert ctx.get("$d") instanceof TypeD;
                            fireCounter.incrementAndGet();
                        }
                );

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
        assert fireCounter.get() == 16 : "Actual: " + fireCounter.get();
    }

    @Test
    void testSimple4() {
        AtomicInteger fireCounter = new AtomicInteger();

        knowledge.newRule("test alpha 1")
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class
                )
                .where("$a.i != $b.i")
                .execute(
                        ctx -> fireCounter.incrementAndGet()
                );

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
        assert fireCounter.get() == 4 : "Actual: " + fireCounter.get();
    }

    @Test
    void testUniType1() {
        AtomicInteger fireCounter = new AtomicInteger();

        knowledge.newRule("test alpha 1")
                .forEach(
                        "$a1", TypeA.class,
                        "$a2", TypeA.class
                )
                .where("$a1.i != $a2.i")
                .execute(
                        ctx -> fireCounter.incrementAndGet()
                );

        StatefulSessionImpl s = knowledge.createSession();

        TypeA a1 = new TypeA("A1");
        a1.setI(1);

        TypeA a2 = new TypeA("A2");
        a2.setI(10);

        s.insertAndFire(a1, a2);
        assert fireCounter.get() == 2;// [a1, a2], [a2, a1]
    }

    @Test
    void testUniType2() {
        AtomicInteger fireCounter = new AtomicInteger();

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
                            fireCounter.incrementAndGet();
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

        //for(rule = 0; rule < 20; rule++) {
        AtomicInteger fireCounter = new AtomicInteger();

        knowledge.newRule("test uni " + rule)
                .forEach(
                        "$a1", TypeA.class,
                        "$a2", TypeA.class,
                        "$a3", TypeA.class
                )
                .where("$a1.i + $a2.i == $a3.i")
                .where("$a3.i > $a2.i")
                .where("$a2.i > $a1.i")
                .execute(
                        ctx -> fireCounter.incrementAndGet()
                );

        StatefulSessionImpl s = knowledge.createSession();

        TypeA a1 = new TypeA("A1");
        a1.setI(1);

        TypeA a2 = new TypeA("A2");
        a2.setI(10);

        s.insertAndFire(a1, a2);
        assert fireCounter.get() == 0;

        TypeA a3 = new TypeA("A3");
        a3.setI(11);

        s.insertAndFire(a3);
        assert fireCounter.get() == 1; //[a1, a2, a3]

        //}


    }

    @Test
    void testUniType4() {
        AtomicInteger fireCounter = new AtomicInteger();

        knowledge.newRule("test uni 2")
                .forEach(
                        "$a1", TypeA.class,
                        "$a2", TypeA.class,
                        "$a3", TypeA.class
                )
                .where("$a1.i + $a2.i == $a3.i")
                .where("$a2.i > $a1.i")
                .execute(
                        ctx -> fireCounter.incrementAndGet()
                );

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

        assert fireCounter.get() == 8 : "Actual: " + fireCounter.get();

    }


    @Test
    void testAlphaBeta2() {
        AtomicInteger fireCounter = new AtomicInteger();

        knowledge.newRule()
                .forEach(
                        fact("$a", TypeA.class),
                        fact("$b", TypeB.class),
                        fact("$c", TypeC.class)
                )
                .where("$a.i == $b.i")
                .where("$a.i > 4")
                .where("$b.i > 3")
                .execute(
                        ctx -> fireCounter.incrementAndGet()
                );

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
        assert fireCounter.get() == 0;
        s.insert(new TypeC("C"));
        s.fire();
        assert fireCounter.get() == 5;
    }

    @Test
    void testAlpha0() {
        AtomicInteger fireCounter = new AtomicInteger();

        knowledge.newRule()
                .forEach(
                        fact("$a", TypeA.class),
                        fact("$b", TypeB.class)
                )
                .where("$a.i > 4")
                .where("$b.i > 3")
                .execute(
                        ctx -> fireCounter.incrementAndGet()
                );

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
        assert fireCounter.get() == 30 : "Actual:" + fireCounter.get();
    }

    @Test
    void testAlpha1() {
        AtomicInteger fireCounter = new AtomicInteger();

        knowledge.newRule()
                .forEach(
                        fact("$a", TypeA.class),
                        fact("$b", TypeB.class),
                        fact("$c", TypeC.class)
                )
                .where("$a.i > 4")
                .where("$b.i > 3")
                .execute(
                        ctx -> fireCounter.incrementAndGet()
                );

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
        assert fireCounter.get() == 0;
        s.insert(new TypeC("C"));
        s.fire();
        assert fireCounter.get() == 30 : "Actual:" + fireCounter.get();
    }

    @Test
    void testAlpha2() {
        AtomicInteger fireCounter = new AtomicInteger();
        Set<TypeA> setA = new HashSet<>();
        Set<TypeB> setB = new HashSet<>();
        Set<TypeC> setC = new HashSet<>();

        knowledge.newRule()
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class,
                        "$c", TypeC.class
                )
                .where("$a.i > 4")
                .where("$b.i > 3")
                .where("$c.i > 6")
                .execute(
                        ctx -> {
                            setA.add(ctx.get("$a"));
                            setB.add(ctx.get("$b"));
                            setC.add(ctx.get("$c"));
                            fireCounter.incrementAndGet();
                        }
                );

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
        assert fireCounter.get() == 0; // No instances of TypeC, no rhs

        // This insert cycle will result in 3 matching pairs of C (7,8,9)
        for (int i = 0; i < 10; i++) {
            TypeC c = new TypeC("C" + i);
            c.setI(i);
            s.insert(c);
        }
        s.fire();
        assert fireCounter.get() == 30 * 3;
        assert setA.size() == 5;
        assert setB.size() == 6;
        assert setC.size() == 3;
    }

    @Test
    void testAlpha3() {
        Configuration conf = knowledge.getConfiguration();
        conf.setWarnUnknownTypes(false);
        assert !knowledge.getConfiguration().isWarnUnknownTypes();

        AtomicInteger counter1 = new AtomicInteger();
        AtomicInteger counter2 = new AtomicInteger();
        AtomicInteger counter3 = new AtomicInteger();

        knowledge.newRule("rule 1")
                .forEach("$a", TypeA.class)
                .where("$a.i > 4")
                .execute(ctx -> counter1.incrementAndGet())
                .newRule("rule 2")
                .forEach("$a", TypeA.class)
                .where("$a.i > 5")
                .execute(ctx -> counter2.incrementAndGet())
                .newRule("rule 3")
                .forEach("$a", TypeA.class)
                .where("$a.i > 6")
                .execute(ctx -> counter3.incrementAndGet());


        Type aType = knowledge.getTypeResolver().getType(TypeA.class.getName());
        StatefulSessionImpl s = knowledge.createSession();

        // This insert cycle will result in 5 matching As
        for (int i = 0; i < 10; i++) {
            TypeA a = new TypeA("A" + i);
            a.setI(i);
            s.insert(a);
        }
        s.fire();
        assert counter1.get() == 5;
        assert counter2.get() == 4;
        assert counter3.get() == 3;

        assert s.get(aType).knownFieldSets().size() == 0;
        assert s.getAlphaConditions().size(aType) == 3;
    }

    @Test
    void testAlpha4() {
        Configuration conf = knowledge.getConfiguration();
        conf.setWarnUnknownTypes(false);
        assert !knowledge.getConfiguration().isWarnUnknownTypes();

        AtomicInteger counter1 = new AtomicInteger();
        AtomicInteger counter2 = new AtomicInteger();
        AtomicInteger counter3 = new AtomicInteger();

        knowledge.newRule("rule 1")
                .forEach(
                        fact("$a", TypeA.class)
                )
                .where("$a.i > 4")
                .execute(ctx -> counter1.incrementAndGet());
        knowledge.newRule("rule 2")
                .forEach(
                        fact("$a", TypeA.class)
                )
                .where("$a.i > 5")
                .execute(ctx -> counter2.incrementAndGet());
        knowledge.newRule("rule 3")
                .forEach(
                        fact("$a", TypeA.class)
                )
                .where("$a.i <= 4") // Inverse to rule 1
                .execute(ctx -> counter3.incrementAndGet());


        Type aType = knowledge.getTypeResolver().getType(TypeA.class.getName());
        StatefulSessionImpl s = knowledge.createSession();

        // This insert cycle will result in 5 matching As
        for (int i = 0; i < 10; i++) {
            TypeA a = new TypeA("A" + i);
            a.setI(i);
            s.insert(a);
        }
        s.fire();
        assert counter1.get() == 5;
        assert counter2.get() == 4;
        assert counter3.get() == 5;

        assert s.get(aType).knownFieldSets().size() == 0;
    }

    @Test
    void testAlpha5() {
        Configuration conf = knowledge.getConfiguration();
        conf.setWarnUnknownTypes(false);
        assert !knowledge.getConfiguration().isWarnUnknownTypes();

        AtomicInteger counter1 = new AtomicInteger();
        AtomicInteger counter2 = new AtomicInteger();
        AtomicInteger counter3 = new AtomicInteger();

        knowledge
                .newRule("rule 1")
                .forEach(fact("$a", TypeA.class))
                .where("$a.id.equals('A5')")
                .execute(ctx -> counter1.incrementAndGet())
                .newRule("rule 2")
                .forEach(
                        fact("$a", TypeA.class)
                )
                .where("$a.id.equals('A7')")
                .execute(ctx -> counter2.incrementAndGet())
                .newRule("rule 3")
                .forEach(
                        fact("$a", TypeA.class)
                )
                .where("!$a.id.equals('A5')") // Inverse to rule 1
                .execute(ctx -> counter3.incrementAndGet());

        Type aType = knowledge.getTypeResolver().getType(TypeA.class.getName());
        StatefulSessionImpl s = knowledge.createSession();

        for (int i = 0; i < 10; i++) {
            s.insert(new TypeA("A" + i));
        }
        s.fire();
        assert s.get(aType).knownFieldSets().size() == 0;

        assert counter1.get() == 1;
        assert counter2.get() == 1;
        assert counter3.get() == 9;
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
        AtomicInteger fireCounter = new AtomicInteger();

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
                .execute(
                        ctx -> fireCounter.incrementAndGet()
                );

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
        assert fireCounter.get() == 0 : "Actual: " + fireCounter.get();

        TypeC c2 = new TypeC("C2");
        c2.setI(1);
        s.insertAndFire(c2);


        assert fireCounter.get() == 4 * 4 : "Actual: " + fireCounter.get();

        TypeC c3 = new TypeC("C3");
        c3.setI(100);
        fireCounter.set(0);
        s.insertAndFire(c3);

        assert fireCounter.get() == 2 * 4 * 4 : "Actual: " + fireCounter.get();

        TypeD d2 = new TypeD("D2");
        fireCounter.set(0);
        s.insertAndFire(d2);

        assert fireCounter.get() == 2 * 2 * 4 * 4 : "Actual: " + fireCounter.get();
    }
}
