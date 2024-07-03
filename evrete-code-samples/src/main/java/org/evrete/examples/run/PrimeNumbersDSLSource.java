package org.evrete.examples.run;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.StatefulSession;

import java.io.IOException;
import java.net.URI;

public class PrimeNumbersDSLSource {
    public static void main(String[] args) throws IOException {
        KnowledgeService service = new KnowledgeService();
        Knowledge knowledge = service
                .newKnowledge()
                .importRules(
                        "JAVA-SOURCE",
                        URI.create("https://www.evrete.org/examples/PrimeNumbersSource.java").toURL());

        // Stateful sessions are AutoCloseable
        try (StatefulSession session = knowledge.newStatefulSession()) {
            // Inject candidates
            for (int i = 2; i <= 100; i++) {
                session.insert(i);
            }
            // Execute rules
            session.fire();
            // Printout current memory state
            session.forEachFact(o -> System.out.print(o + " "));
        }
        service.shutdown();
    }
}
