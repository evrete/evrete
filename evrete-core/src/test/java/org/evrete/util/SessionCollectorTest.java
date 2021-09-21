package org.evrete.util;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.StatefulSession;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

class SessionCollectorTest {
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

    @Test
    void statefulTest() {
        Knowledge knowledge = service
                .newKnowledge()
                .newRule("prime numbers")
                .forEach(
                        "$i1", Integer.class,
                        "$i2", Integer.class,
                        "$i3", Integer.class
                )
                .where("$i1 * $i2 == $i3")
                .execute(ctx -> ctx.deleteFact("$i3"));

        StatefulSession sess1 = knowledge.newStatefulSession();
        Set<Object> set1 = new HashSet<>();
        IntStream.range(2, 101).boxed().collect(sess1.asCollector()).fire().forEachFact((factHandle, o) -> set1.add(o));


        Set<Object> set2 = new HashSet<>();
        StatefulSession sess2 = knowledge.newStatefulSession();
        for (int i = 2; i < 100; i++) {
            sess2.insert(i);
        }
        sess2.fire().forEachFact((factHandle, o) -> set2.add(o));

        assert set1.equals(set2);


    }

}