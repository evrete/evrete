package org.evrete.spi.minimal;

import org.evrete.Configuration;
import org.evrete.KnowledgeService;
import org.evrete.api.*;
import org.evrete.classes.TypeA;
import org.evrete.classes.TypeB;
import org.evrete.classes.TypeC;
import org.evrete.runtime.KnowledgeImpl;
import org.evrete.runtime.builder.LhsBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

class ExpressionsTest {
    private static KnowledgeService service;
    private RuleBuilder<Knowledge> rule;
    private KnowledgeImpl knowledge;

    @BeforeAll
    static void setUpClass() {
        service = new KnowledgeService(new Configuration());
    }

    @AfterAll
    static void shutDownClass() {
        service.shutdown();
    }

    @BeforeEach
    void init() {
        knowledge = (KnowledgeImpl) service.newKnowledge();
        rule = knowledge.newRule();
    }

    @Test
    void test1() {
        LhsBuilder<Knowledge> root = rule.forEach();
        assert root.buildLhs("$a", TypeA.class).getVar().equals("$a");
        root.buildLhs("$b", TypeB.class.getName());
        root.buildLhs("$c", TypeC.class.getName());
        //Evaluator ev = rule.getOutputGroup().compile("$a.i + $b.i + $c.i == 1");
        Evaluator ev = knowledge.compile("$a.i + $b.i + $c.i == 1", root.getFactTypeMapper());

        AtomicLong counter = new AtomicLong();
        Random random = new Random();
        Object[] vars = new Object[8192];
        for (int i = 0; i < vars.length; i++) {
            vars[i] = random.nextInt();
        }
        IntToValue func = i -> {
            long l = i ^ counter.incrementAndGet() % vars.length;
            return vars[(int) l];
        };

        ev.test(func); // No exception

    }

    @Test
    void test2() {
        LhsBuilder<Knowledge> root = rule.forEach();
        assert root.buildLhs("$a", TypeA.class).getVar().equals("$a");
        //Evaluator ev1 = rule.getOutputGroup().compile("$a.i == 1");
        Evaluator ev1 = knowledge.compile("$a.i == 1", root.getFactTypeMapper());

        //Evaluator ev2 = rule.getOutputGroup().compile(" $a.i ==     1   ");
        Evaluator ev2 = knowledge.compile("   $a.i ==     1     ", root.getFactTypeMapper());
        assert ev1.compare(ev2) == LogicallyComparable.RELATION_EQUALS;
    }


}