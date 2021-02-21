package org.evrete.runtime;

import org.evrete.api.KeyMode;
import org.evrete.api.KeysStore;

public interface BetaMemoryNode<D extends NodeDescriptor> {

    KeysStore getStore(KeyMode mode);

    default void mergeDelta() {
        KeysStore delta = getStore(KeyMode.UNKNOWN_UNKNOWN);
        KeysStore main = getStore(KeyMode.KNOWN_KNOWN);
        main.append(delta);
        delta.clear();
    }

    void clear();

    RuntimeFactType[][] getGrouping();

    D getDescriptor();

    default int getSourceIndex() {
        return getDescriptor().getSourceIndex();
    }

    default boolean isConditionNode() {
        return getDescriptor().isConditionNode();
    }

}
