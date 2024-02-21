package org.evrete.api;

/**
 * @see RuleSet#setRuleBuilderExceptionHandler(RuleBuilderExceptionHandler)
 * @deprecated this class has been deprecated since version 3.1.0 with bulk rule compilation
 */
@Deprecated
public interface RuleBuilderExceptionHandler {

    /**
     * @param context   is either a {@link Knowledge} or a {@link RuleSession} instance
     * @param rule  rule that caused the exception
     * @param exception the exception
     * @throws RuntimeException if developer decides that the original exception is unrecoverable and re-throws the exception (or any other instance of {@link RuntimeException})
     * @see RuleSet#setRuleBuilderExceptionHandler(RuleBuilderExceptionHandler)
     * @deprecated
     */
    @Deprecated
    void handle(RuleSet<?> context, Rule rule, RuntimeException exception);
}
