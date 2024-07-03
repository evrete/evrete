package org.evrete.runtime;

public abstract class PreHashed {
    private final int hash;

    protected PreHashed(int hash) {
        this.hash = hash;
    }

    @Override
    public final int hashCode() {
        return hash;
    }
}
