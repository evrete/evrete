package org.evrete.api;

/**
 * Enumeration representing the mode in which keys are retrieved from a memory.
 */
public enum KeyMode {

    OLD_OLD(false), // Known key, known facts
    NEW_NEW(true), // New key, new Facts
    OLD_NEW(true) // Known key, new facts
    ;

    static {
        //noinspection ConstantConditions
        if (OLD_OLD.ordinal() != 0) {
            throw new IllegalStateException("There is contract that the " + OLD_OLD + " key storage always comes first");
        }
    }

    private final boolean delta;

    KeyMode(boolean delta) {
        this.delta = delta;
    }

    public boolean isDelta() {
        return delta;
    }
}
