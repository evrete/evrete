package org.evrete.api;

public interface SharedPlainFactStorage extends BufferedInsert, ReIterable<RuntimeFact> {

    void insert(RuntimeFact fact);

    void delete(RuntimeFact fact);

    void clear();

    int size();
}
