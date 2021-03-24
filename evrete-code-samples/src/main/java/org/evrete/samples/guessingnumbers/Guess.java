package org.evrete.samples.guessingnumbers;

import java.util.Random;

@SuppressWarnings("ALL")
public class Guess {
    private static final int RANGE = 10;

    public Player author;
    public int number;

    public Guess(Player author) {
        this.author = author;
        this.number = randomNumber();
    }

    static int randomNumber() {
        return new Random().nextInt(RANGE);
    }

    @Override
    public String toString() {
        return author + " says '" + number + "'";
    }
}
