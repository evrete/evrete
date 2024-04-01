package org.evrete.util;

import org.evrete.api.*;

import java.util.Collection;

/**
 * KnowledgeWrapper is an abstract class that delegates the knowledge's
 * methods to a provided delegate object.
 */
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
