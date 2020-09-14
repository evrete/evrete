package org.evrete.api;

import java.util.List;
import java.util.function.Predicate;

/**
 * <p>
 * Activation manager controls whether an active rule should really fire based on the history
 * of previous activations. Each session is provided with its own instance of activation
 * manager.
 * </p>
 */
public interface ActivationManager extends Predicate<RuntimeRule> {

    /**
     * <p>
     * This method is called once for each memory change and before any rule activations. A unique
     * sequence id is supplied as an argument so that developers could distinguish
     * the initial fire and subsequent changes in the working memory caused by RHS calls.
     * </p>
     *
     * @param sequenceId memory task counter starting at zero (initial session fire)
     * @param agenda     rules that are activated by current changes in the working memory.
     */
    default void onAgenda(int sequenceId, List<RuntimeRule> agenda) {

    }

    /**
     * @param rule the rule to be allowed or disallowed for activation
     * @return true if the rule needs to be activated
     */
    @Override
    default boolean test(RuntimeRule rule) {
        return true;
    }

    /**
     * This method is called after rule activation. Developers use this method to track activation history.
     * this manager's <code>test()</code> method
     *
     * @param rule rule
     */
    default void onActivation(RuntimeRule rule) {

    }

}
