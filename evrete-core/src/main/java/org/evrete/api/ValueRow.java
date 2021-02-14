package org.evrete.api;

public interface ValueRow extends ReIterable<FactHandleVersioned>, ReIterator<FactHandleVersioned> {

    boolean isDeleted();

    Object get(int fieldIndex);

}
