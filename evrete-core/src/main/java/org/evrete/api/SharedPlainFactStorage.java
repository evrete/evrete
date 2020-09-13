package org.evrete.api;

import org.evrete.api.BufferedInsert;
import org.evrete.api.ReIterable;
import org.evrete.api.RuntimeFact;


public interface SharedPlainFactStorage extends BufferedInsert, ReIterable<RuntimeFact> {

    void insert(RuntimeFact fact);

    void delete(RuntimeFact fact);

    void clear();

    int size();
}
