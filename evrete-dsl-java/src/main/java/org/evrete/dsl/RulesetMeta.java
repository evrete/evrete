package org.evrete.dsl;

import org.evrete.Configuration;
import org.evrete.api.Environment;
import org.evrete.api.RuntimeContext;

import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;

class RulesetMeta {
    private final EnumMap<Phase, List<ListenerMethod>> listeners = new EnumMap<>(Phase.class);

    RulesetMeta() {
        for (Phase phase : Phase.values()) {
            listeners.put(phase, new LinkedList<>());
        }
    }

    void add(Phase phase, ListenerMethod m) {
        this.listeners.get(phase).add(m);
    }

    void add(ListenerMethod m) {
        for (Phase phase : m.phases) {
            add(phase, m);
        }
    }


    void fire(Phase phase, RuntimeContext<?> ctx) {
        Collection<ListenerMethod> c = this.listeners.get(phase);
        if (!c.isEmpty()) {
            Configuration configuration = ctx.getConfiguration();
            Environment environment = new MaskedEnvironment(ctx);
            for (ListenerMethod m : c) {
                m.call(configuration, environment);
            }
        }
    }

}
