package org.evrete.runtime;

import org.evrete.api.KeyMode;
import org.evrete.api.MemoryKey;
import org.evrete.api.ReIterator;
import org.evrete.api.ValueHandle;
import org.evrete.collections.CollectionReIterator;
import org.evrete.runtime.evaluation.MemoryAddress;
import org.evrete.util.Mask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;

//TODO use a RuntimeAware as parent class
public class RhsFactGroupAlpha implements RhsFactGroup {
    private static final MemoryKey KEY_MAIN = new VR(KeyMode.OLD_OLD.ordinal());
    private static final MemoryKey KEY_DELTA = new VR(KeyMode.OLD_NEW.ordinal());
    private final RuntimeFactType[] types;
    private final EnumMap<KeyMode, ReIterator<MemoryKey>> keyIterators = new EnumMap<>(KeyMode.class);
    private final Mask<MemoryAddress> memoryMask = Mask.addressMask();


    RhsFactGroupAlpha(RuntimeRuleImpl rule, RhsFactGroupDescriptor descriptor) {
        this.types = rule.asRuntimeTypes(descriptor.getTypes());
        assert types.length > 0;
        if (types.length > 24)
            throw new UnsupportedOperationException("Too many alpha nodes, another implementation required");

        // Main dummy iterator
        Collection<MemoryKey> mainCollection = new ArrayList<>();
        for (RuntimeFactType type : types) {
            mainCollection.add(KEY_MAIN);
            this.memoryMask.set(type.getMemoryAddress());
        }
        ReIterator<MemoryKey> mainKeyIterator = new CollectionReIterator<>(mainCollection);


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
        ReIterator<MemoryKey> deltaKeyIterator = new CollectionReIterator<>(deltaCollection);

        this.keyIterators.put(KeyMode.OLD_OLD, mainKeyIterator);
        this.keyIterators.put(KeyMode.OLD_NEW, deltaKeyIterator);
        this.keyIterators.put(KeyMode.NEW_NEW, ReIterator.emptyIterator());
    }

    @Override
    public RuntimeFactType[] types() {
        return types;
    }

    @Override
    public ReIterator<MemoryKey> keyIterator(KeyMode mode) {
        return keyIterators.get(mode);
    }

    @Override
    public Mask<MemoryAddress> getMemoryMask() {
        return memoryMask;
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
