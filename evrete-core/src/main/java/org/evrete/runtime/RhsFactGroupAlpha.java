package org.evrete.runtime;

import org.evrete.api.KeyMode;
import org.evrete.api.MemoryKey;
import org.evrete.api.ReIterator;
import org.evrete.api.ValueHandle;
import org.evrete.collections.CollectionReIterator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;

//TODO !!!! use RuntimeAware as parent class
public class RhsFactGroupAlpha implements RhsFactGroup {
    private static final MemoryKey KEY_MAIN = new VR(KeyMode.MAIN.ordinal());
    private static final MemoryKey KEY_DELTA = new VR(KeyMode.KNOWN_UNKNOWN.ordinal());
    private static final MemoryKey KEY_DELTA1 = new VR(KeyMode.UNKNOWN_UNKNOWN.ordinal());
    private final RuntimeFactType[] types;
    private final ReIterator<MemoryKey> deltaKeyIterator;
    private final ReIterator<MemoryKey> mainKeyIterator;
    private final EnumMap<KeyMode, ReIterator<MemoryKey>> keyIterators = new EnumMap<>(KeyMode.class);


    RhsFactGroupAlpha(RuntimeRuleImpl rule, RhsFactGroupDescriptor descriptor) {
        this.types = rule.asRuntimeTypes(descriptor.getTypes());
        assert types.length > 0;
        if (types.length > 24)
            throw new UnsupportedOperationException("Too many alpha nodes, another implementation required");

        // Main dummy iterator
        Collection<MemoryKey> mainCollection = new ArrayList<>();
        for (int i = 0; i < types.length; i++) {
            mainCollection.add(KEY_MAIN);
        }
        this.mainKeyIterator = new CollectionReIterator<>(mainCollection);


        // Delta dummy iterator
        Collection<MemoryKey> deltaCollection = new ArrayList<>();
        for (int i = 1; i < (1 << types.length); i++) {
            for (int bit = 0; bit < types.length; bit++) {
                if ((i & (1 << bit)) == 0) {
                    deltaCollection.add(KEY_MAIN);
                } else {
                    deltaCollection.add(KEY_DELTA);
                }
            }
        }
        this.deltaKeyIterator = new CollectionReIterator<>(deltaCollection);

        this.keyIterators.put(KeyMode.MAIN, mainKeyIterator);
        this.keyIterators.put(KeyMode.KNOWN_UNKNOWN, deltaKeyIterator);
        this.keyIterators.put(KeyMode.UNKNOWN_UNKNOWN, ReIterator.emptyIterator());
    }

    @Override
    public RuntimeFactType[] types() {
        return types;
    }

    @Override
    public ReIterator<MemoryKey> keyIterator(boolean delta) {
        return delta ? keyIterators.get(KeyMode.KNOWN_UNKNOWN) : keyIterators.get(KeyMode.MAIN);
    }

    @Override
    public ReIterator<MemoryKey> keyIterator(KeyMode mode) {
        throw new UnsupportedOperationException("NOT IMPLEMENTED IN " + getClass().getName());
    }

    private static class VR implements MemoryKey {
        private final int transientValue;

        VR(int transientValue) {
            this.transientValue = transientValue;
        }

        @Override
        public ValueHandle get(int fieldIndex) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getMetaValue() {
            return transientValue;
        }

        @Override
        public void setMetaValue(int i) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return transientValue == 0 ? "MAIN" : "DELTA";
        }
    }
}
