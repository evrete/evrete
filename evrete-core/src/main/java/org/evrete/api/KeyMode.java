package org.evrete.api;

public enum KeyMode {

    MAIN, // Known key, known facts
    UNKNOWN_UNKNOWN, // New key, new Facts
    KNOWN_UNKNOWN // Known key, new facts
    ;

    public static final KeyMode[] DELTA_MODES = new KeyMode[]{UNKNOWN_UNKNOWN, KNOWN_UNKNOWN};

    static {
        if (MAIN.ordinal() != 0) {
            throw new IllegalStateException("There is contract that the " + MAIN + " key storage always comes first");
        }
    }

    public boolean isDeltaMode() {
        return this != MAIN;
    }
}
