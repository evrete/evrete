package org.evrete.spi.minimal;

import org.evrete.api.ReIterator;
import org.evrete.api.RuntimeFact;
import org.evrete.api.ValueRow;
import org.evrete.collections.FastIdentityHashSet;

import java.util.Arrays;

class ValueRowImpl implements ValueRow {
    private final FastIdentityHashSet<RuntimeFact> facts = new FastIdentityHashSet<>();
    final Object[] data;
    private final int hash;
    private final ReIterator<RuntimeFact> delegate;

    ValueRowImpl(Object[] data, int hash, RuntimeFact fact) {
        this.data = data;
        this.hash = hash;
        this.delegate = facts.iterator();
        this.facts.add(fact);
    }

    void mergeDataFrom(ValueRowImpl other) {
        this.facts.bulkAdd(other.facts);
    }

    void addFact(RuntimeFact fact) {
        this.facts.add(fact);
    }

    long removeFact(RuntimeFact fact) {
        this.facts.remove(fact);
        return this.facts.size();
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
    public ReIterator<RuntimeFact> iterator() {
        return facts.iterator();
    }

    @Override
    public String toString() {
        return Arrays.toString(data);
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
