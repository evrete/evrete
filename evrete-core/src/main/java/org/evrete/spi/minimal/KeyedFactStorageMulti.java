package org.evrete.spi.minimal;

import org.evrete.api.FieldValue;

class KeyedFactStorageMulti extends AbstractKeyedFactStorage<MemoryKeyMulti, FactsMapMulti> {
    private final MultiState multiState;

    KeyedFactStorageMulti(int fieldCount) {
        super(FactsMapMulti.class, mode -> new FactsMapMulti(fieldCount));
        this.multiState = new MultiState(fieldCount);
    }

    @Override
    MemoryKeyHashed writeKey(FieldValue h) {
        return this.multiState.update(h);
    }

    static class MultiState extends MemoryKeyHashed {
        private final FieldValue[] data;
        private int currentPosition = 0;

        MultiState(int size) {
            this.data = new FieldValue[size];
            super.values = i -> data[i];
        }

        MultiState update(FieldValue h) {
            if (currentPosition == data.length) {
                // Rotate the value position and reset the computed hash
                currentPosition = 0;
                super.hash = 0;
            }

            this.data[currentPosition++] = h;
            super.hash += 37 * h.hashCode();
            return this;
        }
    }

}
