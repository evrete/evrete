package org.evrete.spi.minimal;

import org.evrete.api.ActiveField;
import org.evrete.api.FieldToValueHandle;
import org.evrete.api.KeyMode;
import org.evrete.api.ValueHandle;

import java.util.Objects;

class FactsMapSingle extends AbstractFactsMap<MemoryKeySingle> {
    private final ActiveField field;

    FactsMapSingle(ActiveField field, KeyMode myMode, int minCapacity) {
        super(myMode, minCapacity);
        this.field = field;
    }

    @Override
    MemoryKeySingle newKeyInstance(FieldToValueHandle fieldValues, int hash) {
        return new MemoryKeySingle(fieldValues.apply(field), hash);
    }

    @Override
    boolean sameData(MapKey<MemoryKeySingle> mapEntry, FieldToValueHandle fieldToValueHandle) {
        ValueHandle h1 = mapEntry.key.data;
        ValueHandle h2 = fieldToValueHandle.apply(field);
        return Objects.equals(h1, h2);
    }
}
