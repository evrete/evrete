package org.evrete.runtime;

import org.evrete.api.Memory;
import org.evrete.api.ReIterator;
import org.evrete.api.RuntimeFact;

public interface PlainMemory extends Memory {
    ReIterator<RuntimeFact> mainIterator();

    ReIterator<RuntimeFact> deltaIterator();

    boolean hasChanges();

}
