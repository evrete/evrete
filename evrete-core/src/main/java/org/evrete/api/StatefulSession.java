package org.evrete.api;

import java.util.Collection;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

public interface StatefulSession extends RuleSession<StatefulSession>, AutoCloseable {

    /**
     * Fire all rules.
     */
    StatefulSession fire();

    /**
     * <p>
     * Updates a fact that already exists in the working memory
     * </p>
     *
     * @param handle   fact handle, previously assigned to original fact
     * @param newValue an updated version of the fact
     */
    StatefulSession update(FactHandle handle, Object newValue);

    /**
     * <p>
     * Deletes a fact from working memory.
     * </p>
     *
     * @param handle fact handle
     */
    StatefulSession delete(FactHandle handle);

    /**
     * <p>
     * Returns fact by its handle.
     * </p>
     *
     * @param handle fact handle
     * @param <T>    type of the fact (use {@code Object}  or wildcard if type is unknown)
     * @return fact or {@code null} if fact is not found
     */
    <T> T getFact(FactHandle handle);

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
    default Future<StatefulSession> fireAsync() {
        return fireAsync(this);
    }

    /**
     * <p>
     * Closes the session and destroys its memory. A closed session can not be reused.
     * </p>
     */
    void close();

    /**
     * <p>
     * This method clears session's working memory and its beta-memory nodes as if the session
     * had never been used.
     * </p>
     */
    void clear();


    /**
     * <p>
     * A full-scan memory inspection method.
     * </p>
     *
     * @param consumer consumer for the facts
     */
    StatefulSession forEachFact(BiConsumer<FactHandle, Object> consumer);

    default StatefulSession forEachFact(BiPredicate<FactHandle, Object> filter, BiConsumer<FactHandle, Object> consumer) {
        return forEachFact((factHandle, o) -> {
            if (filter.test(factHandle, o)) {
                consumer.accept(factHandle, o);
            }
        });
    }

    default StatefulSession forEachFact(Consumer<Object> consumer) {
        return forEachFact((factHandle, o) -> {
            consumer.accept(o);
        });
    }

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
    default <T> StatefulSession forEachFact(Class<T> type, Consumer<T> consumer) {
        return forEachFact(type.getName(), consumer);
    }

    /**
     * <p>
     * A memory inspection method that accepts fact type as an argument. Type name can be either
     * a class name or a name of explicitly declared type. In the latter case, the generic type
     * parameter {@code T} must match the declared type's Java type (see {@link Type#getJavaType()})
     * </p>
     *
     * @param type     type name
     * @param consumer consumer for the facts
     * @param <T>      expected java type for the provided type name
     * @throws ClassCastException       if a working memory object can not be cast to the specified type
     * @throws IllegalArgumentException if no such type exists
     * @see TypeResolver
     */
    <T> StatefulSession forEachFact(String type, Consumer<T> consumer);

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
    default <T> StatefulSession forEachFact(Class<T> type, Predicate<T> filter, Consumer<T> consumer) {
        return forEachFact(type, t -> {
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
    default <T> StatefulSession forEachFact(String type, Predicate<T> filter, Consumer<T> consumer) {
        return forEachFact(type, o -> {
            T t = (T) o;
            if (filter.test(t)) {
                consumer.accept(t);
            }
        });
    }

    default StatefulSession insertAndFire(Collection<?> objects) {
        insert(objects);
        return fire();
    }

    default StatefulSession insertAndFire(Object... objects) {
        insert(objects);
        return fire();
    }

}
