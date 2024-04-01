package org.evrete.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the API of the annotated method or type is subject to change in future versions.
 * This annotation serves as a warning to developers that they should use the
 * method or type with caution, as its parameters, methods, or behavior could change.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(value = RetentionPolicy.RUNTIME)
public @interface Unstable {
}
