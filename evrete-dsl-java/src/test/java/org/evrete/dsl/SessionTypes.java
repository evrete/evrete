package org.evrete.dsl;

import org.evrete.api.ActivationMode;
import org.evrete.api.Knowledge;
import org.evrete.api.RuleSession;

public enum SessionTypes {
    SF_D(true, ActivationMode.DEFAULT),
    SF_C(true, ActivationMode.CONTINUOUS),
    SL_D(false, ActivationMode.DEFAULT),
    SL_C(false, ActivationMode.CONTINUOUS);

    private final ActivationMode mode;
    private final boolean stateful;


    SessionTypes(boolean stateful, ActivationMode mode) {
        this.stateful = stateful;
        this.mode = mode;
    }

    RuleSession<?> session(Knowledge knowledge) {
        if (stateful) {
            return knowledge.newStatefulSession(mode);
        } else {
            return knowledge.newStatelessSession(mode);
        }
    }
}
