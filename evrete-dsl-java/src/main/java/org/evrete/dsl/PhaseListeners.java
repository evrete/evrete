package org.evrete.dsl;

import org.evrete.Configuration;
import org.evrete.api.Environment;
import org.evrete.api.RuntimeContext;

import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;

class PhaseListeners implements SessionCloneable<PhaseListeners> {
    private final EnumMap<Phase, List<PhaseListenerMethod>> listeners = new EnumMap<>(Phase.class);

    PhaseListeners() {
        for (Phase phase : Phase.values()) {
            listeners.put(phase, new LinkedList<>());
        }
    }

    @Override
    public PhaseListeners copy(Object sessionInstance) {
        PhaseListeners newInstance = new PhaseListeners();
        for (Phase phase : Phase.values()) {
            for (PhaseListenerMethod lm : listeners.get(phase)) {
                newInstance.add(phase, lm.copy(sessionInstance));
            }
        }
        return newInstance;
    }

    private void add(Phase phase, PhaseListenerMethod m) {
        this.listeners.get(phase).add(m);
    }

    void add(PhaseListenerMethod m) {
        for (Phase phase : m.phases) {
            add(phase, m);
        }
    }


    void fire(Phase phase, RuntimeContext<?> ctx) {
        Collection<PhaseListenerMethod> c = this.listeners.get(phase);
        if (!c.isEmpty()) {
            Configuration configuration = ctx.getConfiguration();
            Environment environment = new MaskedEnvironment(ctx);
            for (PhaseListenerMethod m : c) {
                m.call(configuration, environment);
            }
        }
    }

}
