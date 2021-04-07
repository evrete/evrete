package org.evrete.runtime;

import org.evrete.api.Action;

@FunctionalInterface
public interface MemoryActionListener {
    void apply(Action action, boolean addOrRemove);
}
