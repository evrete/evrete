package org.evrete.dsl;

import org.evrete.api.StatelessSession;

import java.util.List;

class DSLStatelessSession extends AbstractDSLSession<StatelessSession> implements StatelessSession {

    DSLStatelessSession(StatelessSession delegate, RulesetMeta meta, FieldDeclarations fieldDeclarations, List<DSLRule> rules, Object classInstance) {
        super(delegate, meta, fieldDeclarations, rules, classInstance);
    }

    @Override
    public Void fire() {
        return delegate.fire();
    }

}
