package org.evrete.spi.minimal;

import org.evrete.api.ActiveField;
import org.evrete.api.FieldToValueHandle;
import org.evrete.api.KeyMode;

import java.util.Objects;

class FactsMapMulti extends AbstractFactsMap<MemoryKeyMulti> {
    private final ActiveField[] fields;

    FactsMapMulti(ActiveField[] fields, KeyMode myMode, int minCapacity) {
        super(myMode, minCapacity);
        this.fields = fields;
    }

    @Override
    MemoryKeyMulti newKeyInstance(FieldToValueHandle fieldValues, int hash) {
        return new MemoryKeyMulti(fields, fieldValues, hash);
    }

    @Override
    boolean sameData(MapKey<MemoryKeyMulti> mapEntry, FieldToValueHandle key) {
        for (int i = 0; i < fields.length; i++) {
            if (!Objects.equals(mapEntry.key.get(i), key.apply(fields[i]))) return false;
        }
        return true;
    }
}
