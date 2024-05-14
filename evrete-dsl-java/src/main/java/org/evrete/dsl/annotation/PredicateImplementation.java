package org.evrete.dsl.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A marker interface used to indicate that the annotated method is referenced by one or
 * more {@link MethodPredicate} annotations and should not be flagged as unused by the IDE.
 * <p>
 * Alternatively, the {@link org.evrete.api.annotations.RuleElement} annotation can be
 * used for a more generic approach.
 * </p>
 */
@Target(ElementType.METHOD) // Simplified 'value =' as it's the default parameter for annotations
@Retention(RetentionPolicy.RUNTIME)
public @interface PredicateImplementation {

}
