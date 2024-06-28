package org.evrete.examples.run;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.RhsContext;
import org.evrete.api.StatefulSession;
import org.evrete.dsl.annotation.Fact;
import org.evrete.dsl.annotation.Rule;
import org.evrete.dsl.annotation.Where;

import java.util.Random;

public class GuessingNumbersAnnotated {
    private static final int RANGE = 10;

    public static void main(String[] args) throws Exception {
        KnowledgeService service = new KnowledgeService();
        Knowledge knowledge = service
                .newKnowledge()
                .builder()
                .importRules(
                        "JAVA-CLASS",
                        GuessingNumbersKnowledge.class
                )
                .build();

        try (StatefulSession session = knowledge.newStatefulSession()) {
            Player p1 = new Player("Andrew");
            Player p2 = new Player("Anna");
            Guess startingGuess = new Guess(p1);
            session.insertAndFire(p1, p2, startingGuess);
        }

        service.shutdown();
    }

    private static int randomNumber() {
        return new Random().nextInt(RANGE);
    }

    public static class GuessingNumbersKnowledge {
        @Rule
        @Where({"$g.number == $p.secret", "$p != $g.author"})
        public void numberGuessed(RhsContext ctx, @Fact("$g") Guess g, @Fact("$p") Player p) {
            ctx.delete(p);
            System.out.println("Number guessed! " + p + " is leaving the game.");
        }

        @Rule
        @Where({"$g.number != $p.secret", "$p != $g.author"})
        public void wrongGuess(RhsContext ctx, @Fact("$g") Guess g, @Fact("$p") Player p) {
            Guess newGuess = new Guess(p);
            ctx.insert(newGuess);
            System.out.println(newGuess);
        }
    }

    public static class Guess {
        public Player author;
        public int number;

        public Guess(Player author) {
            this.author = author;
            this.number = randomNumber();
        }

        @Override
        public String toString() {
            return author + " says '" + number + "'";
        }
    }

    public static class Player {
        public int secret;
        public String name;

        public Player(String name) {
            this.name = name;
            this.secret = randomNumber();
        }

        @Override
        public String toString() {
            return name + " (secret=" + secret + ")";
        }
    }

}
