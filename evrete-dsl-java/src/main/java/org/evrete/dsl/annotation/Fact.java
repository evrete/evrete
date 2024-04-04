package org.evrete.dsl.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be used to mark a parameter in a method with a fact value.
 * It is used in conjunction with the Rete rule engine to specify a named fact value
 * that is used in the rule condition or action.
 */
@Target(value = ElementType.PARAMETER)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface Fact {
    /**
     * The fact variable, e.g '$customer'
     * @return fact variable
     */
    String value();

    /**
     * Because the engine allows the same Java classes to have
     * different logical types, this parameter enables the explicit identification of the fact's logical type.
     * @see org.evrete.api.Type
     * @return the logical type of the argument
     */
    String type() default "";
}
