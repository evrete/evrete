package org.evrete.samples.guessingnumbers;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.StatefulSession;

public class GuessingNumbers {

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
                .execute(
                        ctx -> {
                            Player p = ctx.get("$p");
                            System.out.println("Number guessed! " + p + " is leaving the game.");
                            ctx.delete(p);
                        }
                )
                .newRule("Wrong guess, next turn")
                .forEach(
                        "$p", Player.class,
                        "$g", Guess.class
                )
                .where("$g.number != $p.secret", "$p != $g.author")
                .execute(
                        ctx -> {
                            Player p = ctx.get("$p");
                            Guess nextGuess = new Guess(p);
                            ctx.insert(nextGuess);
                            System.out.println(nextGuess);
                        }
                );

        try (StatefulSession session = knowledge.createSession()) {
            Player p1 = new Player("Ana");
            Player p2 = new Player("Andrew");

            session.insert(p1, p2);
            session.fire();
            // And nothing happens because there no rules matching Player instances only.

            // To kickstart the session we need initial Guess
            Guess initialGuess = new Guess(p1);
            session.insert(initialGuess);
            session.fire();

            // Printing the winner
            session.forEachFact(Player.class, o -> System.out.println("\nThe winner is " + o));
        }
        service.shutdown();
    }

}
