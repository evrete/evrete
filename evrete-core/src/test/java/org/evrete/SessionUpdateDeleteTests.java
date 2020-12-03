package org.evrete;

import org.evrete.api.ActivationMode;
import org.evrete.api.StatefulSession;
import org.evrete.api.Type;
import org.evrete.api.TypeField;
import org.evrete.classes.TypeA;
import org.evrete.classes.TypeB;
import org.evrete.classes.TypeC;
import org.evrete.helper.RhsAssert;
import org.evrete.runtime.KnowledgeImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.ToIntFunction;

import static org.evrete.api.FactBuilder.fact;
import static org.evrete.helper.TestUtils.sessionObjects;

class SessionUpdateDeleteTests {
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

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void updateAlpha1(ActivationMode mode) {
        AtomicInteger counter = new AtomicInteger();
        TypeA a = new TypeA();

        knowledge.newRule("test1")
                .forEach(fact("$a", TypeA.class))
                .where("$a.i < 10")
                .execute(ctx -> {
                    TypeA $a = ctx.get("$a");
                    $a.setI($a.getI() + 1);
                    ctx.update($a);
                    counter.incrementAndGet();
                });
        StatefulSession s = knowledge.createSession().setActivationMode(mode);
        s.insertAndFire(a);
        assert counter.get() == 10 : "Actual: " + counter.get();
        assert a.getI() == 10;
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void updateAlpha2(ActivationMode mode) {
        AtomicInteger counter = new AtomicInteger();
        TypeA ref = new TypeA();
        ref.setAllNumeric(0);
        knowledge.newRule()
                .forEach(fact("$a", TypeA.class))
                .where("$a.d < 10.0")
                .execute(ctx -> {
                    TypeA $a = ctx.get("$a");
                    $a.setD($a.getD() + 1.01);
                    ctx.update($a);
                    counter.incrementAndGet();
                });
        StatefulSession s = knowledge.createSession().setActivationMode(mode);
        s.insertAndFire(ref);
        assert counter.get() == 10 : "Actual: " + counter.get();
        assert ref.getD() == 10.1;
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void updateAlpha3(ActivationMode mode) {
        AtomicInteger counter = new AtomicInteger();

        Type<TypeA> t = knowledge.getTypeResolver().declare(TypeA.class);
        TypeField field = t.declareField("length", (ToIntFunction<TypeA>) value -> value.getStr().length());
        assert field.getValueType().equals(int.class);

        // Appending counter to the value of field 's' while its length is less than 10
        knowledge.newRule()
                .forEach(fact("$a", TypeA.class))
                .where("$a.length < 10")
                .execute(ctx -> {
                    TypeA $a = ctx.get("$a");
                    int i = counter.getAndIncrement();
                    ctx.update($a.setStr($a.getStr() + i));
                });

        AtomicReference<TypeA> ref = new AtomicReference<>(new TypeA());

        knowledge.createSession().setActivationMode(mode).insertAndFire(ref.get());
        assert counter.get() == 10;
        assert ref.get().getStr().equals("0123456789");
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void retractBeta1(ActivationMode mode) {
        knowledge.newRule()
                .forEach(
                        fact("$a", TypeA.class),
                        fact("$b", TypeB.class),
                        fact("$c", TypeC.class)
                )
                .where("$a.i == $b.i")
                .where("$a.i == $c.i")
                .execute(ctx -> {
                });
        StatefulSession s = knowledge.createSession().setActivationMode(mode);

        for (int i = 1; i < 3; i++) {
            TypeA a = new TypeA("A" + i);
            a.setI(i + 100);
            TypeB b = new TypeB("B" + i);
            b.setI(i + 100);
            TypeC c1 = new TypeC("C" + i);
            c1.setI(i + 100);
            TypeC c2 = new TypeC("C" + i);
            c2.setI(i + 1000);

            s.insert(a, b, c1, c2);
        }
        s.fire();

        Collection<Object> allObjects = sessionObjects(s);

        // Retract ALL objects
        allObjects.forEach(s::delete);
        s.fire();
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void updateBeta1(ActivationMode mode) {
        RhsAssert rhsAssert = new RhsAssert(
                "$a", TypeA.class,
                "$b", TypeB.class
        );

        knowledge.newRule("update2")
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class
                )
                .where("$a.i == $b.i")
                .execute(rhsAssert);
        StatefulSession s = knowledge.createSession().setActivationMode(mode);

        Collection<Object> allObjects = sessionObjects(s);
        assert allObjects.size() == 0;

        final int size = 1000;


        Runnable multiActions = () -> {
            TypeA[] instancesA = new TypeA[size];
            TypeB[] instancesB = new TypeB[size];

            for (int i = 0; i < size; i++) {
                TypeA a = new TypeA("A" + i);
                a.setAllNumeric(i);
                TypeB b = new TypeB("B" + i);
                b.setAllNumeric(-1);
                instancesA[i] = a;
                instancesB[i] = b;
                s.insert(a, b);
            }
            s.fire();

            // No matching conditions exist at the moment, the rule hasn't fired
            rhsAssert.assertCount(0).reset();
            Collection<Object> allObjects1 = sessionObjects(s);
            // checking the session memory
            assert allObjects1.size() == size * 2 : "Actual: " + allObjects1.size();
            assert sessionObjects(s, TypeA.class).size() == size;
            assert sessionObjects(s, TypeB.class).size() == size;


            // Updating a single instance of B to match the condition
            TypeB single = instancesB[0];
            single.setAllNumeric(5); // Matches TypeA.i = 5
            s.update(single);
            for (TypeB b : instancesB) {
                if (b != single) {
                    s.delete(b);
                }
            }
            s.fire();

            // Assert execution && memory state
            rhsAssert
                    .assertCount(1)
                    .assertContains("$a", instancesA[5])
                    .assertContains("$b", single)
                    .reset();
            allObjects1 = sessionObjects(s);
            assert allObjects1.size() == size + 1;

            // Delete all
            s.delete(allObjects1);
            s.fire();
            allObjects1 = sessionObjects(s);
            assert allObjects1.size() == 0;

        };


        multiActions.run();
        multiActions.run();
        multiActions.run();
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void updateBeta2(ActivationMode mode) {
        AtomicInteger counter = new AtomicInteger();

        knowledge.addConditionTestListener((node, evaluator, values, result) -> {
        });

        knowledge.newRule("update2")
                .forEach(
                        fact("$a", TypeA.class),
                        fact("$b", TypeB.class),
                        fact("$c", TypeC.class)
                )
                .where("$a.i == $b.i", 2.0)
                .where("$a.i != $c.i", 10.0)
                .execute(ctx -> counter.getAndIncrement());
        StatefulSession s = knowledge.createSession().setActivationMode(mode);

        Collection<Object> allObjects = sessionObjects(s);
        assert allObjects.size() == 0;


        int session = 0;

        while (session < 10) {

            counter.set(0);
            int count = 20;
            for (int i = 0; i < count; i++) {
                TypeA a = new TypeA("A" + i);
                a.setI(i);
                TypeB b = new TypeB("B" + i);
                b.setI(i);
                TypeC c = new TypeC("C" + i);
                c.setI(i);

                s.insert(a, b, c);
            }

            s.fire();

            allObjects = sessionObjects(s);
            assert allObjects.size() == count * 3;

            assert counter.get() == count * (count - 1) : counter.get() + " vs " + count * (count - 1);


            Collection<TypeC> col = sessionObjects(s, TypeC.class);
            assert col.size() == count;

            for (TypeC c : col) {
                c.setI(-1);
            }

            counter.set(0);
            s.update(col);
            s.fire();

            allObjects = sessionObjects(s);
            assert allObjects.size() == 3 * count : allObjects.size() + " vs " + (3 * count);

            assert counter.get() == count * count : counter.get() + " vs " + (count * count);
            counter.set(0);

            TypeC single = col.iterator().next();
            for (TypeC c : col) {
                if (c != single) {
                    s.delete(c);
                }
            }

            s.fire();

            allObjects = sessionObjects(s);
            assert allObjects.size() == 2 * count + 1;

            // Retract ALL objects
            allObjects.forEach(s::delete);
            s.fire();

            allObjects = sessionObjects(s);
            assert allObjects.size() == 0;

            session++;
        }
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void retractMemoryTest(ActivationMode mode) {
        AtomicInteger counter = new AtomicInteger();

        knowledge.newRule()
                .forEach(
                        fact("$a", TypeA.class),
                        fact("$b", TypeB.class)
                )
                .where("$a.i == $b.i")
                .execute(ctx -> counter.getAndIncrement());
        StatefulSession s = knowledge.createSession().setActivationMode(mode);
        //RuntimeRule rule = s.getRules().iterator().next();

        Collection<Object> allObjects = sessionObjects(s);
        assert allObjects.size() == 0;


        final int count = 200;
        for (int i = 0; i < count; i++) {
            TypeA a = new TypeA("A" + i);
            a.setI(i);
            TypeB b = new TypeB("B" + i);
            b.setI(i);
            s.insert(a, b);
        }

        s.fire();

        allObjects = sessionObjects(s);
        assert allObjects.size() == count * 2;

        // Retracting all
        for (Object o : allObjects) {
            s.delete(o);
        }
        s.fire();


        // Inserting the same data
        for (int i = 0; i < count; i++) {
            TypeA a = new TypeA("A" + i);
            a.setI(i);
            TypeB b = new TypeB("B" + i);
            b.setI(i);
            s.insert(a, b);
        }

        s.fire();

        allObjects = sessionObjects(s);
        assert allObjects.size() == count * 2;


    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void retractBeta(ActivationMode mode) {

        AtomicInteger counter = new AtomicInteger();

        knowledge.newRule()
                .forEach(
                        fact("$a", TypeA.class),
                        fact("$b", TypeB.class),
                        fact("$c", TypeC.class)
                )
                .where("$a.i == $b.i")
                .where("$a.i != $c.i")
                .execute(ctx -> counter.getAndIncrement());
        StatefulSession s = knowledge.createSession().setActivationMode(mode);

        // Initial state, zero objects
        Collection<Object> allObjects = sessionObjects(s);
        assert allObjects.size() == 0;

        TypeA a1 = new TypeA("A1");
        a1.setI(1);
        TypeB b1 = new TypeB("B1");
        b1.setI(1);
        TypeC c1 = new TypeC("C1");
        c1.setI(1);
        TypeA a2 = new TypeA("A2");
        a2.setI(2);
        TypeB b2 = new TypeB("B2");
        b2.setI(2);
        TypeC c2 = new TypeC("C2");
        c2.setI(2);

        // Insert and fire
        s.insert(a1, b1, c1);
        s.insert(a2, b2, c2);
        s.fire();

        assert counter.get() == 2;
        allObjects = sessionObjects(s);
        assert allObjects.size() == 6;

        Collection<TypeC> cObjects = sessionObjects(s, TypeC.class);
        assert cObjects.size() == 2;
        //assert endNodeData.size() == 2;

        for (TypeC c : cObjects) {
            c.setI(-1);
        }

        counter.set(0);
        s.update(cObjects);
        s.fire();

        allObjects = sessionObjects(s);
        assert allObjects.size() == 6 : "Actual: " + allObjects.size() + " vs 6";

        assert counter.get() == 4 : counter.get() + " vs 4";
        counter.set(0);

        TypeC single = cObjects.iterator().next();
        for (TypeC c : cObjects) {
            if (c != single) {
                s.delete(c);
            }
        }

        s.fire();

        allObjects = sessionObjects(s);
        assert allObjects.size() == 2 * 2 + 1;

        // Retract ALL objects
        allObjects.forEach(s::delete);
        s.fire();

        allObjects = sessionObjects(s);
        assert allObjects.size() == 0;

    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void primeNumbers1(ActivationMode mode) {
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

        StatefulSession s = knowledge.createSession().setActivationMode(mode);

        for (int i = 2; i <= 100; i++) {
            s.insert(i);
        }

        s.fire();

        AtomicInteger primeCounter = new AtomicInteger();
        s.forEachMemoryObject(o -> primeCounter.incrementAndGet());

        assert primeCounter.get() == 25 : "Actual: " + primeCounter.get(); // There are 25 prime numbers in the range [2...100]
        s.close();
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void primeNumbers2(ActivationMode mode) {
        knowledge.newRule("prime numbers")
                .forEach(
                        "$i1", Integer.class,
                        "$i2", Integer.class,
                        "$i3", Integer.class
                )
                .where("$i1 * $i2 == $i3")
                .execute(
                        ctx -> {
                            Integer i3 = ctx.get("$i3");
                            ctx.delete(i3);
                        }
                );

        StatefulSession s = knowledge.createSession().setActivationMode(mode);

        for (int i = 2; i <= 100; i++) {
            s.insert(i);
        }

        s.fire();

        AtomicInteger primeCounter = new AtomicInteger();
        s.forEachMemoryObject(o -> primeCounter.incrementAndGet());

        assert primeCounter.get() == 25 : "Actual: " + primeCounter.get(); // There are 25 prime numbers in the range [2...100]
        s.close();
    }

}

