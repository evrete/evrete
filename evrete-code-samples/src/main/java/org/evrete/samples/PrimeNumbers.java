package org.evrete.samples;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.StatefulSession;

class PrimeNumbers {
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
                .execute(
                        ctx -> {
                            int $i3 = ctx.get("$i3");
                            ctx.delete($i3);
                        }
                );

        try (StatefulSession session = knowledge.createSession()) {
            // Inject candidates
            for (int i = 2; i <= 100; i++) {
                session.insert(i);
            }

            // Execute rules
            session.fire();

            // Print current memory state
            session.forEachFact((handle, o) -> System.out.println(o));
        }
        service.shutdown();
    }
}
