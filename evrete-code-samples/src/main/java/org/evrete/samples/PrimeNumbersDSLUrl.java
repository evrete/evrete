package org.evrete.samples;

import org.evrete.KnowledgeService;
import org.evrete.api.StatefulSession;

import java.io.IOException;
import java.net.URL;

@SuppressWarnings("unused")
public class PrimeNumbersDSLUrl {
    public static void main(String[] args) throws IOException {
        KnowledgeService service = new KnowledgeService();
        StatefulSession session = service
                .newKnowledge()
                .appendDslRules(
                        "JAVA-SOURCE",
                        new URL("https://www.evrete.org/examples/PrimeNumbersSource.java"))
                .createSession();

        // Inject candidates
        for (int i = 2; i <= 100; i++) {
            session.insert(i);
        }
        // Execute rules
        session.fire();
        // Printout current memory state
        session.forEachFact((handle, o) -> System.out.print(o + " "));
        session.close();
    }
}
