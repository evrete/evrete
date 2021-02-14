package org.evrete.spi.minimal;

import org.evrete.api.FactHandleVersioned;
import org.evrete.api.ReIterator;
import org.evrete.api.ValueRow;
import org.evrete.collections.LinearHashSet;

import java.util.Arrays;

class ValueRowImpl implements ValueRow {
    final Object[] data;
    private final LinearHashSet<FactHandleVersioned> facts = new LinearHashSet<>();
    private final int hash;
    private final ReIterator<FactHandleVersioned> delegate;
    private int factCount = 0;
    private boolean deleted;

    ValueRowImpl(Object[] data, int hash, FactHandleVersioned fact) {
        this(data, hash);
        this.addFact(fact);
    }

    private ValueRowImpl(Object[] data, int hash) {
        this.data = data;
        this.hash = hash;
        this.delegate = facts.iterator();
    }

    void mergeDataFrom(ValueRowImpl other) {
        this.facts.bulkAdd(other.facts);
    }

    void addFact(FactHandleVersioned fact) {
        this.facts.addSilent(fact);
        this.factCount++;
    }

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    @Override
    public long reset() {
        return delegate.reset();
    }

    @Override
    public boolean hasNext() {
        return delegate.hasNext();
    }

    @Override
    public FactHandleVersioned next() {
        return delegate.next();
    }

    @Override
    public void remove() {
        delegate.remove();
    }

    @Override
    public ReIterator<FactHandleVersioned> iterator() {
        return facts.iterator();
    }

    @Override
    public String toString() {
        if (deleted) {
            return Arrays.toString(data) + " -X-> " + facts;
        } else {
            return Arrays.toString(data) + " ---> " + facts;
        }
    }

    @Override
    public Object get(int i) {
        return data[i];
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValueRowImpl other = (ValueRowImpl) o;
        return MiscUtils.sameData(other.data, data);
    }

    @Override
    public final int hashCode() {
        return hash;
    }
}
