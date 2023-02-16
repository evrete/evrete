package org.evrete.api;

/**
 * <p>
 * An exception handler that is invoked for unchecked exceptions thrown by {@link RuleSet#addRule(RuleBuilder)}
 * </p>
 * <p>
 * The {@link RuleSet}'s default exception handler simply re-throws the exception, thus breaking the whole
 * build process:
 * </p>
 * <code>
 * <pre>
 *     handler = (context, builder, exception) -> {
 *         throw exception;
 *     };</pre>
 * </code>
 * <p>
 * Custom {@link RuleBuilderExceptionHandler} implementations allow developers to optionally omit failed rules,
 * continue with a different {@link RuleBuilder}, or throw unchecked exception if the original (cause) exception
 * is deemed unrecoverable.
 * </p>
 *
 * @see RuleSet#setRuleBuilderExceptionHandler(RuleBuilderExceptionHandler)
 */
public interface RuleBuilderExceptionHandler {

    /**
     * @param context     is either a {@link Knowledge} or a {@link RuleSession} instance
     * @param builder     rule builder that caused the exception
     * @param exception   the exception
     * @throws RuntimeException if developer decides that the original exception is unrecoverable and re-throws the exception (or any other instance of {@link RuntimeException})
     * @see RuleSet#setRuleBuilderExceptionHandler(RuleBuilderExceptionHandler)
     */
    void handle(RuleSet<?> context, RuleBuilder<?> builder, RuntimeException exception);
}
