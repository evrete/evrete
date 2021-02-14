package org.evrete.api;


public interface KnowledgeSession<S extends KnowledgeSession<S>> extends WorkingMemory, RuntimeContext<S> {

    ActivationManager getActivationManager();

    S setActivationManager(ActivationManager activationManager);

    void fire();


}
