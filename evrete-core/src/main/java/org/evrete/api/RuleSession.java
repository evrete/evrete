package org.evrete.api;


public interface RuleSession<S extends RuleSession<S>> extends WorkingMemory, RuntimeContext<S>, RuleSet<RuntimeRule> {

    ActivationManager getActivationManager();

    S setActivationManager(ActivationManager activationManager);

    RuntimeContext<?> getParentContext();

}
