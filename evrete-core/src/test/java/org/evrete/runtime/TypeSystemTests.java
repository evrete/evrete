package org.evrete.runtime;

import org.evrete.KnowledgeService;
import org.evrete.classes.TypeA;
import org.evrete.runtime.evaluation.DefaultEvaluatorHandle;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

public class TypeSystemTests {
    private static KnowledgeService service;
    private KnowledgeRuntime knowledge;

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
        knowledge = (KnowledgeRuntime) service.newKnowledge();
    }


    @Test
    void testFactValuesOnInsert() {
        knowledge.builder().newRule("test rule")
                .forEach("$a", TypeA.class)
                .where("$a.i > 0")
                .where("$a.l > 0")
                .where("$a.f > 0")
                .execute()
                .build();



        try(StatefulSessionImpl session = (StatefulSessionImpl) knowledge.newStatefulSession()) {

            TypeA a1 = new TypeA();
            a1.setI(1);
            a1.setL(1);
            a1.setF(1f);


            DefaultFactHandle ah1 = (DefaultFactHandle) session.insert(a1);
            ActiveType type = session.getActiveType(ah1);

            assert type.getCountOfAlphaConditions() == 3;

            // 1. Counting conditions
            Set<DefaultEvaluatorHandle> alphaConditionHandles = new HashSet<>();
            type.getAlphaConditions().forEach(entry -> alphaConditionHandles.add(entry.getHandle()));
            assert alphaConditionHandles.size() == 3;



/*
            FactWrapper wrapper1 = session.getFactWrapper(ah1);
            System.out.println(wrapper1);
            // Three conditions must match the wrapper's alpha bits
            assert wrapper1.getAlphaConditionTests().length() == 3 : "Actual size: " + wrapper1.getAlphaConditionTests().length();
*/

        }

    }
}
