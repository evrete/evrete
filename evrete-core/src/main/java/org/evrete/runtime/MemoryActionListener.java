package org.evrete.runtime;

import org.evrete.api.Action;

interface MemoryActionListener {
    void onBufferAction(int type, Action action, int delta);


}
