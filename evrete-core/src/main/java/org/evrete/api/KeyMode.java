package org.evrete.api;

public enum KeyMode {

    MAIN(false, 2), // Known key, known facts
    UNKNOWN_UNKNOWN(true, 1), // New key, new Facts
    KNOWN_UNKNOWN(true, 1) // Known key, new facts
    ;

    private final boolean delta;
    private final int deltaMask;

    KeyMode(boolean delta, int deltaMask) {
        this.delta = delta;
        this.deltaMask = deltaMask;
    }

    public int getDeltaMask() {
        return deltaMask;
    }

    public boolean isDelta() {
        return delta;
    }

    static {
        if (MAIN.ordinal() != 0) {
            throw new IllegalStateException("There is contract that the " + MAIN + " key storage always comes first");
        }
    }
}
