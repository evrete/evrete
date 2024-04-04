package org.evrete.dsl.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for methods intended to listen to changes in the {@link org.evrete.api.Environment}.
 * Methods annotated with {@code @EnvironmentListener} will be called in response to each
 * {@link org.evrete.api.Environment#set(String, Object)} invocation that affects the specified environment key.
 * Annotated methods must adhere to the following constraints:
 * <ul>
 *   <li>Must be {@code public}.</li>
 *   <li>Must return {@code void}.</li>
 *   <li>Must have exactly one argument (the value corresponding to the environment key of interest).</li>
 * </ul>
 */
@Target(value = ElementType.METHOD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface EnvironmentListener {
    /**
     * Specifies the key from {@link org.evrete.api.Environment} to be used as the method argument.
     *
     * @return The key of the environment.
     */
    String value() default "";

}
