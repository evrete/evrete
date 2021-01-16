package org.evrete.runtime;

import org.evrete.api.KeysStore;

public interface BetaMemoryNode<D extends NodeDescriptor> {

    KeysStore getMainStore();

    KeysStore getDeltaStore();

    default void mergeDelta() {
        getMainStore().append(getDeltaStore());
        getDeltaStore().clear();
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
