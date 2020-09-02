package org.evrete.runtime;

import org.evrete.api.ActivationManager;
import org.evrete.api.RuntimeRule;

public class UnconditionalActivationManager implements ActivationManager {

    @Override
    public void reset(int sequenceId) {
    }

    @Override
    public void onActivation(RuntimeRule rule) {
    }

    @Override
    public boolean test(RuntimeRule runtimeRule) {
        return true;
    }
}
