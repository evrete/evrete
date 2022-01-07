package org.evrete.benchmarks;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.StatefulSession;
import org.evrete.benchmarks.helper.SessionWrapper;
import org.evrete.benchmarks.helper.TestUtils;
import org.evrete.benchmarks.models.misc.*;
import org.evrete.util.RhsAssert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

class Drools1InverseTest {
    private static final boolean print = false;
    private static final AtomicInteger varIndexer = new AtomicInteger(0);
    private static final Class<?>[] classes2 = new Class<?>[]{
            TypeA.class,
            TypeB.class
    };
    private static final Class<?>[] classes3 = new Class<?>[]{
            TypeA.class,
            TypeB.class,
            TypeC.class,
    };
    private static final Class<?>[] classes6 = new Class<?>[]{
            TypeA.class,
            TypeB.class,
            TypeA.class,
            TypeB.class,
            TypeC.class,
            TypeD.class
    };
    private static final Random insertCountRandom = new SecureRandom();
    private static final Random valueRandom = new SecureRandom();
    private static final Random typeRandom = new SecureRandom();
    private static KnowledgeService service;

    @BeforeAll
    static void setUpClass() {
        service = new KnowledgeService();
    }

    @AfterAll
    static void shutDownClass() {
        service.shutdown();
    }

    private static List<Object> randomInput(Class<?>[] classes) {
        List<Object> insert = new ArrayList<>();
        int count = insertCountRandom.nextInt(classes.length) + 1;

        StringBuilder code = new StringBuilder(2048);
        StringJoiner varNames = new StringJoiner(", ");
        code.append("//---------------\n");
        int valMax = 8;
        for (int i = 0; i < count; i++) {
            // Pick a random type;
            int typeIndex = typeRandom.nextInt(1024) % classes.length;
            Class<?> type = classes[typeIndex];
            try {
                Base instance = (Base) type.getDeclaredConstructor().newInstance();
                int val = valueRandom.nextInt(valMax) - (valMax / 2);
                String varName = "o" + varIndexer.incrementAndGet();
                varNames.add(varName);
                instance.setId(String.valueOf(val));
                instance.setAllNumeric(val);
                insert.add(instance);
                code.append(type.getSimpleName()).append(" ").append(varName).append(" = new ").append(type.getSimpleName()).append("();\n");
                code.append(varName).append(".setAllNumeric(").append(val).append(");\n");
                code.append(varName).append(".setId(String.valueOf(").append(val).append("));\n");
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        if (print) {
            code.append("s1.insertAndFire(").append(varNames).append(");\n");
            code.append("s2.insertAndFire(").append(varNames).append(");\n");
            code.append("Drools1Rhs.assertCount(rhsAssert.getCount());\n");
            code.append("Drools1Rhs.reset();\n");
            code.append("rhsAssert.reset();\n");
            code.append("//---------------\n");
            System.out.println(code);
        }
        return insert;
    }

    @Test
    void test1() {
        Knowledge knowledge = service.newKnowledge();
        KieContainer droolsKnowledge = TestUtils.droolsKnowledge("src/test/drl/droolsTest1_1Inverse.drl");

        // $a.i == $b.i
        Predicate<Object[]> beta = arr -> {
            int a1i = (int) arr[0];
            int b1i = (int) arr[1];
            return a1i == b1i;
        };

        RhsAssert
                rhsAssert = new RhsAssert(
                "$a", TypeA.class,
                "$b", TypeB.class
        );

        knowledge.newRule("test alpha 1")
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class
                )
                .where(beta, "$a.i", "$b.i")
                .execute(rhsAssert);


        StatefulSession eSession = knowledge.newStatefulSession();
        KieSession dSession = droolsKnowledge.newKieSession();

        SessionWrapper s1 = SessionWrapper.of(eSession);
        SessionWrapper s2 = SessionWrapper.of(dSession);

        for (int i = 0; i < 4000; i++) {
            List<Object> insert = randomInput(classes2);
            s1.insertAndFire(insert.toArray());
            s2.insertAndFire(insert.toArray());
            Drools1Rhs.assertCount(rhsAssert.getCount());
            Drools1Rhs.reset();
            rhsAssert.reset();
        }
    }

    @Test
    void test2() {
        Knowledge knowledge = service.newKnowledge();
        KieContainer droolsKnowledge = TestUtils.droolsKnowledge("src/test/drl/droolsTest1_2Inverse.drl");

        // $a.i == $b.i
        Predicate<Object[]> beta = arr -> {
            int a1i = (int) arr[0];
            int b1i = (int) arr[1];
            return a1i == b1i;
        };

        // $c.i > 0
        Predicate<Object[]> alpha = arr -> {
            int i = (int) arr[0];
            return i >= 0;
        };

        RhsAssert rhsAssert = new RhsAssert(
                "$a", TypeA.class,
                "$b", TypeB.class,
                "$c", TypeC.class
        );

        knowledge.newRule("test alpha 1")
                .forEach(
                        "$a", TypeA.class,
                        "$b", TypeB.class,
                        "$c", TypeC.class
                )
                .where(beta, "$a.i", "$b.i")
                .where(alpha, "$c.i")
                .execute(rhsAssert);


        StatefulSession eSession = knowledge.newStatefulSession();
        KieSession dSession = droolsKnowledge.newKieSession();

        SessionWrapper s1 = SessionWrapper.of(eSession);
        SessionWrapper s2 = SessionWrapper.of(dSession);

        for (int i = 0; i < 512; i++) {
            List<Object> insert = randomInput(classes3);
            s1.insertAndFire(insert.toArray());
            s2.insertAndFire(insert.toArray());
            Drools1Rhs.assertCount(rhsAssert.getCount());
            Drools1Rhs.reset();
            rhsAssert.reset();
        }
    }

    @Test
    void test3() {
        Knowledge knowledge = service.newKnowledge();
        KieContainer droolsKnowledge = TestUtils.droolsKnowledge("src/test/drl/droolsTest1_3Inverse.drl");
        // $a.i == $b.i
        Predicate<Object[]> beta = arr -> {
            int a1i = (int) arr[0];
            int b1i = (int) arr[1];
            return a1i == b1i;
        };

        // $c.i > 0
        Predicate<Object[]> alpha = arr -> {
            int i = (int) arr[0];
            return i >= 0;
        };

        RhsAssert rhsAssert = new RhsAssert(
                "$a1", TypeA.class,
                "$b1", TypeB.class,
                "$a2", TypeA.class,
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
                .where(beta, "$a1.i", "$b1.i")
                .where(beta, "$a2.i", "$b2.i")
                .where(alpha, "$c.i")
                .execute(rhsAssert);


        StatefulSession eSession = knowledge.newStatefulSession();
        KieSession dSession = droolsKnowledge.newKieSession();

        SessionWrapper s1 = SessionWrapper.of(eSession);
        SessionWrapper s2 = SessionWrapper.of(dSession);


        for (int i = 0; i < 20; i++) {
            List<Object> insert = randomInput(classes6);
            s1.insertAndFire(insert.toArray());
            s2.insertAndFire(insert.toArray());
            Drools1Rhs.assertCount(rhsAssert.getCount());
            Drools1Rhs.reset();
            rhsAssert.reset();
        }
    }
}
