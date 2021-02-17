package org.evrete.api;

public interface ValueRow extends ReIterable<FactHandleVersioned>, ReIterator<FactHandleVersioned> {

    boolean isDeleted();

    ValueHandle get1(int fieldIndex);

}
