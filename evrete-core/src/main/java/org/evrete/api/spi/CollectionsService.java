package org.evrete.api.spi;

import org.evrete.api.FieldsKey;
import org.evrete.api.KeysStore;

public interface CollectionsService {

    KeysStore newKeyStore(int[] factTypeCounts);

    SharedBetaFactStorage newBetaStorage(FieldsKey typeFields);

    SharedPlainFactStorage newPlainStorage();

    default <Z> KeysStore newKeyStore(Z[][] grouping) {
        int[] factTypeCounts = new int[grouping.length];
        for (int i = 0; i < grouping.length; i++) {
            factTypeCounts[i] = grouping[i].length;
        }
        return newKeyStore(factTypeCounts);
    }
}
