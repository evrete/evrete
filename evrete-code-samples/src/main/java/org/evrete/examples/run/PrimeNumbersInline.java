package org.evrete.examples.run;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.StatefulSession;

public class PrimeNumbersInline {
    public static void main(String[] args) {
        KnowledgeService service = new KnowledgeService();
        Knowledge knowledge = service
                .newKnowledge()
                .builder()
                .newRule("prime numbers")
                .forEach(
                        "$i1", Integer.class,
                        "$i2", Integer.class,
                        "$i3", Integer.class
                )
                .where("$i1 * $i2 == $i3")
                .execute(ctx -> ctx.deleteFact("$i3"))
                .build();

        // Stateful sessions are AutoCloseable
        try (StatefulSession session = knowledge.newStatefulSession()) {
            // Inject candidates
            for (int i = 2; i <= 100; i++) {
                session.insert(i);
            }
            // Execute rules
            session.fire();
            // Print current memory state
            session.forEachFact(o -> System.out.print(o + " "));
        }
        service.shutdown();
    }
}
