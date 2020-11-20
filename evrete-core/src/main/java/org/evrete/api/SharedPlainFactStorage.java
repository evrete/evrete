package org.evrete.api;

public interface SharedPlainFactStorage extends BufferedInsert, ReIterable<RuntimeFact> {

    void insert(RuntimeFact fact);

    void delete(RuntimeFact fact);

    RuntimeFact find(Object o);

    void clear();

    int size();

    void insert(SharedPlainFactStorage other);
}
