package org.evrete.api;

public enum KeyMode {

    MAIN(2), // Known key, known facts
    UNKNOWN_UNKNOWN(1), // New key, new Facts
    KNOWN_UNKNOWN(1) // Known key, new facts
    ;

    private final int deltaMask;

    KeyMode(int deltaMask) {
        this.deltaMask = deltaMask;
    }

    public int getDeltaMask() {
        return deltaMask;
    }

    static {
        if (MAIN.ordinal() != 0) {
            throw new IllegalStateException("There is contract that the " + MAIN + " key storage always comes first");
        }
    }

    public boolean isDeltaMode() {
        return this != MAIN;
    }
}
