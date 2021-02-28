package org.evrete.spi.minimal;

import org.evrete.api.FactHandleVersioned;
import org.evrete.api.KeyMode;
import org.evrete.api.ReIterator;
import org.evrete.api.ValueRow;
import org.evrete.collections.CollectionReIterator;
import org.evrete.collections.MappedReIterator;
import org.evrete.util.MapOfList;

import java.util.Collection;
import java.util.List;
import java.util.Map;

class FieldsFactMap {
    private final MapOfList<ValueRowImpl, FactHandleVersioned> data = new MapOfList<>();
    private final int myModeOrdinal;

    FieldsFactMap(KeyMode myMode) {
        this.myModeOrdinal = myMode.ordinal();
    }

    public void clear() {
        data.clear();
    }

    void addAll(FieldsFactMap other) {
        for (Map.Entry<ValueRowImpl, List<FactHandleVersioned>> entry : other.data.entrySet()) {
            ValueRowImpl key = entry.getKey();
            key.setTransient(myModeOrdinal);
            Collection<FactHandleVersioned> col = entry.getValue();
            for (FactHandleVersioned v : col) {
                this.data.add(key, v);
            }
        }
    }

    ReIterator<ValueRow> keys() {
        ReIterator<ValueRowImpl> it = new CollectionReIterator<>(data.keySet());
        return new MappedReIterator<>(it, row -> row);
    }

    ReIterator<FactHandleVersioned> values(ValueRowImpl row) {
        Collection<FactHandleVersioned> col = get(row);
        return col == null ? ReIterator.emptyIterator() : new CollectionReIterator<>(col);
    }

    public void add(ValueRowImpl valueRow, FactHandleVersioned factHandleVersioned) {
        data.add(valueRow, factHandleVersioned);
    }

    Collection<FactHandleVersioned> get(ValueRowImpl key) {

        List<FactHandleVersioned> col = data.get(key);
        if (col == null) {
            return null;
        } else if (col.isEmpty()) {
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
}
