/*
package org.evrete;

import org.evrete.api.Knowledge;
import org.evrete.api.RuleBuilder;
import org.evrete.api.StatefulSession;
import org.evrete.classes.TypeA;
import org.evrete.classes.TypeB;
import org.evrete.classes.TypeC;
import org.evrete.classes.TypeD;
import org.evrete.helper.RhsAssert;
import org.evrete.runtime.RuleDescriptor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.evrete.api.FactBuilder.fact;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AggregateBaseTests {
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

    @Test
    void groupBase01() {
        RuleBuilder<Knowledge> builder = knowledge.newRule("rule group 01");
        builder
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class)
                .where("$a.i == $b.i")
                .having(
                        fact("$b", TypeB.class),
                        fact("$c", TypeC.class)
                )
                .where("$c.i == $a.i", "$b.i == $c.i")
                .exists()
                .setRhs(
                        ctx -> {

                        }
                )
        ;

        RuleDescriptor desc = knowledge.compileRule(builder);
        assert desc.getLhs().getAggregateDescriptors().size() == 1;
    }

    @Test
    void groupBase02() {
        RuleBuilder<Knowledge> builder = knowledge.newRule("rule group 02");
        builder
                .forEach("$a", TypeA.class, "$b", TypeB.class)
                .where("$a.i == $b.i")
                .having(
                        fact("$b", TypeB.class),
                        fact("$c", TypeC.class)
                ).where("$c.i == $a.i", "$b.i == $c.i")
                .exists()
                .having(
                        fact("$b", TypeB.class),
                        fact("$c", TypeC.class)
                )
                .where("$c.i == $a.i", "$b.i == $c.i")
                .exists();

        assert builder.getLhs().getAggregateGroups().size() == 2;
        RuleDescriptor desc = knowledge.compileRule(builder);
        assert desc.getLhs().getAggregateDescriptors().size() == 2;
    }

    @Test
    void groupBase03() {
        //External only conditions, '$a.i == $b.i' in this case
        assertThrows(IllegalStateException.class,
                () -> knowledge
                        .newRule("rule group 03")
                        .forEach("$a", TypeA.class, "$b", TypeB.class)
                        .having(
                                fact("$c", TypeC.class),
                                fact("$d", TypeD.class)
                        )
                        .where("$a.i == $b.i", "$c.i == $d.i")
                        .notExists().execute(null));
    }


    @Test
    void groupBase04() {
        RuleBuilder<Knowledge> builder = knowledge.newRule("rule group 04");
        builder
                .forEach("$a", TypeA.class, "$b", TypeB.class)
                .where("$a.i == $b.i", "$a.id != 'Test'")
                .having(
                        fact("$b", TypeB.class),
                        fact("$c", TypeC.class)
                ).where("$c.i == $a.i", "$b.i == $c.i", "$b.d > 0")
                .exists()
                .having(
                        fact("$b", TypeB.class),
                        fact("$c", TypeC.class)
                ).where("$c.i == $a.i", "$b.i == $c.i")
                .exists();

        assert builder.getLhs().getAggregateGroups().size() == 2;
        RuleDescriptor desc = knowledge.compileRule(builder);
        assert desc.getLhs().getAggregateDescriptors().size() == 2;
    }

    @Test
    void groupBase05() {
        RuleBuilder<Knowledge> builder = knowledge.newRule("rule group 05");
        builder
                .forEach("$a", TypeA.class, "$b", TypeB.class)
                .where("$a.i == $b.i", "$a.id == 'Hello'", "$a.d > 4444")
                .having(
                        fact("$b", TypeB.class),
                        fact("$c", TypeC.class)
                ).where("$c.i == $a.i", "$b.i == $c.i", "$a.id != 'Hello1'", "$a.i != 777")
                .exists();


        RuleDescriptor desc = knowledge.compileRule(builder);
        assert builder.getLhs().getAggregateGroups().size() == 1;
        assert desc.getLhs().getAggregateDescriptors().size() == 1;
    }

    @Test
    void groupBase06() {
        RuleBuilder<Knowledge> builder = knowledge.newRule("rule group 06");
        builder
                .forEach("$a", TypeA.class, "$b", TypeB.class)
                .where("$a.i == $b.i")
                .having(
                        fact("$b1", TypeB.class),
                        fact("$c1", TypeC.class)
                ).where("$c1.i == $a.l", "$b1.i == $c1.i")
                .exists()
        ;

        assert builder.getLhs().getAggregateGroups().size() == 1;
        RuleDescriptor desc = knowledge.compileRule(builder);
        assert desc.getLhs().getAggregateDescriptors().size() == 1;
    }

    @Test
    void groupBase07() {
        //Two beta clusters
        RuleBuilder<Knowledge> builder = knowledge.newRule("rule group 02");
        builder
                .forEach(
                        "$a1", TypeA.class,
                        "$b1", TypeB.class,
                        "$a2", TypeA.class,
                        "$b2", TypeB.class
                )
                .where("$a1.i == $b1.i", "$a2.i == $b2.i")
                .having(
                        fact("$b", TypeB.class),
                        fact("$c", TypeC.class)
                ).where("$c.i == $a1.i", "$b.i == $c.i").exists()
                .having(
                        fact("$b", TypeB.class),
                        fact("$c", TypeC.class)
                ).where("$c.d == $a1.d", "$b.i == $c.i").exists()
                .having(
                        fact("$b", TypeB.class),
                        fact("$c", TypeC.class)
                ).where("$c.i == $a2.i", "$b.i == $c.i").exists()
        ;

        assert builder.getLhs().getAggregateGroups().size() == 3;
        RuleDescriptor desc = knowledge.compileRule(builder);
        assert desc.getLhs().getAggregateDescriptors().size() == 3;
    }

    @Test
    void looseAggregate1() {
        AtomicInteger counter = new AtomicInteger();
        RuleBuilder<Knowledge> builder = knowledge
                .newRule();

        builder
                .forEach("$a", TypeA.class, "$b", TypeB.class)
                .where("$a.i == $b.i")
                .having(
                        fact("$b1", TypeB.class),
                        fact("$c1", TypeC.class)
                )
                .where("$b1.i * 5 == $c1.i")
                .exists()
                .having(
                        fact("$b2", TypeB.class),
                        fact("$c2", TypeC.class)
                )
                .where("$b2.i * 7 == $c2.i")
                .exists()
                .execute(ctx -> counter.incrementAndGet());


        RuleDescriptor desc = knowledge.getRuleDescriptor(builder);
        assert desc.getLhs().getAggregateDescriptors().size() == 2;
        StatefulSession session = knowledge.createSession();

        for (int i = 1; i <= 4; i++) {
            TypeA a = new TypeA(i);
            TypeB b = new TypeB(i);
            TypeC c = new TypeC(i);
            session.insert(a, b, c);
        }

        session.fire();
        assert counter.get() == 0;

        session.insertAndFire(new TypeC(5));
        assert counter.get() == 0;

        session.insertAndFire(new TypeC(6), new TypeC(7));
        assert counter.get() == 4;
        counter.set(0);

        session.insertAndFire(new TypeB(45136), new TypeC(1325), new TypeC(3246));
        assert counter.get() == 4;

    }

    @Test
    void betaToBeta1() {
        RhsAssert rhsAssert = new RhsAssert(
                "$a", TypeA.class,
                "$b", TypeB.class
        );

        RuleBuilder<Knowledge> builder = knowledge
                .newRule();

        builder
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class
                )
                .where("$a.i == $b.i")
                .having(
                        "$c1", TypeC.class,
                        "$d1", TypeD.class
                )
                .where("$c1.f == $d1.f", "$c1.d == $a.d * 10.0")
                .exists()
                .execute(rhsAssert);


        RuleDescriptor desc = knowledge.getRuleDescriptor(builder);
        assert desc.getLhs().getAggregateDescriptors().size() == 1;
        assert desc.getLhs().getAllFactTypes().length == 4;
        StatefulSession session = knowledge.createSession();


        TypeA a1 = new TypeA("a1");
        TypeA a2 = new TypeA("a2");
        a1.setI(1);
        a2.setI(2);
        a1.setD(1.0);
        a2.setD(2.0);

        TypeB b1 = new TypeB("b1");
        TypeB b2 = new TypeB("b2");
        b1.setI(1);
        b2.setI(2);

        TypeD d1 = new TypeD("d1");
        d1.setF(11.0f);

        TypeC c1 = new TypeC("c1");
        c1.setF(11.0f);
        c1.setD(a1.d * 10);


        session.insertAndFire(a1, a2, b1, b2, c1, d1);

        rhsAssert
                .assertCount(1); // Matching


        TypeC c2 = new TypeC("c2");
        c2.setF(11.0f);
        c2.setD(a2.d * 10);

        throw new UnsupportedOperationException("TODO continue the test !!!!");

        //counter.set(0);
        //session.delete(c1);
        //session.fire();
        //assert counter.get() == 0 : "Actual: " + counter.get();

        //session.insertAndFire(c1, c2);
        //assert counter.get() == 2 : "Actual: " + counter.get();

    }

    @Test
    void alphaToAlpha1() {
        AtomicInteger ruleCounter1 = new AtomicInteger();
        AtomicInteger ruleCounter2 = new AtomicInteger();

        knowledge
                // Rule 1
                .newRule()
                .forEach("$a", TypeA.class)
                .where("$a.i > 1")
                .having(fact("$b", TypeB.class))
                .where("$b.i == $a.i", "$b.d > 10.0")
                .exists()
                .execute(ctx -> ruleCounter1.decrementAndGet())
                // Rule 2
                .newRule()
                .forEach("$a", TypeA.class)
                .where("$a.i > 2")
                .having("$b", TypeB.class)
                .where("$b.i == $a.i", "$b.d > 30.0")
                .exists()
                .execute(ctx -> ruleCounter2.decrementAndGet());


        // Create session
        StatefulSession session = knowledge.createSession();


        // Prepare objects for testing
        Set<TypeA> setA = new HashSet<>();
        Set<TypeB> setB = new HashSet<>();


        for (int count = 0; count < 10; count++) {
            // Filling A
            TypeA a1 = new TypeA("a1");
            TypeA a2 = new TypeA("a2");
            TypeA a3 = new TypeA("a3");
            TypeA a4 = new TypeA("a4");
            a1.setI(1);
            a2.setI(2);
            a3.setI(3);
            a3.setI(4);

            setA.add(a1);
            setA.add(a2);
            setA.add(a3);
            setA.add(a4);

            // Filling B
            TypeB b1 = new TypeB("b1");
            TypeB b2 = new TypeB("b2");
            TypeB b3 = new TypeB("b3");
            TypeB b4 = new TypeB("b4");
            b1.setI(1);
            b1.setD(11.0);
            b2.setI(2);
            b2.setD(21.0);
            b3.setI(3);
            b3.setD(31.0);
            b4.setI(4);
            b4.setD(41.0);

            setB.add(b1);
            setB.add(b2);
            setB.add(b3);
            setB.add(b4);

        }

        // Perform manual test
        for (TypeA $a : setA) {

            // Rule 1
            boolean exists1 = false;
            for (TypeB $b : setB) {
                if ($b.i == $a.i && $b.d > 10.0) {
                    exists1 = true;
                    break;
                }
            }

            if ($a.i > 1 && exists1) {
                ruleCounter1.incrementAndGet();
            }

            // Rule 2
            boolean exists2 = false;
            for (TypeB $b : setB) {
                if ($b.i == $a.i && $b.d > 30.0) {
                    exists2 = true;
                    break;
                }
            }

            if ($a.i > 2 && exists2) {
                ruleCounter2.incrementAndGet();
            }

        }


        //This will decrease counters to zero
        session.insert(setA);
        session.insert(setB);
        session.fire();
        assert ruleCounter1.get() == 0 : "Actual: " + ruleCounter1.get();
        assert ruleCounter2.get() == 0 : "Actual: " + ruleCounter2.get();
    }

    @Test
    void alphaToAlpha2() {
        AtomicInteger ruleCounter1 = new AtomicInteger();
        AtomicInteger ruleCounter2 = new AtomicInteger();

        knowledge
                // Rule 1
                .newRule()
                .forEach("$a", TypeA.class)
                .where("$a.i > 1")
                .having(fact("$b", TypeB.class))
                .where("$b.i == $a.i", "$b.d > 10.0")
                .notExists()
                .execute(ctx -> ruleCounter1.decrementAndGet())
                // Rule 2
                .newRule()
                .forEach("$a", TypeA.class)
                .where("$a.i > 2")
                .having("$b", TypeB.class)
                .where("$b.i == $a.i", "$b.d > 60.0")
                .notExists()
                .execute(ctx -> ruleCounter2.decrementAndGet());


        // Create session
        StatefulSession session = knowledge.createSession();


        // Prepare objects for testing
        Set<TypeA> setA = new HashSet<>();
        Set<TypeB> setB = new HashSet<>();


        for (int count = 0; count < 10; count++) {
            // Filling A
            TypeA a1 = new TypeA("a1");
            TypeA a2 = new TypeA("a2");
            TypeA a3 = new TypeA("a3");
            TypeA a4 = new TypeA("a4");
            a1.setI(1);
            a2.setI(2);
            a3.setI(3);
            a3.setI(4);

            setA.add(a1);
            setA.add(a2);
            setA.add(a3);
            setA.add(a4);

            // Filling B
            TypeB b1 = new TypeB("b1");
            TypeB b2 = new TypeB("b2");
            TypeB b3 = new TypeB("b3");
            TypeB b4 = new TypeB("b4");
            b1.setI(1);
            b1.setD(11.0);
            b2.setI(2);
            b2.setD(21.0);
            b3.setI(3);
            b3.setD(31.0);
            b4.setI(4);
            b4.setD(41.0);

            setB.add(b1);
            setB.add(b2);
            setB.add(b3);
            setB.add(b4);

        }

        // Perform manual test
        for (TypeA $a : setA) {

            // Rule 1
            boolean exists1 = false;
            for (TypeB $b : setB) {
                if ($b.i == $a.i && $b.d > 10.0) {
                    exists1 = true;
                    break;
                }
            }

            if ($a.i > 1 && !exists1) {
                ruleCounter1.incrementAndGet();
            }

            // Rule 2
            boolean exists2 = false;
            for (TypeB $b : setB) {
                if ($b.i == $a.i && $b.d > 60.0) {
                    exists2 = true;
                    break;
                }
            }

            if ($a.i > 2 && !exists2) {
                ruleCounter2.incrementAndGet();
            }

        }


        //This will decrease counters to zero
        session.insert(setA);
        session.insert(setB);
        session.fire();
        assert ruleCounter1.get() == 0 : "Actual: " + ruleCounter1.get();
        assert ruleCounter2.get() == 0 : "Actual: " + ruleCounter2.get();
    }
}
*/
