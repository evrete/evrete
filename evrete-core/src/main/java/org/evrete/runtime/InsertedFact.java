package org.evrete.runtime;

import org.evrete.api.FactHandle;

class InsertedFact {
    final FactHandle handle;
    final FactRecord record;

    InsertedFact(FactHandle handle, FactRecord record) {
        this.handle = handle;
        this.record = record;
    }
}
