package org.evrete.dsl.rules;

import org.evrete.api.RhsContext;
import org.evrete.api.events.EnvironmentChangeEvent;
import org.evrete.api.events.SessionClosedEvent;
import org.evrete.api.events.SessionCreatedEvent;
import org.evrete.api.events.SessionFireEvent;
import org.evrete.dsl.annotation.EventSubscription;
import org.evrete.dsl.annotation.Fact;
import org.evrete.dsl.annotation.Rule;
import org.evrete.dsl.annotation.Where;

import static org.evrete.dsl.TestUtils.PhaseHelperData.event;

public class PhaseListenerRuleSet1 {

    @EventSubscription
    public static void onSessionCreate(SessionCreatedEvent sessionCreatedEvent) {
        event(sessionCreatedEvent);
    }

    @EventSubscription
    public static void onSessionFire(SessionFireEvent event) {
        event(event);
    }

    @EventSubscription
    public static void onSessionClose(SessionClosedEvent event) {
        event(event);
    }

    @EventSubscription
    public static void onEnvironmentChange(EnvironmentChangeEvent event) {
        event(event);
    }

    @Rule
    @Where(value = {"$i > 0"})
    public void rule(RhsContext ctx, @Fact("$i") int $i) {
    }
}
