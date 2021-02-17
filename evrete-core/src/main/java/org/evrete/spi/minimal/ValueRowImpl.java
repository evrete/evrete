package org.evrete.spi.minimal;

import org.evrete.api.FactHandleVersioned;
import org.evrete.api.ReIterator;
import org.evrete.api.ValueHandle;
import org.evrete.api.ValueRow;
import org.evrete.collections.CollectionReIterator;

import java.util.Arrays;
import java.util.LinkedList;

class ValueRowImpl implements ValueRow {
    final ValueHandle[] data;
    private final LinkedList<FactHandleVersioned> facts = new LinkedList<>();
    private final int hash;
    private final ReIterator<FactHandleVersioned> delegate;
    private boolean deleted;

    ValueRowImpl(ValueHandle[] data, int hash, FactHandleVersioned fact) {
        this(data, hash);
        this.addFact(fact);
    }

    private ValueRowImpl(ValueHandle[] data, int hash) {
        this.data = data;
        this.hash = hash;
        this.delegate = new CollectionReIterator<>(facts);
    }

    void mergeDataFrom(ValueRowImpl other) {
        this.facts.addAll(other.facts);
    }

    void addFact(FactHandleVersioned fact) {
        this.facts.add(fact);
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
        return new CollectionReIterator<>(facts);
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
    public ValueHandle get1(int i) {
        return data[i];
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValueRowImpl other = (ValueRowImpl) o;
        return MiscUtils.sameData1(other.data, data);
    }

    @Override
    public final int hashCode() {
        return hash;
    }
}
