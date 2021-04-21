package org.evrete.util;

import org.evrete.api.Knowledge;
import org.evrete.api.RuleBuilder;
import org.evrete.api.RuleSession;
import org.evrete.api.StatefulSession;
import org.evrete.runtime.RuleDescriptor;

import java.util.Collection;
import java.util.List;

public class KnowledgeWrapper extends RuntimeContextWrapper<Knowledge> implements Knowledge {

    protected KnowledgeWrapper(Knowledge delegate) {
        super(delegate);
    }

    @Override
    public Collection<RuleSession<?>> getSessions() {
        return delegate.getSessions();
    }

    @Override
    public StatefulSession createSession() {
        return delegate.createSession();
    }

    @Override
    public List<RuleDescriptor> getRules() {
        return delegate.getRules();
    }

    @Override
    public RuleDescriptor compileRule(RuleBuilder<?> builder) {
        return delegate.compileRule(builder);
    }

    @Override
    public RuleDescriptor getRule(String name) {
        return delegate.getRule(name);
    }
}
