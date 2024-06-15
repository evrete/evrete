package org.evrete.runtime;

public abstract class PreHashed {
    private final int hash;

    protected PreHashed(int hash) {
        this.hash = hash;
    }

    protected PreHashed(PreHashed other) {
        this.hash = other.hash;
    }

    @Override
    public final int hashCode() {
        return hash;
    }

    protected boolean hashEquals(PreHashed other) {
        return hash == other.hash;
    }
}
