package org.evrete.runtime.memory;

import org.evrete.api.KeysStore;
import org.evrete.api.ReIterator;
import org.evrete.api.ThreadUnsafe;

interface NodeIterationStateFactory<S extends NodeIterationStateFactory.State, E> {
    /**
     * This method will be called for each batch of evaluation tasks regardless of
     * its size
     */
    S newIterationState();

    boolean hasNonPlainSources();

    interface State {
        void saveTo(KeysStore destination);

        boolean evaluate();

        void setEvaluationEntry(KeysStore.Entry entry, int sourceId);

        void setSecondaryEntry(KeysStore.Entry entry, int nonPlainIndex);

        @ThreadUnsafe
        ReIterator<KeysStore.Entry>[] buildSecondary();
    }
}
