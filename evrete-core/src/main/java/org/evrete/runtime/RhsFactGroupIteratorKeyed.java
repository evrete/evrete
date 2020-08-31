/*
package org.evrete.runtime;

import org.evrete.api.ReIterable;
import org.evrete.api.ReIterator;
import org.evrete.api.RuntimeFact;
import org.evrete.collections.NestedReIterator;

public class RhsFactGroupIteratorKeyed extends RhsFactGroupIterator {
    private final NestedReIterator<RuntimeFact> nestedReIterator;
    private Runnable nestedRunnable;

    public RhsFactGroupIteratorKeyed(int factGroupId, RuntimeFact[][] factState) {
        this.nestedReIterator = new NestedReIterator<>(factState[factGroupId]);
    }

    public <I extends ReIterator<RuntimeFact>> void setIterators(I[] data) {
        nestedReIterator.setIterators(data);
    }

    public long reset() {
        return nestedReIterator.reset();
    }

    @Override
    public void forEachFact() {
        nestedReIterator.runForEach(nestedRunnable);
    }

    @Override
    public void setDelegate(NestedFactRunnable delegate) {
        super.setDelegate(delegate);
        this.nestedRunnable = delegate::forEachFact;
    }

    public <I extends ReIterable<RuntimeFact>> void setIterables(I[] data) {
        nestedReIterator.setIterables(data);
    }

}
*/
