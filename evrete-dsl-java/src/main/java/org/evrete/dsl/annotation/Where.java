package org.evrete.dsl.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be used to define conditions for a rule method.
 * It supports both literal conditions and method references.
 */
@Target(value = ElementType.METHOD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface Where {
    /**
     * This annotation value defines an array of literal conditions such as `["$c.type == $cat.id", "$c.rating > 30.0"]`.
     *
     * @return an array of literal conditions
     */
    String[] value() default {};

    /**
     * Defines conditions expressed as method references.
     *
     * @return array of {@link MethodPredicate} conditions
     */
    MethodPredicate[] methods() default {};
}
