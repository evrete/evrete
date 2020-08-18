package org.evrete.samples;

import org.evrete.KnowledgeService;
import org.evrete.api.StatefulSession;

class PrimeNumbers {
    public static void main(String[] args) {

        KnowledgeService service = new KnowledgeService();
        StatefulSession session = service
                .newKnowledge()
                .newRule("prime numbers")
                .forEach(
                        "$i1", Integer.class,
                        "$i2", Integer.class,
                        "$i3", Integer.class
                )
                .where("$i1.intValue * $i2.intValue == $i3.intValue")
                .execute(
                        ctx -> {
                            int $i3 = ctx.get("$i3");
                            ctx.delete($i3);
                        }
                )
                .createSession();

        // Inject candidates
        for (int i = 2; i <= 100; i++) {
            session.insert(i);
        }

        // Execute rules
        session.fire();

        // Print current memory state
        session.forEachMemoryObject(System.out::println);

        // Closing resources
        session.close();
        service.shutdown();
    }
}
