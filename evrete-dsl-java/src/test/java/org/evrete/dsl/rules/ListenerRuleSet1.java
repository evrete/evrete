package org.evrete.dsl.rules;

import org.evrete.Configuration;
import org.evrete.api.Environment;
import org.evrete.api.RhsContext;
import org.evrete.dsl.Phase;
import org.evrete.dsl.annotation.Fact;
import org.evrete.dsl.annotation.PhaseListener;
import org.evrete.dsl.annotation.Rule;
import org.evrete.dsl.annotation.Where;

import static org.evrete.dsl.ListenerInvocationData.event;

public class ListenerRuleSet1 {

    @PhaseListener(Phase.BUILD)
    public static void onBuild() {
        event(Phase.BUILD);
    }

    @PhaseListener(Phase.CREATE)
    public static void onCreate1(Configuration c) {
        event(Phase.CREATE);
    }

    @Rule
    @Where(value = {"$i > 0"})
    public void rule(RhsContext ctx, @Fact("$i") int $i) {
    }

    @PhaseListener(Phase.CREATE)
    public void onCreate2(Environment e) {
        event(Phase.CREATE);
    }

    @PhaseListener(Phase.FIRE)
    public void onFire(Environment e) {
        event(Phase.FIRE);
    }

    @PhaseListener(Phase.CLOSE)
    public void onClose(Environment e) {
        event(Phase.CLOSE);
    }


    @PhaseListener({Phase.CREATE, Phase.FIRE, Phase.CLOSE})
    public void multiple() {
        event(Phase.CREATE, Phase.FIRE, Phase.CLOSE);
    }
}
