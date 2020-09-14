package org.evrete.api;

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
     * Returns a fact reference by its name, specified in the LHS
     * </p>
     *
     * @param name the fact name
     * @return current inner fact representation with a reference to the inserted instance
     */
    RuntimeFact getFact(String name);

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
     * <code>getRule().getRuntime()</code>.
     * </p>
     *
     * @return runtime context (session)
     */
    default RuntimeContext<?> getRuntime() {
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
    default Object getObject(String name) {
        return getFact(name).getDelegate();
    }

    /**
     * <p>
     * A typed version of the <code>getObject()</code> method.
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
