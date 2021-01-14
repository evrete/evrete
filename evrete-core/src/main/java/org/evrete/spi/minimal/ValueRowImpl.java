package org.evrete.spi.minimal;

import org.evrete.api.ReIterator;
import org.evrete.api.RuntimeFact;
import org.evrete.api.ValueRow;
import org.evrete.collections.LinearIdentityHashSet;

import java.util.Arrays;

class ValueRowImpl implements ValueRow {
    final Object[] data;
    private final LinearIdentityHashSet<RuntimeFact> facts = new LinearIdentityHashSet<>();
    private final int hash;
    private final ReIterator<RuntimeFact> delegate;
    private int factCount = 0;
    private boolean deleted;

    ValueRowImpl(Object[] data, int hash, RuntimeFact fact) {
        this(data, hash);
        this.addFact(fact);
    }

    ValueRowImpl(Object[] data, int hash) {
        this.data = data;
        this.hash = hash;
        this.delegate = facts.iterator();
    }

    void mergeDataFrom(ValueRowImpl other) {
        this.facts.bulkAdd(other.facts);
    }

    void addFact(RuntimeFact fact) {
        this.facts.addSilent(fact);
        this.factCount++;
    }

    long removeFact(RuntimeFact fact) {
        assert fact.isDeleted();
        this.factCount--;
        return this.factCount;
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
    public RuntimeFact next() {
        return delegate.next();
    }

    @Override
    public void remove() {
        delegate.remove();
    }

    @Override
    public ReIterator<RuntimeFact> iterator() {
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
