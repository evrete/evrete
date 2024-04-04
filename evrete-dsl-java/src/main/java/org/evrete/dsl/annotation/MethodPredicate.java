package org.evrete.dsl.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The {@code MethodPredicate} annotation is used to define a predicate method that
 * represents a condition for a rule.
 */
@Target(value = ElementType.METHOD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface MethodPredicate {

    /**
     * Name of the method that will represent an LHS condition.
     *
     * @return name of the method
     */
    String method();

    /**
     * Specifies which fact fields are to be passed to the method as its arguments.
     * The corresponding field types must match the method's signature.
     *
     * @return array of field references which will be method arguments.
     */
    String[] args() default {};
}
