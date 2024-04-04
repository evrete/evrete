package org.evrete.dsl.annotation;

import org.evrete.dsl.Phase;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to bind listener methods to specific lifecycle events.
 */
@Target(value = ElementType.METHOD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface PhaseListener {
    /**
     * Defines phase events that will trigger the annotated method.
     *
     * @return an array of `Phase` values
     */
    Phase[] value() default {};

}
