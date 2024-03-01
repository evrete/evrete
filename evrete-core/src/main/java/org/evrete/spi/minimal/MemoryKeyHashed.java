package org.evrete.spi.minimal;

class MemoryKeyHashed {
    int hash;
    IntToValueHandle values;

    @Override
    public final int hashCode() {
        return hash;
    }
}
