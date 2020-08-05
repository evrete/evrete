package org.evrete.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A dev-only annotation which indicates that the method MUST be called
 * within a single thread or that its thread safety MUST be ensured externally.
 */
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD})
@Retention(RetentionPolicy.SOURCE)
public @interface ThreadUnsafe {
}
