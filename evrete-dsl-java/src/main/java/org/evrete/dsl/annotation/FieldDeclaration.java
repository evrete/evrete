package org.evrete.dsl.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Marks a method as a field declaration. New field's name becomes this annotation's <code>name()</code>
 * parameter or the annotated method's name if the <code>name()</code> is empty.
 * </p>
 * <p>
 * The annotated method must be non-void and have a single argument. The argument's type
 * denotes the fact type that we want to declare a new field on. The method's return type becomes
 * the field's type.
 * </p>
 * <p>
 * The annotated method can use both instance and static class members to compute field value.
 * </p>
 */
@Target(value = ElementType.METHOD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface FieldDeclaration {
    String name() default "";

}
