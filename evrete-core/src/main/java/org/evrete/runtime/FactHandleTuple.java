package org.evrete.runtime;

import org.evrete.api.FactHandle;
import org.evrete.api.Type;

class FactHandleTuple {
    final Object value;
    final Type<?> type;
    final FactHandle handle;

    FactHandleTuple(Type<?> type, FactHandle handle, Object value) {
        this.value = value;
        this.type = type;
        this.handle = handle;
    }
}
