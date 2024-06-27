package org.evrete.dsl.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for methods that serve as subscribers to the engine's events.
 * Methods annotated with {@code @EventSubscription} will be called in response to each event specified
 * by the method's argument. Annotated methods must adhere to the following constraints:
 * <ul>
 *   <li>
 *       Must be {@code public}.
 *   </li>
 *   <li>
 *       Must be {@code void}.
 *   </li>
 *   <li>
 *       Must have exactly one argument of a type that exactly matches one of
 *   the {@link org.evrete.api.events.ContextEvent} sub-interfaces.
 *   </li>
 * </ul>
 */
@Target(value = ElementType.METHOD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface EventSubscription {

    /**
     * Indicates whether the subscription should be asynchronous.
     *
     * @return true if the subscription is asynchronous, false otherwise.
     */
    boolean async() default false;
}
