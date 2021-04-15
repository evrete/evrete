package org.evrete.api;


import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.BiConsumer;

public interface RuleSession<S extends RuleSession<S>> extends WorkingMemory, RuntimeContext<S>, RuleSet<RuntimeRule>, AutoCloseable {

    ActivationManager getActivationManager();

    S setActivationManager(ActivationManager activationManager);

    void forEachFact(BiConsumer<FactHandle, Object> consumer);

    RuntimeContext<?> getParentContext();

    void fire();

    /**
     * <p>
     * Fires session asynchronously and returns a Future representing the session execution
     * status.
     * </p>
     *
     * @param result the result to return by the Future
     * @param <T>    result type parameter
     * @return a Future representing pending completion of the {@link #fire()} command
     * @throws RejectedExecutionException if the task cannot be
     *                                    scheduled for execution
     */
    <T> Future<T> fireAsync(T result);

    /**
     * <p>
     * Same as {@link #fireAsync(Object)}, with the Future's get method will returning
     * <code>null</code> upon successful completion.
     * </p>
     *
     * @return a Future representing pending completion of the {@link #fire()} command.
     * @throws RejectedExecutionException if the task cannot be
     *                                    scheduled for execution
     * @see #fireAsync(Object)
     */
    default Future<?> fireAsync() {
        return fireAsync(null);
    }

    void close();

    void clear();

}
