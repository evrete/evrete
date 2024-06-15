package org.evrete.api;

/**
 * Provides access to the rule's RHS (Right-Hand Side) variables and
 * the session memory's methods.
 */
public interface RhsContext extends SessionOps {

    /**
     * Notifies the working memory that one of its objects has changed. Always call this method to
     * re-evaluate conditions, and avoid calling it if the changes are not relevant to the conditions.
     *
     * @param obj the changed object
     * @return the context itself so methods can be chained
     */
    RhsContext update(Object obj);

    /**
     * Removes an instance from the working memory.
     *
     * @param obj the object to remove
     * @return the context itself so methods can be chained
     */
    RhsContext delete(Object obj);

    /**
     * Returns a reference to the current rule and its environment.
     *
     * @return the current rule
     */
    RuntimeRule getRule();

    /**
     * Provides access to the runtime context, equivalent to
     * {@code getRule().getRuntime()}.
     *
     * @return the runtime context (session)
     */
    default RuleSession<?> getRuntime() {
        return getRule().getRuntime();
    }

    /**
     * Deletes a fact from the working memory by its reference name.
     *
     * @param factRef the reference name of the fact to delete
     * @return the context itself so methods can be chained
     */
    default RhsContext deleteFact(String factRef) {
        return delete(getObject(factRef));
    }

    /**
     * Updates a fact in the working memory by its reference name.
     *
     * @param factRef the reference name of the fact to update
     * @return the context itself so methods can be chained
     */
    default RhsContext updateFact(String factRef) {
        return update(getObject(factRef));
    }

    /**
     * Returns the current fact by its name.
     *
     * @param name the name of the fact
     * @return the current instance
     */
    Object getObject(String name);

    /**
     * A typed version of the {@code getObject()} method.
     *
     * @param name the name of the fact
     * @param <T> the type to cast the fact to
     * @return the typed instance
     */
    @SuppressWarnings("unchecked")
    default <T> T get(String name) {
        return (T) getObject(name);
    }

    /**
     * A typed version of the {@code get()} method with an explicit generic cast type.
     *
     * @param name the name of the fact
     * @param type the class type to cast the fact to
     * @param <T> the type to cast the fact to
     * @return the typed instance
     */
    @SuppressWarnings("unused")
    default <T> T get(Class<T> type, String name) {
        return get(name);
    }
}
