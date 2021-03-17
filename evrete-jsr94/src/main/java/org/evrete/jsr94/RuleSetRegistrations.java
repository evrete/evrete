package org.evrete.jsr94;

import javax.rules.RuleExecutionSetNotFoundException;
import javax.rules.admin.RuleExecutionSetRegisterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class RuleSetRegistrations {
    private final Map<String, RuleExecutionSetImpl> registrations = new ConcurrentHashMap<>();

    void registerRuleExecutionSet(String s, RuleExecutionSetImpl ruleExecutionSet) throws RuleExecutionSetRegisterException {
        if (s == null) {
            throw new RuleExecutionSetRegisterException("Null registration URI is not allowed");
        }
        registrations.put(s, ruleExecutionSet);
    }

    void deregisterRuleExecutionSet(String s) {
        registrations.remove(s);
    }

    RuleExecutionSetImpl getChecked(String uri) throws RuleExecutionSetNotFoundException {
        RuleExecutionSetImpl set = registrations.get(uri);
        if (set == null) {
            throw new RuleExecutionSetNotFoundException("Ruleset '" + uri + "' not found");
        } else {
            return set;
        }
    }

    List<String> getKeys() {
        return new ArrayList<>(registrations.keySet());
    }
}
