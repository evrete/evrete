package org.evrete.jsr94;

import org.evrete.api.Knowledge;
import org.evrete.runtime.RuleDescriptor;

import javax.rules.admin.Rule;
import javax.rules.admin.RuleExecutionSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class RuleExecutionSetImpl implements RuleExecutionSet {
    private static final long serialVersionUID = -3626185443249453796L;
    private final Knowledge knowledge;
    private final List<Rule> rules;
    private String defaultObjectFilter;

    RuleExecutionSetImpl(Knowledge knowledge, Map<?, ?> map) {
        this.knowledge = knowledge;
        Utils.copyConfiguration(knowledge, map);
        this.rules = new ArrayList<>(knowledge.getRules().size());
        for (RuleDescriptor rd : knowledge.getRules()) {
            this.rules.add(new RuleImpl(rd));
        }
    }

    @Override
    public String getName() {
        return knowledge.get(Const.RULE_SET_NAME, "");
    }

    Knowledge getKnowledge() {
        return knowledge;
    }

    @Override
    public String getDescription() {
        return knowledge.get(Const.RULE_SET_DESCRIPTION, "");
    }

    @Override
    public Object getProperty(Object o) {
        return Utils.getProperty(knowledge, o);
    }

    @Override
    public void setProperty(Object o, Object o1) {
        Utils.setProperty(knowledge, o, o1);
    }

    @Override
    public String getDefaultObjectFilter() {
        return defaultObjectFilter;
    }

    @Override
    public void setDefaultObjectFilter(String defaultObjectFilter) {
        this.defaultObjectFilter = defaultObjectFilter;
    }

    @Override
    public List<Rule> getRules() {
        return rules;
    }

    private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
        throw new UnsupportedOperationException("Serialization not supported");
    }
}
