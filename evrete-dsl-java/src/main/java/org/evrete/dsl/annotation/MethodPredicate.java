package org.evrete.dsl.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value = ElementType.METHOD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface MethodPredicate {
    /**
     * <p>
     * Name of the method that will represent an LHS condition.
     * </p>
     *
     * @return name of the method
     */
    String method();

    /**
     * @see #args()
     * @deprecated deprecated in favor of {@link #args()}
     */
    @Deprecated
    String[] descriptor() default {};

    /**
     * @return array of field references which will be method arguments.
     */
    String[] args() default {};
}
