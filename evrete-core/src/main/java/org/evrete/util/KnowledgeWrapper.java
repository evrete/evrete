package org.evrete.util;

import org.evrete.api.Knowledge;
import org.evrete.api.RuleSession;
import org.evrete.api.StatefulSession;
import org.evrete.api.StatelessSession;
import org.evrete.runtime.RuleDescriptor;

import java.util.Collection;

public abstract class KnowledgeWrapper extends RuntimeContextWrapper<Knowledge, Knowledge, RuleDescriptor> implements Knowledge {

    protected KnowledgeWrapper(Knowledge delegate) {
        super(delegate);
    }

    @Override
    public Collection<RuleSession<?>> getSessions() {
        return delegate.getSessions();
    }

    @Override
    public StatefulSession newStatefulSession() {
        return delegate.newStatefulSession();
    }

    @Override
    public StatelessSession newStatelessSession() {
        return delegate.newStatelessSession();
    }

}
