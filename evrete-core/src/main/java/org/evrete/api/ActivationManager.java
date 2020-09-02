package org.evrete.api;

import java.util.function.Predicate;

/**
 * <p>
 *     Activation manager controls whether an active rule should really fire based on the history
 *     of previous rule activations. Each session is provided with its own instance of activation
 *     manager.
 * </p>
 */
public interface ActivationManager extends Predicate<RuntimeRule> {

    /**
     * <p>
     *      When a session is fired, its activation (execution) may result in changes in the session's
     *      memory and subsequent activations of rules. This process continues until there are no changes
     *      left in the working memory.
     * </p>
     * <p>
     *     This method is called once for each memory change and before any rule activations.
     * </p>
     *
     * @param sequenceId memory task id starting at zero (initial session fire)
     */
    void reset(int sequenceId);

    /**
     * This method is called before rule activation, that is after a rule has become active AND passed
     * this manager's <code>test()</code> method
     * @param rule rule
     */
    void onActivation(RuntimeRule rule);

}
