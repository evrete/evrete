package org.evrete;

import org.evrete.api.StatefulSession;
import org.evrete.api.Type;
import org.evrete.api.TypeField;
import org.evrete.classes.TypeA;
import org.evrete.classes.TypeB;
import org.evrete.classes.TypeC;
import org.evrete.runtime.KnowledgeImpl;
import org.evrete.runtime.StatefulSessionImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Random;
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

    @Test
    void updateAlpha1() {
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
        StatefulSession s = knowledge.createSession();
        s.insertAndFire(a);
        assert counter.get() == 10;
        assert a.getI() == 10;
    }

    @Test
    void updateAlpha2() {
        AtomicInteger counter = new AtomicInteger();
        AtomicReference<TypeA> ref = new AtomicReference<>();
        ref.set(new TypeA());
        knowledge.newRule()
                .forEach(fact("$a", TypeA.class))
                .where("$a.d < 10.0")
                .execute(ctx -> {
                    TypeA $a = ctx.get("$a");
                    $a.setD($a.getD() + 1.01);
                    ctx.update($a);
                    counter.incrementAndGet();
                });
        StatefulSession s = knowledge.createSession();
        s.insertAndFire(ref.get());
        assert counter.get() == 10;
        assert ref.get().getD() == 10.1;
    }

    @Test
    void updateAlpha3() {
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

        knowledge.createSession().insertAndFire(ref.get());
        assert counter.get() == 10;
        assert ref.get().getStr().equals("0123456789");
    }

    @Test
    void updateBeta1() {
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
        StatefulSessionImpl s = knowledge.createSession();

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

    @Test
    void updateBetaMixed() {
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
        StatefulSessionImpl s = knowledge.createSession();
        //RuntimeRule rule = s.getRules().iterator().next();

        Collection<Object> allObjects = sessionObjects(s);
        assert allObjects.size() == 0;

        int session = 0;

        while (session++ < 50) {
            counter.set(0);
            int count = new Random().nextInt(10) + 40;
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
            assert counter.get() == count * (count - 1) : counter.get() + " vs " + count * (count - 1);

            allObjects = sessionObjects(s);
            assert allObjects.size() == count * 3;

            Collection<TypeC> col = sessionObjects(s, TypeC.class);
            assert col.size() == count;

            for (TypeC c : col) {
                c.setI(-1);
            }

            counter.set(0);
            s.update(col);
            s.fire();

            allObjects = sessionObjects(s);
            assert allObjects.size() == 3 * count;

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
        }
    }

    @Test
    void retractBeta() {

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
        StatefulSessionImpl s = knowledge.createSession();

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
        assert allObjects.size() == 6;

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

        assert primeCounter.get() == 25 : "Actual: " + primeCounter.get(); // There are 25 prime numbers in the range [2...100]
        s.close();
    }

}

