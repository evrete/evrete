package org.evrete.dsl.annotation;

import org.evrete.dsl.DefaultSort;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value = ElementType.TYPE)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface RuleSortPolicy {
    DefaultSort value() default DefaultSort.BY_NAME;
}
