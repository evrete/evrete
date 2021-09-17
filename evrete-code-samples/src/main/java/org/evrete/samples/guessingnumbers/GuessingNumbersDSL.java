package org.evrete.samples.guessingnumbers;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.StatefulSession;

import java.io.IOException;

public class GuessingNumbersDSL {
    public static void main(String[] args) throws IOException {
        KnowledgeService service = new KnowledgeService();
        Knowledge knowledge = service
                .newKnowledge(
                        "JAVA-CLASS",
                        GuessingNumbersKnowledge.class
                );

        try (StatefulSession session = knowledge.newStatefulSession()) {
            Player p1 = new Player("Andrew");
            Player p2 = new Player("Anna");
            Guess startingGuess = new Guess(p1);
            session.insert(p1, p2, startingGuess);
            session.fire();
        }

        service.shutdown();

    }
}
