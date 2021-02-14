package org.evrete.api;

//TODO !!!! javadoc, cleanup
public interface SharedPlainFactStorage extends BufferedInsert, ReIterable<FactHandleVersioned> {

    void insert(FactHandleVersioned fact);

    void clear();

    int size();

    void insert(SharedPlainFactStorage other);
}
