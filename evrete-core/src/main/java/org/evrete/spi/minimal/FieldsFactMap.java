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

    /*
    private static final ToIntFunction<Object> HASH_FUNCTION = Object::hashCode;
    private static final BiPredicate<ValueRowImpl, ValueRowImpl> EQ_FUNCTION_TYPED = ValueRowImpl::equals;
    private static final Function<ValueRowImpl, ValueRow> IMPL_MAPPING = v -> v;

    ReIterator<ValueRow> keyIterator() {
        return iterator(IMPL_MAPPING);
    }

    @Override
    protected ToIntFunction<Object> getHashFunction() {
        return HASH_FUNCTION;
    }

    public void addAll(FieldsFactMap other) {
        resize(this.size() + other.size() + 1);
        other.forEachDataEntry(this::insertOtherNoResize);
    }

    @Override
    protected BiPredicate<Object, Object> getEqualsPredicate() {
        return IDENTITY_EQUALS;
    }

    private void insertOtherNoResize(ValueRowImpl other) {
        int hash = other.hashCode();
        int addr = findBinIndex(other, hash, EQ_FUNCTION_TYPED);
        ValueRowImpl found = get(addr);
        if (found == null) {
            saveDirect(other, addr);
        } else {
            //TODO !!!!!
            throw new UnsupportedOperationException();
            //found.mergeDataFrom(other);
        }
    }
*/

    @Override
    public String toString() {
        return data.toString();
    }
}
