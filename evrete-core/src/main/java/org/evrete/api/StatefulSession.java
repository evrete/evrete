package org.evrete.api;

import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * The StatefulSession interface represents a stateful rule session.
 */
public interface StatefulSession extends RuleSession<StatefulSession>,  AutoCloseable {

    /**
     * Fire all rules.
     */
    StatefulSession fire();


    /**
     * Fires the session asynchronously and returns a Future representing the session's execution status.
     *
     * @param result the result to return by the Future
     * @param <T> the result type parameter
     * @return a Future representing the pending completion of the {@link #fire()} command
     * @deprecated Use external tools like {@link java.util.concurrent.ExecutorService#submit(Runnable, Object)}.
     */
    @Deprecated
    default <T> Future<T> fireAsync(T result) {
        throw new UnsupportedOperationException("Deprecated");
    }

    /**
     * Same as {@link #fireAsync(Object)}, but the Future's get method will return
     * the session itself.
     *
     * @return a Future representing pending completion of the {@link #fire()} command.
     * @deprecated Use external tools like {@link java.util.concurrent.ExecutorService#submit(Runnable, Object)}.
     */
    @Deprecated
    default Future<StatefulSession> fireAsync() {
        throw new UnsupportedOperationException("Deprecated");
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
     * @return this instance
     */
    default StatefulSession forEachFact(BiConsumer<FactHandle, Object> consumer){
        streamFactEntries().forEach(entry -> consumer.accept(entry.getKey(), entry.getValue()));
        return this;
    }

    default StatefulSession forEachFact(BiPredicate<FactHandle, Object> filter, BiConsumer<FactHandle, Object> consumer) {
        return forEachFact((factHandle, o) -> {
            if (filter.test(factHandle, o)) {
                consumer.accept(factHandle, o);
            }
        });
    }

    default StatefulSession forEachFact(Consumer<Object> consumer) {
        return forEachFact((factHandle, o) -> consumer.accept(o));
    }

    /**
     * <p>
     * A memory inspection method that accepts fact type as an argument.
     * </p>
     *
     * @param type     expected fact type
     * @param consumer consumer for the facts
     * @param <T>      expected java type for the provided type name
     * @return this instance
     * @throws ClassCastException       if a working memory object can not be cast to the specified type
     * @throws IllegalArgumentException if no such type exists
     * @see TypeResolver
     */
    default <T> StatefulSession forEachFact(Class<T> type, Consumer<T> consumer) {
        streamFacts(type).forEach(consumer);
        return this;
    }

    /**
     * <p>
     * A memory inspection method that accepts fact type as an argument. Type name can be either
     * a class name or a name of explicitly declared type. In the latter case, the generic type
     * parameter {@code T} must match the declared type's Java type (see {@link Type#getJavaClass()} )
     * </p>
     *
     * @param type     type name
     * @param consumer consumer for the facts
     * @param <T>      expected java type for the provided type name
     * @return this instance
     * @throws ClassCastException       if a working memory object can not be cast to the specified type
     * @throws IllegalArgumentException if no such type exists
     * @see TypeResolver
     */
    default <T> StatefulSession forEachFact(String type, Consumer<T> consumer) {
        this.<T>streamFacts(type).forEach(consumer);
        return this;
    }

    /**
     * <p>
     * A filtering version of the {@link #forEachFact(Class, Consumer)} method.
     * </p>
     *
     * @param type     expected fact type
     * @param consumer consumer for the facts
     * @param <T>      expected java type for the provided type name
     * @param filter   filtering predicate
     * @return this instance
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
     * @return new stateful session instance
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

    default StatefulSession insertAndFire(Iterable<?> objects) {
        insert0(objects, true);
        return fire();
    }

    default StatefulSession insertAndFire(Object... objects) {
        insert0(objects, true);
        return fire();
    }

}
