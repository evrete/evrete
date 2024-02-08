package org.evrete.helper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@DisabledIfSystemProperty(named = "IgnoreInOSGiTest", matches = "true")
public @interface IgnoreTestInOSGi {
}