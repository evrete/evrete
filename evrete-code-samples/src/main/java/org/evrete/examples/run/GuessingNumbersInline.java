package org.evrete.examples.run;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.StatefulSession;

import java.util.Random;

public class GuessingNumbersInline {
    private static final int RANGE = 10;

    public static void main(String[] args) {
        KnowledgeService service = new KnowledgeService();
        Knowledge knowledge = service
                .newKnowledge()
                .newRule("Number is guessed")
                .forEach(
                        "$p", Player.class,
                        "$g", Guess.class
                )
                .where("$g.number == $p.secret", "$p != $g.author")
                .execute(ctx -> {
                    Player p = ctx.get("$p");
                    System.out.println("Number guessed! " + p + " is leaving the game.");
                    ctx.delete(p);
                })
                .newRule("Wrong guess, next turn")
                .forEach(
                        "$p", Player.class,
                        "$g", Guess.class
                )
                .where("$g.number != $p.secret", "$p != $g.author")
                .execute(ctx -> {
                    Player p = ctx.get("$p");
                    Guess nextGuess = new Guess(p);
                    ctx.insert(nextGuess);
                    System.out.println(nextGuess);
                });

        try (StatefulSession session = knowledge.newStatefulSession()) {
            Player p1 = new Player("Ana");
            Player p2 = new Player("Andrew");

            session.insert(p1, p2);
            session.fire();
            // And nothing happens because there are no rules matching
            // Player instances only.

            // To kickstart the session we need an initial Guess
            Guess initialGuess = new Guess(p1);
            session.insert(initialGuess);
            session.fire();

            // Printing the winner
            session.forEachFact(
                    Player.class,
                    o -> System.out.println("\nThe winner is " + o)
            );
        }
        service.shutdown();
    }

    private static int randomNumber() {
        return new Random().nextInt(RANGE);
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
