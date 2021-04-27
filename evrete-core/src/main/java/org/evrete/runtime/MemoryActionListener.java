package org.evrete.runtime;

import org.evrete.api.Action;

interface MemoryActionListener {
    void apply(int type, Action action, int delta);
}
