package org.evrete.runtime.memory;

import org.evrete.api.KeysStore;
import org.evrete.api.ReIterator;
import org.evrete.runtime.BetaEvaluationState;

public interface NodeIterationStateFactory<S extends NodeIterationStateFactory.State> {
    /**
     * This method will be called for each batch of evaluation tasks regardless of
     * its size
     */
    S newIterationState(BetaConditionNode node);

    interface State extends BetaEvaluationState {
        void saveTo(KeysStore destination);

        void setEvaluationEntry(KeysStore.Entry entry, int sourceId);

        void setSecondaryEntry(KeysStore.Entry entry, int nonPlainIndex);

        boolean hasNonPlainSources();

        ReIterator<KeysStore.Entry>[] buildSecondary();
    }
}
