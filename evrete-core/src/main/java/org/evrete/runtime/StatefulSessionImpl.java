package org.evrete.runtime;

import org.evrete.api.ActivationManager;
import org.evrete.api.RuntimeRule;
import org.evrete.api.StatefulSession;

import java.util.function.BooleanSupplier;

public class StatefulSessionImpl extends AbstractRuleSession<StatefulSession> implements StatefulSession {

    StatefulSessionImpl(KnowledgeRuntime knowledge) {
        super(knowledge);
    }

    @Override
    public StatefulSession setActivationManager(ActivationManager activationManager) {
        applyActivationManager(activationManager);
        return this;
    }

    @Override
    public StatefulSession setFireCriteria(BooleanSupplier fireCriteria) {
        applyFireCriteria(fireCriteria);
        return this;
    }

    @Override
    public RuntimeRule getRule(String name) {
        return getRuleStorage().get(name);
    }
}
