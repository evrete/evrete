package org.evrete.dsl.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value = ElementType.METHOD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface Where {
    /**
     * <p>
     * This annotation value defines an array of literal conditions like "[$c.type == $cat.id, $c.rating &gt; 30.0]".
     * The implementation must parse and match every condition with the method signature.
     * So in the example above the annotated method might look like {@code void doSomething(Customer $a, Category $cat)}
     * </p>
     *
     * @return array of literal conditions
     */
    String[] value() default {};

    /**
     * @return array of MethodPredicate conditions
     * @see MethodPredicate
     */
    MethodPredicate[] asMethods() default {};


}
