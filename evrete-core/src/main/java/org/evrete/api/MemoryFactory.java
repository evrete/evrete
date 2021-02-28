package org.evrete.api;

import org.evrete.runtime.ActiveField;

import java.util.function.BiPredicate;

//TODO !!! Javadoc
public interface MemoryFactory {

    KeysStore newKeyStore(int[] factTypeCounts);

    SharedBetaFactStorage newBetaStorage(ActiveField[] fields);

    ValueResolver getValueResolver();

    <Z> FactStorage<Z> newFactStorage(Type<?> type, Class<Z> storageClass, BiPredicate<Z, Z> identityFunction);

    default <Z> KeysStore newKeyStore(Z[][] grouping) {
        int[] factTypeCounts = new int[grouping.length];
        for (int i = 0; i < grouping.length; i++) {
            factTypeCounts[i] = grouping[i].length;
        }
        return newKeyStore(factTypeCounts);
    }
}
