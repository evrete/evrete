package org.evrete.dsl.annotation;

import org.evrete.dsl.Sort;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value = ElementType.TYPE)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface RuleSortPolicy {
    Sort value() default Sort.BY_NAME;
}
