package org.evrete.api;


public interface MemoryKey {

    ValueHandle get(int fieldIndex);

    int getMetaValue();

    void setMetaValue(int i);
}
