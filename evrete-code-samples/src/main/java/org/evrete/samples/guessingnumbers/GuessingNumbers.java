package org.evrete.samples.guessingnumbers;

import org.evrete.KnowledgeService;
import org.evrete.api.StatefulSession;

public class GuessingNumbers {

    public static void main(String[] args) {
        KnowledgeService service = new KnowledgeService();
        StatefulSession session = service
                .newKnowledge()
                .newRule("Number is guessed")
                .forEach(
                        "$player", Player.class,
                        "$guess", Guess.class
                )
                .where(
                        "$guess.number == $player.secret",
                        "$player != $guess.author"
                )
                .execute(
                        ctx -> {
                            Player p = ctx.get("$player");
                            System.out.println("Number guessed! " + p + " is leaving the game.");
                            ctx.delete(p);
                        }
                )
                .newRule("Number is not guessed, next turn")
                .forEach(
                        "$player", Player.class,
                        "$guess", Guess.class
                )
                .where(
                        "$guess.number != $player.secret",
                        "$player != $guess.author"
                )
                .execute(
                        ctx -> {
                            Player p = ctx.get("$player");
                            Guess nextGuess = new Guess(p);
                            ctx.insert(nextGuess);
                            System.out.println(nextGuess);
                        }
                )
                .createSession();

        Player p1 = new Player("Andrew");
        Player p2 = new Player("Anna");
        Guess startingGuess = new Guess(p1);

        session.insert(p1, p2, startingGuess);

        session.fire();


        // Closing resources
        session.close();
        service.shutdown();
    }

}
