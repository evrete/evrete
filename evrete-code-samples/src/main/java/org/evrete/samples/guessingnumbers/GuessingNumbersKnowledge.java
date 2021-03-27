package org.evrete.samples.guessingnumbers;

import org.evrete.api.RhsContext;
import org.evrete.dsl.annotation.Fact;
import org.evrete.dsl.annotation.Rule;
import org.evrete.dsl.annotation.Where;

@SuppressWarnings("ALL")
public class GuessingNumbersKnowledge {

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
