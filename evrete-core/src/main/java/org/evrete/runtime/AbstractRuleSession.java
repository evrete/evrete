package org.evrete.runtime;

import org.evrete.Configuration;
import org.evrete.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * <p>
 *     Base session class with common methods
 * </p>
 * @param <S> session type parameter
 */
abstract class AbstractRuleSession<S extends RuleSession<S>> extends AbstractRuntime<RuntimeRule, S> implements RuleSession<S> {
    private BooleanSupplier fireCriteria = () -> true;
    final boolean warnUnknownTypes;
    ActivationManager activationManager;
    final List<SessionLifecycleListener> lifecycleListeners = new ArrayList<>();
    boolean active = true;
    private final KnowledgeRuntime knowledge;

    AbstractRuleSession(KnowledgeRuntime knowledge) {
        super(knowledge);
        this.knowledge = knowledge;
        this.warnUnknownTypes = knowledge.getConfiguration().getAsBoolean(Configuration.WARN_UNKNOWN_TYPES);
        this.activationManager = newActivationManager();
    }

    protected abstract S thisInstance();

    boolean fireCriteriaMet() {
        return this.fireCriteria.getAsBoolean();
    }

    private void applyFireCriteria(BooleanSupplier fireCriteria) {
        this.fireCriteria = fireCriteria;
    }

    @Override
    public S setActivationManager(ActivationManager activationManager) {
        this.activationManager = activationManager;
        return thisInstance();
    }

    @Override
    public S setExecutionPredicate(BooleanSupplier criteria) {
        applyFireCriteria(criteria);
        return thisInstance();
    }

    @Override
    public final ActivationManager getActivationManager() {
        return activationManager;
    }

    @Override
    public final S addEventListener(SessionLifecycleListener listener) {
        this.lifecycleListeners.add(listener);
        return thisInstance();
    }

    @Override
    public final S removeEventListener(SessionLifecycleListener listener) {
        this.lifecycleListeners.remove(listener);
        return thisInstance();
    }

    @Override
    final void _assertActive() {
        if (!active) {
            throw new IllegalStateException("Session has been closed");
        }
    }

    @Override
    public KnowledgeRuntime getParentContext() {
        return knowledge;
    }

}
