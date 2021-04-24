package org.evrete.runtime;

import org.evrete.api.KeyMode;
import org.evrete.api.MemoryKey;
import org.evrete.api.ReIterator;

public class MemoryKeyIterator implements ReIterator<MemoryKey> {
    private final ReIterator<MemoryKey> delegate;
    private final KeyMode forceMode;

    public MemoryKeyIterator(ReIterator<MemoryKey> delegate, KeyMode forceMode) {
        this.delegate = delegate;
        this.forceMode = forceMode;
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
    public MemoryKey next() {
        MemoryKey key = delegate.next();
        key.setMetaValue(forceMode.ordinal());
        return key;
    }

    @Override
    public void remove() {
        delegate.remove();
    }

}
