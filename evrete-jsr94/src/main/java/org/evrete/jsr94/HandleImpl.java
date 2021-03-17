package org.evrete.jsr94;

import org.evrete.api.FactHandle;

import javax.rules.Handle;
import java.util.Collections;
import java.util.List;

class HandleImpl implements Handle {
    static final List<HandleImpl> EMPTY_LIST = Collections.emptyList();
    private static final long serialVersionUID = -3461342115008712020L;
    final FactHandle delegate;

    HandleImpl(FactHandle delegate) {
        assert delegate != null;
        this.delegate = delegate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HandleImpl handle = (HandleImpl) o;
        return delegate.equals(handle.delegate);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }
}
