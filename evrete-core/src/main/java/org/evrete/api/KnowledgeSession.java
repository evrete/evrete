package org.evrete.api;


import java.util.function.BiConsumer;

public interface KnowledgeSession<S extends KnowledgeSession<S>> extends WorkingMemory, RuntimeContext<S> {

    ActivationManager getActivationManager();

    S setActivationManager(ActivationManager activationManager);

    void forEachFact(BiConsumer<FactHandle, Object> consumer);

    void fire();

    void close();

    void clear();

}
