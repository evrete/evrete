package org.evrete.runtime;

import org.evrete.api.ReIterable;
import org.evrete.api.ReIterator;
import org.evrete.api.RuntimeFact;

public abstract class RhsFactGroupIterator implements NestedFactRunnable {
    private NestedFactRunnable delegate;

    @Override
    public void setDelegate(NestedFactRunnable delegate) {
        this.delegate = delegate;
    }

    public NestedFactRunnable getDelegate() {
        return delegate;
    }

    public abstract <I extends ReIterator<RuntimeFact>> void setIterators(I[] data);

    public abstract long reset();

    public abstract <I extends ReIterable<RuntimeFact>> void setIterables(I[] data);
}
