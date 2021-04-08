package org.evrete.runtime;

import org.evrete.util.Bits;

class LazyInsertState {
    final FactRecord record;
    private final Bits alphaTests;

    LazyInsertState(FactRecord record, Bits alphaTests) {
        this.record = record;
        this.alphaTests = alphaTests;
    }

    public Bits getAlphaTests() {
        return alphaTests;
    }


    @Override
    public String toString() {
        return record.toString();
    }
}
