package org.evrete.runtime;

import org.evrete.api.FactHandleVersioned;
import org.evrete.api.ReIterator;
import org.evrete.api.ValueRow;

public interface RhsFactGroup {
    ReIterator<ValueRow[]> keyIterator(boolean delta);

    ReIterator<FactHandleVersioned> factIterator(FactType type, ValueRow row);

    FactType[] types();
}
