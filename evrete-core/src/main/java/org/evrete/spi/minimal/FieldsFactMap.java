package org.evrete.spi.minimal;

import org.evrete.api.FactHandleVersioned;
import org.evrete.api.KeyMode;
import org.evrete.api.MemoryKey;
import org.evrete.api.ReIterator;
import org.evrete.collections.CollectionReIterator;
import org.evrete.collections.LinkedData;
import org.evrete.collections.MappedReIterator;

import java.util.HashMap;
import java.util.Map;

class FieldsFactMap {
    //TODO !!! replace implementation
    private final Map<ValueRowImpl, DataWrapper> data = new HashMap<>();
    private final int myModeOrdinal;

    FieldsFactMap(KeyMode myMode) {
        this.myModeOrdinal = myMode.ordinal();
    }

    public void clear() {
        data.clear();
    }

    void addAll(FieldsFactMap other) {
        for (Map.Entry<ValueRowImpl, DataWrapper> entry : other.data.entrySet()) {
            ValueRowImpl key = entry.getKey();
            key.setMetaValue(myModeOrdinal);

            this.data.computeIfAbsent(key, k -> new DataWrapper()).consume(entry.getValue());

/*
            Collection<FactHandleVersioned> col = entry.getValue();
            for (FactHandleVersioned v : col) {
                this.data.add(key, v);
            }
*/
        }
    }

    ReIterator<MemoryKey> keys() {
        ReIterator<ValueRowImpl> it = new CollectionReIterator<>(data.keySet());
        return new MappedReIterator<>(it, row -> row);
    }

    ReIterator<FactHandleVersioned> values(ValueRowImpl row) {
        // TODO !!!! analyze usage, return null and call remove() on the corresponding key iterator
        DataWrapper col = get(row);
        return col == null ? ReIterator.emptyIterator() : col.iterator();
    }

    public void add(ValueRowImpl valueRow, FactHandleVersioned factHandleVersioned) {
        this.data.computeIfAbsent(valueRow, k -> new DataWrapper()).add(factHandleVersioned);
    }

    DataWrapper get(ValueRowImpl key) {

        DataWrapper col = data.get(key);
        if (col == null) {
            return null;
        } else if (col.size() == 0) {
            data.remove(key);
            return null;
        } else {
            return col;
        }
    }

    @Override
    public String toString() {
        return data.toString();
    }

    static class DataWrapper extends LinkedData<FactHandleVersioned> {

    }

}
