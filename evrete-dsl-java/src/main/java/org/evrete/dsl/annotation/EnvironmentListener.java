package org.evrete.dsl.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 *     Class methods that are annotated with {@code @EnvironmentListener} will
 *     be called for each external {@link org.evrete.api.Environment#set(String, Object)} invocation.
 *     Annotated methods must be public, void and have only one argument.
 * </p>
 */
@Target(value = ElementType.METHOD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface EnvironmentListener {
    /**
     *
     * @return property name
     */
    String value() default "";

}
