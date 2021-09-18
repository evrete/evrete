package org.evrete.api;

import java.util.EventListener;

public interface SessionLifecycleListener extends EventListener {

    void onEvent(Event evt);

    enum Event {
        PRE_FIRE, PRE_CLOSE
    }
}
