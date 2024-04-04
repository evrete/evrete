package org.evrete.dsl.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to mark a class as a ruleset.
 */
@Target(value = ElementType.TYPE)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface RuleSet {
    /**
     * Provides optional name for the ruleset
     * @return name of the ruleset
     */
    String value() default "";

    /**
     * <p>
     * Defines how rules are sorted in the event of an absence or equality of the salience parameter.
     * As compiled Java classes do not retain any sorting information, this method determines the
     * fallback sorting mechanism.
     * </p>
     *
     * @return the mode used for sorting.
     */
    Sort defaultSort() default Sort.BY_NAME;

    /**
     * The {@code Sort} enum represents different sorting modes for rule sets.
     */
    enum Sort {
        /**
         * Sorts rules by name.
         */
        BY_NAME,

        /**
         * Sorts rules by name in reverse order.
         */
        BY_NAME_INVERSE

    }
}
