package org.evrete.api;

public interface ValueRow extends ReIterable<RuntimeFact>, ReIterator<RuntimeFact> {

    Object get(int i);

}
