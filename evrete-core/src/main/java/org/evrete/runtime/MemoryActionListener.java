package org.evrete.runtime;

import org.evrete.api.Action;

@FunctionalInterface
interface MemoryActionListener {
    void apply(Action action, boolean addOrRemove);
}
