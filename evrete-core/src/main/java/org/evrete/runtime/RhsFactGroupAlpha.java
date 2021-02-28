package org.evrete.runtime;

import org.evrete.api.*;
import org.evrete.collections.CollectionReIterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

//TODO optimize by preconfiguring main and delta iterators
//TODO !!!! use RuntimeAware as parent class
public class RhsFactGroupAlpha implements RhsFactGroup {
    private static final VR KEY_MAIN = new VR(KeyMode.MAIN.ordinal());
    private static final VR KEY_DELTA = new VR(KeyMode.KNOWN_UNKNOWN.ordinal());
    private final FactType[] types;
    private final ReIterator<ValueRow[]> deltaKeyIterator;
    private final ReIterator<ValueRow[]> mainKeyIterator;
    private final SessionMemory memory;

    RhsFactGroupAlpha(SessionMemory memory, RhsFactGroupDescriptor descriptor) {
        this.types = descriptor.getTypes();
        assert types.length > 0;
        this.memory = memory;

        ValueRow[] dummyMain = new VR[this.types.length];
        Arrays.fill(dummyMain, KEY_MAIN);
        this.mainKeyIterator = new CollectionReIterator<>(Collections.singletonList(dummyMain));


        Collection<VR[]> deltaCollection = new ArrayList<>();
        int cnt = types.length;
        //TODO !!! fix it
        if (cnt > 24) throw new UnsupportedOperationException("Too many alpha nodes, another implementation required");
        for (int i = 1; i < (1 << cnt); i++) {
            VR[] arr = new VR[cnt];
            for (int bit = 0; bit < cnt; bit++) {
                if ((i & (1 << bit)) == 0) {
                    arr[bit] = KEY_MAIN;
                } else {
                    arr[bit] = KEY_DELTA;
                }
            }
            deltaCollection.add(arr);
        }

        this.deltaKeyIterator = new CollectionReIterator<>(deltaCollection);

    }

    @Override
    public FactType[] types() {
        return types;
    }

    @Override
    public ReIterator<FactHandleVersioned> factIterator(FactType type, ValueRow row) {
        KeyMode mode = KeyMode.values()[row.getTransient()];
        return memory.get(type.getType()).get(type.getFields()).get(type.getAlphaMask()).iterator(mode, row);
    }

    @Override
    public ReIterator<ValueRow[]> keyIterator(boolean delta) {
        return delta ? deltaKeyIterator : mainKeyIterator;
    }

    private static class VR implements ValueRow {
        private final int transientValue;

        VR(int transientValue) {
            this.transientValue = transientValue;
        }

        @Override
        public ValueHandle get(int fieldIndex) {
            //TODO override or provide a message
            throw new UnsupportedOperationException();
        }

        @Override
        public int getTransient() {
            return transientValue;
        }

        @Override
        public void setTransient(int transientValue) {
            //TODO override or provide a message
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return transientValue == 0 ? "MAIN" : "DELTA";
        }
    }
}
