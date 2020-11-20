package org.evrete.api;

public interface ValueRow extends ReIterable<RuntimeFact>, ReIterator<RuntimeFact> {

    boolean isDeleted();

    Object get(int i);

}
