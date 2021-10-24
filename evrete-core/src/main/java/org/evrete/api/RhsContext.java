package org.evrete.api;

import java.util.Collection;

/**
 * <p>
 * Rule activation context that provides access to the rule's variables and
 * working memory methods.
 * </p>
 */
public interface RhsContext {
    /**
     * <p>
     * Inserts a new object into the working memory.
     * </p>
     *
     * @param obj the object
     * @return the context itself so the methods could be chained
     */
    RhsContext insert(Object obj);


    default RhsContext insert(Collection<?> objects) {
        for (Object o : objects) {
            insert(o);
        }
        return this;
    }

    default RhsContext insert(Object[] objects) {
        for (Object o : objects) {
            insert(o);
        }
        return this;
    }

    /**
     * <p>
     * This method lets the working memory know that one of its objects has changed.
     * Always call this method to get conditions re-evaluated, and
     * avoid calling it if the changes are not relevant to the conditions.
     * </p>
     *
     * @param obj the changed object
     * @return the context itself so the methods could be chained
     */
    RhsContext update(Object obj);

    /**
     * <p>
     * This method removes an instance from the working memory.
     * </p>
     *
     * @param obj the object to remove
     * @return the context itself so the methods could be chained
     */
    RhsContext delete(Object obj);


    /**
     * <p>
     * A convenience method that returns reference to the current rule and its
     * environment.
     * </p>
     *
     * @return current rule
     */
    RuntimeRule getRule();

    /**
     * <p>
     * Provides access to the runtime context, an equivalent to
     * {@code getRule().getRuntime()}.
     * </p>
     *
     * @return runtime context (session)
     */
    default RuleSession<?> getRuntime() {
        return getRule().getRuntime();
    }

    default RhsContext deleteFact(String factRef) {
        return delete(getObject(factRef));
    }

    default RhsContext updateFact(String factRef) {
        return update(getObject(factRef));
    }

    /**
     * <p>
     * Returns current fact by its name
     * </p>
     *
     * @param name the fact name
     * @return current instance
     */
    Object getObject(String name);

    /**
     * <p>
     * A typed version of the {@code getObject()} method.
     * </p>
     *
     * @param name fact name
     * @param <T>  cast type
     * @return current instance
     */
    @SuppressWarnings("unchecked")
    default <T> T get(String name) {
        return (T) getObject(name);
    }

}
