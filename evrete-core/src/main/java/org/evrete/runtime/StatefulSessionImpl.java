package org.evrete.runtime;

import org.evrete.api.ActivationManager;
import org.evrete.api.Named;
import org.evrete.api.RuntimeRule;
import org.evrete.api.StatefulSession;
import org.evrete.runtime.memory.SessionMemory;

import java.util.List;

public class StatefulSessionImpl extends SessionMemory implements StatefulSession {
    private final KnowledgeImpl knowledge;
    private boolean active = true;
    private ActivationManager activationManager;

    StatefulSessionImpl(KnowledgeImpl knowledge) {
        super(knowledge);
        this.knowledge = knowledge;
        this.activationManager = newActivationManager();
    }

    @Override
    public RuntimeRule getRule(String name) {
        return Named.find(getRules(), name);
    }

    @Override
    public void close() {
        if (active) {
            active = false;
            super.destroy();
            knowledge.close(this);
        }
    }

    @Override
    public StatefulSession addImport(String imp) {
        super.addImport(imp);
        return this;
    }

    @Override
    public StatefulSession addImport(Class<?> type) {
        super.addImport(type);
        return this;
    }

    @Override
    public ActivationManager getActivationManager() {
        return activationManager;
    }

    @Override
    public void setActivationManager(ActivationManager activationManager) {
        this.activationManager = activationManager;
    }

    @Override
    public <A extends ActivationManager> void setActivationManagerFactory(Class<A> managerClass) {
        super.setActivationManagerFactory(managerClass);
        this.activationManager = newActivationManager();
    }


    @Override
    public void fire() {
        if (hasMemoryTasks()) {
            fireDefault(new ActivationContext(this));
        }
    }

    private void fireDefault(ActivationContext ctx) {
        while (active && hasMemoryTasks()) {
            // Prepare and process memory deltas
            processChanges();
            List<RuntimeRule> agenda = getAgenda();
            if (agenda.size() > 0) {
                activationManager.onAgenda(ctx.incrementFireCount(), agenda);
                for (RuntimeRule r : getAgenda()) {
                    RuntimeRuleImpl impl = (RuntimeRuleImpl) r;

                    if (activationManager.test(impl)) {
                        impl.executeRhs();
                        activationManager.onActivation(impl);
                    }
                    impl.resetState();
                }
            }
            commitMemoryDeltas();
        }
    }

}
