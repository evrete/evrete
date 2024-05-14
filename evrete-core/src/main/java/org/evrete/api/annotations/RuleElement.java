package org.evrete.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A marker interface indicating that the annotated type, method, field, parameter, or package is being used inside rules.
 * Use this annotation in your project's code inspection tools, e.g., to avoid warnings about the element being unused.
 */
@Target(value = {ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.PACKAGE})
@Retention(value = RetentionPolicy.RUNTIME)
public @interface RuleElement {

}
