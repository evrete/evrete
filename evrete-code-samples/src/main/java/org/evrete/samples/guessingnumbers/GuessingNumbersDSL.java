package org.evrete.samples.guessingnumbers;

import org.evrete.KnowledgeService;
import org.evrete.api.StatefulSession;

import java.io.IOException;

public class GuessingNumbersDSL {
    public static void main(String[] args) throws IOException {
        KnowledgeService service = new KnowledgeService();
        StatefulSession session = service
                .newKnowledge()
                .appendDslRules(
                        "JAVA-CLASS",
                        GuessingNumbersKnowledge.class
                )
                .createSession();

        Player p1 = new Player("Walter");
        Player p2 = new Player("Anna");
        Guess startingGuess = new Guess(p1);

        session.insert(p1, p2, startingGuess);

        session.fire();

        // Closing resources
        session.close();
        service.shutdown();

    }
}
