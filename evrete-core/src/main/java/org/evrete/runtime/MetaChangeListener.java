package org.evrete.runtime;

interface MetaChangeListener {
    void onNewActiveField(ActiveField newField);

    void onNewAlphaBucket(MemoryAddress address);

}
