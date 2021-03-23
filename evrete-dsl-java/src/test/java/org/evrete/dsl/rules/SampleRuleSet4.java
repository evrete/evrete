package org.evrete.dsl.rules;

import org.evrete.dsl.TestUtils;
import org.evrete.dsl.annotation.Fact;
import org.evrete.dsl.annotation.MethodPredicate;
import org.evrete.dsl.annotation.Rule;
import org.evrete.dsl.annotation.Where;

import java.io.File;
import java.util.Random;

@SuppressWarnings({"unused", "MethodMayBeStatic", "ResultOfMethodCallIgnored"})
public class SampleRuleSet4 {
    private static final String PROP = "some-unused-property";

    public static boolean testLong(Long l) {
        TestUtils.testFile(l);
        return l > 0;
    }

    @Rule
    public void ruleInteger(@Fact("$i") int $i) {
        String s = (Math.abs(new Random().nextInt())) + ".dat";
        new File(s).exists();
    }

    @Rule
    @Where(asMethods = {@MethodPredicate(method = "testLong", descriptor = {"$l"})})
    public void ruleLong(@Fact("$l") Long $l) {
        System.setProperty(PROP, "true");
    }
}
