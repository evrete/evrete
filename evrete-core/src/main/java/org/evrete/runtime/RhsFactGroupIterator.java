package org.evrete.runtime;

import org.evrete.api.ReIterable;
import org.evrete.api.ReIterator;
import org.evrete.api.RuntimeFact;
import org.evrete.collections.NestedReIterator;

public class RhsFactGroupIterator implements Runnable {
    private final NestedReIterator<RuntimeFact> nestedReIterator;
    private Runnable delegate;

    public RhsFactGroupIterator(int factGroupId, RuntimeFact[][] factState) {
        this.nestedReIterator = new NestedReIterator<>(factState[factGroupId]);
    }

    public void setDelegate(Runnable delegate) {
        this.delegate = delegate;
    }


    public <I extends ReIterator<RuntimeFact>> void setIterators(I[] data) {
        nestedReIterator.setIterators(data);
    }

    public long reset() {
        return nestedReIterator.reset();
    }

    @Override
    public void run() {
        nestedReIterator.runForEach(delegate);
    }

    public <I extends ReIterable<RuntimeFact>> void setIterables(I[] data) {
        nestedReIterator.setIterables(data);
    }
}
