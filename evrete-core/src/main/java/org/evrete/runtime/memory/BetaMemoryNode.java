package org.evrete.runtime.memory;

import org.evrete.api.KeysStore;
import org.evrete.api.RuntimeContext;
import org.evrete.runtime.RuntimeFactType;
import org.evrete.runtime.structure.NodeDescriptor;
import org.evrete.util.Bits;

public interface BetaMemoryNode<D extends NodeDescriptor> {

    KeysStore getMainStore();

    KeysStore getDeltaStore();

    RuntimeContext<?, ?> getRuntime();

    default void mergeDelta() {
        getMainStore().append(getDeltaStore());
        getDeltaStore().clear();
    }

    RuntimeFactType[][] getGrouping();

    D getDescriptor();

    default int getSourceIndex() {
        return getDescriptor().getSourceIndex();
    }

    default Bits getTypeMask() {
        return getDescriptor().getMask();
    }

    default boolean isConditionNode() {
        return getDescriptor().isConditionNode();
    }

}
