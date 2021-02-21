package org.evrete.api;

public enum KeyMode {
    UNKNOWN_UNKNOWN, // New key, new Facts
    KNOWN_KNOWN, // Known key, known facts
    KNOWN_UNKNOWN // Known key, new facts
    ;

    public boolean isDeltaMode() {
        return this != KNOWN_KNOWN;
    }
}
