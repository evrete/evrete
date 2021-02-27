package org.evrete.api;


//TODO !!!! rename to FactKey
public interface ValueRow {

    ValueHandle get(int fieldIndex);

    int getTransient();

    void setTransient(int transientValue);

}
