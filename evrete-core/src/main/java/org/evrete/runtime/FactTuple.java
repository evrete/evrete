package org.evrete.runtime;

import org.evrete.api.FactHandle;

// TODO !!! rename, its instances are result of insert operations only. Optionally reuse (extend) this class further
//  during delta memory processing
class FactTuple {
    final FactHandle handle;
    final FactRecord record;

    FactTuple(FactHandle handle, FactRecord record) {
        this.handle = handle;
        this.record = record;
    }
}
