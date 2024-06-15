package org.evrete.dsl;

import org.evrete.api.FactHandle;
import org.evrete.api.StatefulSession;

import java.util.List;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

class DSLStatefulSession extends AbstractDSLSession<StatefulSession> implements StatefulSession {

    DSLStatefulSession(StatefulSession delegate, RulesetMeta meta, FieldDeclarations fieldDeclarations, List<DSLRule> rules, Object classInstance) {
        super(delegate, meta, fieldDeclarations, rules, classInstance);
    }

    @Override
    public StatefulSession fire() {
        delegate.fire();
        return self();
    }

    @Override
    public void close() {
        delegate.close();
    }


    @Override
    public void clear() {
        delegate.clear();
    }

}
