package org.evrete.api;

public enum KeyMode {
    NEW_KEYS_NEW_FACTS,
    KNOWN_KEYS_KNOWN_FACTS,
    KNOWN_KEYS_NEW_FACTS;

    public boolean isDeltaMode() {
        return this != KNOWN_KEYS_KNOWN_FACTS;
    }
}
