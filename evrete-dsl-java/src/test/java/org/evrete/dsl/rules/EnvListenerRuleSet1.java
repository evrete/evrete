package org.evrete.dsl.rules;

import org.evrete.dsl.annotation.EnvironmentListener;
import org.evrete.dsl.annotation.Rule;

import static org.evrete.dsl.TestUtils.EnvHelperData.add;

public class EnvListenerRuleSet1 {

    @EnvironmentListener("property1")
    public static void set(String s) {
        add("property1", s);
    }

    @Rule
    public void rule(int i) {
    }

    @EnvironmentListener("property2")
    public void set(int i) {
        add("property2", i);
    }

}
