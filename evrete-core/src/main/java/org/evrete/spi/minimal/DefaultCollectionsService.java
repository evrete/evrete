package org.evrete.spi.minimal;

import org.evrete.api.FieldsKey;
import org.evrete.api.KeysStore;
import org.evrete.api.spi.CollectionsService;
import org.evrete.api.spi.SharedBetaFactStorage;
import org.evrete.api.spi.SharedPlainFactStorage;

public class DefaultCollectionsService implements CollectionsService {

    private static KeysStore factory(int[] arraySizes, int level) {
        int depth = arraySizes.length;
        int arrSize = arraySizes[level];
        assert arrSize > 0;
        if (level == depth - 1) {
            return new KeysStorePlain(level, arrSize);
        } else {
            return new KeysStoreMap(level, arrSize, () -> factory(arraySizes, level + 1));
        }
    }

    @Override
    public SharedBetaFactStorage newBetaStorage(FieldsKey typeFields) {
        return new SharedBetaDataTuple(typeFields);
    }

    @Override
    public KeysStore newKeyStore(int[] factTypeCounts) {
        return factory(factTypeCounts, 0);
    }

    @Override
    public SharedPlainFactStorage newPlainStorage() {
        return new SharedAlphaData();
    }
}
