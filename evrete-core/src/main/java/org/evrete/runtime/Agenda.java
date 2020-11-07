package org.evrete.runtime;

import org.evrete.api.RuntimeRule;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Agenda {
    private final List<RuntimeRule> activeRules = new LinkedList<>();
    private final List<RuntimeRule> inactiveRules = new LinkedList<>();
    private final List<RuntimeRuleImpl> allRules;

    public Agenda(List<RuntimeRuleImpl> allRules) {
        this.allRules = allRules;
    }

    public List<RuntimeRule> activeRules() {
        return Collections.unmodifiableList(activeRules);
    }

    public List<RuntimeRule> inactiveRules() {
        return Collections.unmodifiableList(inactiveRules);
    }

    public Agenda update() {
        activeRules.clear();
        inactiveRules.clear();
        for (RuntimeRuleImpl r : allRules) {
            if (r.isInActiveState()) {
                activeRules.add(r);
            } else {
                inactiveRules.add(r);
            }
        }
        return this;
    }
}
