package org.evrete.dsl.rules;

import org.evrete.api.events.EnvironmentChangeEvent;
import org.evrete.dsl.annotation.EventSubscription;
import org.evrete.dsl.annotation.Rule;

import static org.evrete.dsl.TestUtils.EnvHelperData.add;

public class EnvListenerRuleSet1 {

    @EventSubscription
    public static void set(EnvironmentChangeEvent event) {
        add(event);
    }

    @Rule
    public void rule(int i) {
    }

    @EventSubscription
    public void set1(EnvironmentChangeEvent event) {
        add(event);
    }

}
