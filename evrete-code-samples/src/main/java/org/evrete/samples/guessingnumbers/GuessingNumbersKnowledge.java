package org.evrete.samples.guessingnumbers;

import org.evrete.api.RhsContext;
import org.evrete.dsl.annotation.Fact;
import org.evrete.dsl.annotation.Rule;
import org.evrete.dsl.annotation.Where;

@SuppressWarnings("ALL")
public class GuessingNumbersKnowledge {

    @Rule("Number is guessed")
    @Where({
            "$guess.number == $player.secret",
            "$player != $guess.author"
    })
    public void rule1(RhsContext ctx, @Fact("$guess") Guess g, @Fact("$player") Player p) {
        System.out.println("Number guessed! " + p + " is leaving the game.");
        ctx.delete(p);
    }

    @Rule("Number is not guessed, next turn")
    @Where({
            "$guess.number != $player.secret",
            "$player != $guess.author"
    })
    public void rule2(RhsContext ctx, @Fact("$guess") Guess g, @Fact("$player") Player p) {
        Guess newGuess = new Guess(p);
        ctx.insert(newGuess);
        System.out.println(newGuess);
    }
}
