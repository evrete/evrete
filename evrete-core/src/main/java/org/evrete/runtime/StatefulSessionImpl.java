package org.evrete.runtime;

import org.evrete.api.ActivationManager;
import org.evrete.api.StatefulSession;

import java.util.function.BooleanSupplier;

public class StatefulSessionImpl extends AbstractKnowledgeSession<StatefulSession> implements StatefulSession {

    StatefulSessionImpl(KnowledgeRuntime knowledge) {
        super(knowledge);
    }

    @Override
    public StatefulSession addImport(String imp) {
        return (StatefulSession) super.addImport(imp);
    }

    @Override
    public StatefulSession addImport(Class<?> type) {
        super.addImport(type);
        return this;
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


}
