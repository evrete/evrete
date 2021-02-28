package org.evrete.spi.minimal;

import org.evrete.api.*;
import org.evrete.runtime.ActiveField;
import org.evrete.runtime.FieldsKey;

import java.util.Collection;
import java.util.Objects;
import java.util.StringJoiner;

class SharedBetaData implements SharedBetaFactStorage {
    private final ActiveField[] fields;
    private final FieldsFactMap[] maps = new FieldsFactMap[KeyMode.values().length];

    SharedBetaData(FieldsKey typeFields) {
        this.fields = typeFields.getFields();
        for (KeyMode mode : KeyMode.values()) {
            this.maps[mode.ordinal()] = new FieldsFactMap(mode);
        }
    }

    @Override
    public ReIterator<ValueRow> iterator(KeyMode keyMode) {
        return maps[keyMode.ordinal()].keys();
    }

    @Override
    public ReIterator<FactHandleVersioned> iterator(KeyMode mode, ValueRow row) {
        return get(mode).values((ValueRowImpl) row);
    }

    @Override
    public void clear() {
        for (FieldsFactMap map : maps) {
            map.clear();
        }
    }

    private int hash(FieldToValueHandle key) {
        int hash = 0;
        for (ActiveField field : fields) {
            hash ^= Objects.hashCode(key.apply(field));
        }
        return hash;
    }

    @Override
    public void commitChanges() {
        FieldsFactMap main = get(KeyMode.MAIN);
        FieldsFactMap delta1 = get(KeyMode.UNKNOWN_UNKNOWN);
        FieldsFactMap delta2 = get(KeyMode.KNOWN_UNKNOWN);
        main.addAll(delta1);
        main.addAll(delta2);

        delta1.clear();
        delta2.clear();
    }


    @Override
    public void insert(FieldToValueHandle key, FactHandleVersioned value) {
        int hash = hash(key);
        ValueHandle[] data = new ValueHandle[fields.length];
        for (int i = 0; i < fields.length; i++) {
            ActiveField field = fields[i];
            data[i] = key.apply(field);
        }
        ValueRowImpl row = new ValueRowImpl(data, hash);

        Collection<FactHandleVersioned> found = get(KeyMode.MAIN).get(row);
        if (found != null) {
            // Existing key, saving as such
            get(KeyMode.KNOWN_UNKNOWN).add(row, value);
        } else {
            get(KeyMode.UNKNOWN_UNKNOWN).add(row, value);
        }


    }

    private FieldsFactMap get(KeyMode mode) {
        return maps[mode.ordinal()];
    }


    @Override
    public String toString() {
        StringJoiner s = new StringJoiner("\n");

        for (KeyMode mode : KeyMode.values()) {
            String m = mode + "\n\t" + get(mode);
            s.add(m);
        }
        return "\n" + s.toString();
    }
}
