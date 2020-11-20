package org.evrete.runtime.memory;

import org.evrete.api.ReIterator;
import org.evrete.api.RuntimeFact;
import org.evrete.collections.LinearIdentityHashMap;
import org.evrete.runtime.RuntimeFactImpl;

import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.ToIntFunction;

class IdentityMap extends LinearIdentityHashMap<Object, RuntimeFactImpl> {
    private static final ToIntFunction<Object> HASH = System::identityHashCode;
    private static final Function<Entry<Object, RuntimeFactImpl>, RuntimeFact> MAPPER = Entry::getValue;

    private static final BiPredicate<Object, Object> EQ = (fact1, fact2) -> fact1 == fact2;

    ReIterator<RuntimeFact> factIterator() {
        return iterator(MAPPER);
    }

    @Override
    protected ToIntFunction<Object> keyHashFunction() {
        return HASH;
    }

    @Override
    protected BiPredicate<Object, Object> keyHashEquals() {
        return EQ;
    }

    boolean contains(Object o) {
        return get(o) != null;
    }
}
