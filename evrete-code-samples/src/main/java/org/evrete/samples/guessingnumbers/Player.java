package org.evrete.samples.guessingnumbers;

@SuppressWarnings("ALL")
public class Player {
    public int secret;
    public String name;

    public Player(String name) {
        this.name = name;
        this.secret = Guess.randomNumber();
    }

    @Override
    public String toString() {
        return name + " (secret=" + secret + ")";
    }
}
