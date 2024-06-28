package org.evrete.examples.misc;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.StatefulSession;

import java.io.IOException;
import java.net.URI;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class PrimeNumbersDSLUrl {
    public static void main(String[] args) throws IOException {
        KnowledgeService service = new KnowledgeService();
        Knowledge knowledge = service
                .newKnowledge()
                .builder()
                .importRules(
                        "JAVA-SOURCE",
                        URI.create("https://www.evrete.org/examples/PrimeNumbersSource.java").toURL())
                .build();

        try (StatefulSession session = knowledge.newStatefulSession()) {
            // Inject candidates
            for (int i = 2; i <= 100; i++) {
                session.insert(i);
            }
            // Execute rules
            session.fire();
            // Printout current memory state
            session.forEachFact((handle, o) -> System.out.println(o));
        }

        service.shutdown();
    }
}
