package org.evrete.api;

public enum KeyMode {

    MAIN, // Known key, known facts
    UNKNOWN_UNKNOWN, // New key, new Facts
    KNOWN_UNKNOWN // Known key, new facts
    ;

    static {
        if (MAIN.ordinal() != 0) {
            throw new IllegalStateException("There is contract that the " + MAIN + " key storage always comes first");
        }
    }

    public boolean isDeltaMode() {
        return this != MAIN;
    }
}
