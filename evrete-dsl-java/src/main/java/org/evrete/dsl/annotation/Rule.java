package org.evrete.dsl.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a method as a rule within a rule-based system. The arguments of the method are treated as declared facts,
 * and the body of the method constitutes the action of the rule.
 * The conditions of the rule are established through additional method annotations.
 */
@Target(value = ElementType.METHOD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface Rule {
    int DEFAULT_SALIENCE = Integer.MIN_VALUE;
    /**
     * Optional identifier or name for the rule. Useful for rule management and referencing.
     * @return the declared name of the rule
     */
    String value() default "";

    /**
     * Determines the priority of the rule relative to others.
     * @return the salience of the rule
     */
    int salience() default DEFAULT_SALIENCE;
}
