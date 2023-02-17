package org.evrete.dsl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

class EnvListeners implements SessionCloneable<EnvListeners> {
    private final Map<String, Collection<EnvListenerMethod>> methods = new HashMap<>();


    EnvListeners() {
    }

    private EnvListeners(EnvListeners parent, Object sessionInstance) {
        for (Map.Entry<String, Collection<EnvListenerMethod>> entry : parent.methods.entrySet()) {
            Collection<EnvListenerMethod> col = entry.getValue();
            ArrayList<EnvListenerMethod> copy = new ArrayList<>(col.size());
            for (EnvListenerMethod m : col) {
                copy.add(m.copy(sessionInstance));
            }
            this.methods.put(entry.getKey(), copy);
        }
    }

    void add(String property, EnvListenerMethod m) {
        methods.computeIfAbsent(property, p -> new ArrayList<>()).add(m);
    }


    void fire(String property, Object value, boolean staticOnly) {
        Collection<EnvListenerMethod> col = methods.get(property);
        if (col != null) {
            for (EnvListenerMethod m : col) {
                m.call(value, staticOnly);
            }
        }
    }

    @Override
    public EnvListeners copy(Object sessionInstance) {
        return new EnvListeners(this, sessionInstance);
    }
}
