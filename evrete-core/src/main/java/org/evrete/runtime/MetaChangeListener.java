package org.evrete.runtime;

import org.evrete.runtime.evaluation.MemoryAddress;

interface MetaChangeListener {
    void onNewActiveField(ActiveField newField);

    void onNewAlphaBucket(MemoryAddress address);

}
