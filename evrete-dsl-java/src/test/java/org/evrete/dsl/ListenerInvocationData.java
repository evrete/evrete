package org.evrete.dsl;

import java.util.EnumMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ListenerInvocationData {
    static final EnumMap<Phase, AtomicInteger> EVENTS = new EnumMap<>(Phase.class);

    static {
        reset();
    }

    public static void reset() {
        for (Phase phase : Phase.values()) {
            EVENTS.put(phase, new AtomicInteger());
        }
    }

    public static int count(Phase phase) {
        return EVENTS.get(phase).get();
    }

    public static void event(Phase... phases) {
        for (Phase phase : phases) {
            EVENTS.get(phase).incrementAndGet();
        }
    }

    public static int total() {
        int total = 0;
        for (AtomicInteger i : EVENTS.values()) {
            total += i.get();
        }
        return total;
    }
}
