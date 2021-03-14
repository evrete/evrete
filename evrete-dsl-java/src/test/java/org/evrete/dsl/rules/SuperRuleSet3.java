package org.evrete.dsl.rules;

public class SuperRuleSet3 {

    @SuppressWarnings({"unused"})
    public static boolean test(Integer i1, Integer i2, Integer i3) {
        return i3 == i1 * i2;
    }

}
