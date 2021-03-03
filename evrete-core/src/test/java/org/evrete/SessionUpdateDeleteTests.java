package org.evrete;

import org.evrete.api.*;
import org.evrete.classes.TypeA;
import org.evrete.classes.TypeB;
import org.evrete.classes.TypeC;
import org.evrete.helper.FactEntry;
import org.evrete.helper.RhsAssert;
import org.evrete.helper.TestUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.ToIntFunction;

import static org.evrete.api.FactBuilder.fact;
import static org.evrete.helper.TestUtils.sessionFacts;

class SessionUpdateDeleteTests {
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

    @BeforeEach
    void init() {
        knowledge = service.newKnowledge();
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
        assert counter.get() == 10 : "Actual " + counter.get() + " vs expected " + 10;
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

        Collection<FactEntry> allObjects = TestUtils.sessionFacts(s);

        // Retract ALL objects
        allObjects.forEach(e -> s.delete(e.getHandle()));
        s.fire();
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void statefulBetaUpdateDelete1(ActivationMode mode) {
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

        Collection<FactEntry> allObjects = TestUtils.sessionFacts(s);
        assert allObjects.size() == 0;

        final int size = 3;
        int count = 3;

        for (int k = 0; k < count; k++) {
            TypeA[] instancesA = new TypeA[size];
            TypeB[] instancesB = new TypeB[size];
            FactHandle[] handlesB = new FactHandle[size];

            for (int i = 0; i < size; i++) {
                TypeA a = new TypeA("A" + i + "/" + k);
                a.setAllNumeric(i);
                TypeB b = new TypeB("B" + i + "/" + k);
                b.setAllNumeric(-1);
                instancesA[i] = a;
                s.insert(a);
                instancesB[i] = b;
                handlesB[i] = s.insert(b);
            }
            rhsAssert.reset();
            s.fire();
            Collection<FactEntry> facts = TestUtils.sessionFacts(s);

            // checking the session memory
            assert facts.size() == size * 2 : "Actual: " + facts.size();
            assert sessionFacts(s, TypeA.class).size() == size;
            assert sessionFacts(s, TypeB.class).size() == size;
            // No matching conditions exist at the moment, the rule hasn't fired
            rhsAssert.assertCount(0).reset();


            // Updating a single instance of B to match the condition
            TypeB single = instancesB[0];
            single.setAllNumeric(0); // Matches TypeA.i = 0
            s.update(handlesB[0], single);
            for (int i = 0; i < instancesB.length; i++) {
                TypeB b = instancesB[i];
                if (b != single) {
                    s.delete(handlesB[i]);
                }
            }
            s.fire();

            // Assert execution && memory state
            rhsAssert
                    .assertCount(1)
                    .assertContains("$a", instancesA[0])
                    .assertContains("$b", single)
                    .reset();
            facts = TestUtils.sessionFacts(s);
            assert facts.size() == size + 1;

            // Delete all
            for (FactEntry fe : facts) {
                s.delete(fe.getHandle());
            }
            s.fire();
            facts = TestUtils.sessionFacts(s);
            assert facts.size() == 0;
        }
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void updateBeta2(ActivationMode mode) {
        AtomicInteger counter = new AtomicInteger();

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

        Collection<FactEntry> allObjects = TestUtils.sessionFacts(s);
        assert allObjects.size() == 0;


        int fireCount = 0;

        int objectCount = 16;
        while (fireCount++ < 10) {
            counter.set(0);
            for (int i = 0; i < objectCount; i++) {
                TypeA a = new TypeA("A" + i);
                a.setI(i);
                TypeB b = new TypeB("B" + i);
                b.setI(i);
                TypeC c = new TypeC("C" + i);
                c.setI(i);

                s.insert(a, b, c);
            }

            s.fire();

            allObjects = TestUtils.sessionFacts(s);
            assert allObjects.size() == objectCount * 3;

            assert counter.get() == objectCount * (objectCount - 1) : counter.get() + " vs expected " + objectCount * (objectCount - 1);


            Collection<FactEntry> col = sessionFacts(s, TypeC.class);
            assert col.size() == objectCount;

            for (FactEntry entry : col) {
                TypeC c = (TypeC) entry.getFact();
                c.setI(-1);
            }

            counter.set(0);
            for (FactEntry entry : col) {
                s.update(entry.getHandle(), entry.getFact());
            }
            s.fire();

            allObjects = TestUtils.sessionFacts(s);
            assert allObjects.size() == 3 * objectCount : allObjects.size() + " vs " + (3 * objectCount);

            assert counter.get() == objectCount * objectCount : "Actual " + counter.get() + " vs expected " + (objectCount * objectCount);
            counter.set(0);

            FactEntry single = col.iterator().next();
            for (FactEntry c : col) {
                if (c != single) {
                    s.delete(c.getHandle());
                }
            }

            s.fire();

            allObjects = TestUtils.sessionFacts(s);
            assert allObjects.size() == 2 * objectCount + 1;

            // Retract ALL objects
            allObjects.forEach(o -> s.delete(o.getHandle()));
            s.fire();

            allObjects = TestUtils.sessionFacts(s);
            assert allObjects.size() == 0;
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

        Collection<FactEntry> allObjects = TestUtils.sessionFacts(s);
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

        allObjects = TestUtils.sessionFacts(s);
        assert allObjects.size() == count * 2;

        // Retracting all
        for (FactEntry e : allObjects) {
            s.delete(e.getHandle());
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

        allObjects = TestUtils.sessionFacts(s);
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
        Collection<FactEntry> allObjects = TestUtils.sessionFacts(s);
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
        allObjects = TestUtils.sessionFacts(s);
        assert allObjects.size() == 6;

        Collection<FactEntry> cObjects = sessionFacts(s, TypeC.class);
        assert cObjects.size() == 2 : "All: " + allObjects;
        //assert endNodeData.size() == 2;

        for (FactEntry c : cObjects) {
            ((TypeC) c.getFact()).setI(-1);
        }

        counter.set(0);
        for (FactEntry e : cObjects) {
            s.update(e.getHandle(), e.getFact());
        }
        s.fire();

        allObjects = TestUtils.sessionFacts(s);
        assert allObjects.size() == 6 : "Actual: " + allObjects.size() + " vs 6";

        assert counter.get() == 4 : counter.get() + " vs 4";
        counter.set(0);

        FactEntry single = cObjects.iterator().next();
        for (FactEntry c : cObjects) {
            if (c != single) {
                s.delete(c.getHandle());
            }
        }

        s.fire();

        allObjects = TestUtils.sessionFacts(s);
        assert allObjects.size() == 2 * 2 + 1;

        // Retract ALL objects
        allObjects.forEach(h -> s.delete(h.getHandle()));
        s.fire();

        allObjects = TestUtils.sessionFacts(s);
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
        s.forEachFact((h, o) -> primeCounter.incrementAndGet());

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
        s.forEachFact((h, o) -> primeCounter.incrementAndGet());

        assert primeCounter.get() == 25 : "Actual: " + primeCounter.get(); // There are 25 prime numbers in the range [2...100]
        s.close();
    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void externalUpdate1(ActivationMode mode) {
        AtomicInteger counter = new AtomicInteger();
        StatefulSession session = knowledge
                .newRule()
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class
                )
                .where("$a.i > 0")
                .execute(
                        ctx -> counter.incrementAndGet()
                )
                .createSession()
                .setActivationMode(mode);

        TypeA a = new TypeA();
        TypeB b1 = new TypeB();
        TypeB b2 = new TypeB();

        int cnt = 3;
        a.setAllNumeric(cnt);

        FactHandle handleA = session.insert(a);
        session.insert(b1);
        session.insert(b2);
        session.fire();

        for (int i = 0; i < cnt * 3; i++) {
            a.setI(a.getI() - 1);
            session.update(handleA, a);
            session.fire();
        }

        assert counter.get() == cnt * 2 : "Actual: " + counter.get() + ", Expected: " + cnt * 2;

    }

    @ParameterizedTest
    @EnumSource(ActivationMode.class)
    void externalUpdate2(ActivationMode mode) {
        AtomicInteger counter = new AtomicInteger();
        StatefulSession session = knowledge
                .newRule()
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class
                )
                .where("$a.i > 0")
                .execute(
                        ctx -> counter.incrementAndGet()
                )
                .createSession()
                .setActivationMode(mode);

        TypeA a = new TypeA();
        TypeB b = new TypeB();

        int cnt = 2;
        a.setAllNumeric(cnt);

        FactHandle handleA = session.insert(a);
        FactHandle handleB = session.insert(b);

        session.fire();

        for (int i = 0; i < cnt; i++) {
            a.setI(a.getI() - 1);
            session.update(handleA, a);
            session.update(handleB, b);
            session.fire();
        }

        assert counter.get() == cnt : "Actual: " + counter.get() + ", Expected: " + cnt;

    }

    @Test
    void externalUpdate3() {
        AtomicInteger counter = new AtomicInteger();

        StatefulSession session = knowledge
                .newRule("rule 1")
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class
                )
                .where("$a.i == 0")
                .execute(
                        ctx -> {
                            TypeA a = ctx.get("$a");
                            a.setI(-1);
                            ctx.update(a);
                            counter.incrementAndGet();
                        }
                )
                .newRule("rule 2")
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class
                )
                .where("$a.i == -1")
                .execute(
                        ctx -> {
                            TypeA a = ctx.get("$a");
                            a.setI(2);
                            ctx.update(a);
                            counter.incrementAndGet();
                        }
                )
                .createSession()
                .setActivationMode(ActivationMode.DEFAULT);

        TypeA a = new TypeA();
        a.setAllNumeric(0);
        TypeB b = new TypeB();
        b.setAllNumeric(0);


        session.insert(a);
        FactHandle handleB = session.insert(b);
        session.fire();

        // From now on no rules will be fired
        for (int i = 0; i < 50; i++) {
            b.setI(b.getI() + 1);
            session.update(handleB, b);
            session.fire();
        }

        assert counter.get() == 2 : "Actual : " + counter.get();
    }
}

