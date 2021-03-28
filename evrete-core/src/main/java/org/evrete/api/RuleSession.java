package org.evrete.api;


import java.util.function.BiConsumer;

public interface RuleSession<S extends RuleSession<S>> extends WorkingMemory, RuntimeContext<S>, RuleSet<RuntimeRule>, AutoCloseable {

    ActivationManager getActivationManager();

    S setActivationManager(ActivationManager activationManager);

    void forEachFact(BiConsumer<FactHandle, Object> consumer);

    RuntimeContext<?> getParentContext();

    void fire();

    void close();

    void clear();

}
