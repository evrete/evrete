package org.evrete.api;


import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

public interface RuleSession<S extends RuleSession<S>> extends WorkingMemory, RuntimeContext<S>, RuleSet<RuntimeRule>, AutoCloseable {

    ActivationManager getActivationManager();

    S setActivationManager(ActivationManager activationManager);

    void forEachFact(BiConsumer<FactHandle, Object> consumer);

    /**
     * <p>
     * A memory inspection method that accepts fact type as an argument.
     * </p>
     *
     * @param type     expected fact type
     * @param consumer consumer for the facts
     * @param <T>      expected java type for the provided type name
     * @throws ClassCastException       if a working memory object can not be cast to the specified type
     * @throws IllegalArgumentException if no such type exists
     * @see TypeResolver
     */
    default <T> void forEachFact(Class<T> type, Consumer<T> consumer) {
        forEachFact(type.getName(), consumer);
    }

    /**
     * <p>
     * A memory inspection method that accepts fact type as an argument. Type name can be either
     * a class name or a name of explicitly declared type. In the latter case, the generic type
     * parameter <code>T</code> must match the declared type's Java type (see {@link Type#getJavaType()})
     * </p>
     *
     * @param type     type name
     * @param consumer consumer for the facts
     * @param <T>      expected java type for the provided type name
     * @throws ClassCastException       if a working memory object can not be cast to the specified type
     * @throws IllegalArgumentException if no such type exists
     * @see TypeResolver
     */
    <T> void forEachFact(String type, Consumer<T> consumer);

    /**
     * <p>
     * A filtering version of the {@link #forEachFact(Class, Consumer)} method.
     * </p>
     *
     * @param type     expected fact type
     * @param consumer consumer for the facts
     * @param <T>      expected java type for the provided type name
     * @param filter   filtering predicate
     * @see #forEachFact(Class, Consumer)
     */
    default <T> void forEachFact(Class<T> type, Predicate<T> filter, Consumer<T> consumer) {
        forEachFact(type, t -> {
            if (filter.test(t)) {
                consumer.accept(t);
            }
        });
    }

    /**
     * <p>
     * A filtering version of the {@link #forEachFact(String, Consumer)} method.
     * </p>
     *
     * @param type     expected fact type
     * @param consumer consumer for the facts
     * @param <T>      expected java type for the provided type name
     * @param filter   filtering predicate
     * @see #forEachFact(String, Consumer)
     */
    @SuppressWarnings("unchecked")
    default <T> void forEachFact(String type, Predicate<T> filter, Consumer<T> consumer) {
        forEachFact(type, o -> {
            T t = (T) o;
            if (filter.test(t)) {
                consumer.accept(t);
            }
        });
    }

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
     * the session itself.
     * </p>
     *
     * @return a Future representing pending completion of the {@link #fire()} command.
     * @throws RejectedExecutionException if the task cannot be
     *                                    scheduled for execution
     * @see #fireAsync(Object)
     */
    @SuppressWarnings("unchecked")
    default Future<S> fireAsync() {
        return (Future<S>) fireAsync(this);
    }

    void close();

    void clear();

}
