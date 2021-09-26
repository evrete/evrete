package org.evrete.samples;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.StatefulSession;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class PrimeNumbers {
    public static void main(String[] args) {
        KnowledgeService service = new KnowledgeService();
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

        try (StatefulSession session = knowledge.newStatefulSession()) {
            // Inject candidates
            for (int i = 2; i <= 100; i++) {
                session.insert(i);
            }

            // Execute rules
            session.fire();

            // Print current memory state
            session.forEachFact(System.out::println);
        }
        service.shutdown();
    }
}
