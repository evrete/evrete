package org.evrete.samples;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.StatefulSession;

import java.io.IOException;
import java.net.URL;
import java.util.Random;

@SuppressWarnings("unused")
public class PrimeStringsDSLSource {
    public static void main(String[] args) throws IOException {
        KnowledgeService service = new KnowledgeService();

        Knowledge knowledge = service
                .newKnowledge(
                        "JAVA-SOURCE",
                        new URL("https://www.evrete.org/examples/PrimeStringsSource.java")
                );
        try (StatefulSession session = knowledge.createSession()) {
            session.set("random-offset", new Random().nextInt());
            // Inject candidates
            for (int i = 2; i <= 100; i++) {
                session.insert(String.valueOf(i));
            }
            // Execute rules
            session.fire();
            // Printout current memory state
            session.forEachFact(String.class, System.out::println);
        }
    }
}
