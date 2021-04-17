package org.evrete.dsl.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value = ElementType.TYPE)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface RuleSet {
    /**
     * @return name of the ruleset
     */
    String value() default "";

    /**
     * <p>
     * Defines how rules are sorted by default.
     * (Compiled Java classes do not keep any sorting information)
     * </p>
     *
     * @return
     */
    Sort defaultSort() default Sort.BY_NAME;

    enum Sort {
        BY_NAME(1),
        BY_NAME_INVERSE(-1);

        public static final Sort DEFAULT = Sort.BY_NAME;
        final int modifier;

        Sort(int modifier) {
            this.modifier = modifier;
        }

        public int getModifier() {
            return modifier;
        }
    }
}
