package org.evrete.runtime;

import org.evrete.api.KeyReIterators;
import org.evrete.api.Memory;
import org.evrete.api.SharedBetaFactStorage;
import org.evrete.api.ValueRow;
import org.evrete.runtime.memory.SessionMemory;
import org.evrete.util.ValueRowToArray;

import static org.evrete.api.KeyMode.KNOWN_KEYS_NEW_FACTS;
import static org.evrete.api.KeyMode.NEW_KEYS_NEW_FACTS;

public class RuntimeFactTypeKeyed extends RuntimeFactType {
    private final SharedBetaFactStorage keyStorage;
    private final KeyReIterators<ValueRow> keyIterators;
    private final KeyReIterators<ValueRow[]> mappedKeyIterators;

    public RuntimeFactTypeKeyed(SessionMemory runtime, FactType other) {
        super(runtime, other);
        this.keyStorage = runtime.getBetaFactStorage(other);
        this.keyIterators = runtime.getBetaFactStorage(other).keyIterators();
        this.mappedKeyIterators = runtime.getBetaFactStorage(other).keyIterators(ValueRowToArray.SUPPLIER);
    }

    public RuntimeFactTypeKeyed(RuntimeFactTypeKeyed other) {
        super(other.getRuntime(), other);
        this.keyStorage = other.keyStorage;
        this.keyIterators = other.keyIterators;
        this.mappedKeyIterators = other.mappedKeyIterators;
    }


    @Override
    public Memory getSource() {
        return keyStorage;
    }

    @Override
    boolean isBetaNode() {
        return true;
    }

    public KeyReIterators<ValueRow> getKeyIterators() {
        return keyIterators;
    }

    public KeyReIterators<ValueRow[]> getMappedKeyIterators() {
        return mappedKeyIterators;
    }

    @Override
    public boolean isInActiveState() {
        return
                keyIterators.keyIterator(NEW_KEYS_NEW_FACTS).reset() > 0
                        ||
                        keyIterators.keyIterator(KNOWN_KEYS_NEW_FACTS).reset() > 0;
    }

    @Override
    public void resetState() {
    }

    public SharedBetaFactStorage getKeyStorage() {
        return keyStorage;
    }

    @Override
    public boolean isInsertDeltaAvailable() {
        return
                keyIterators.keyIterator(NEW_KEYS_NEW_FACTS).reset() > 0
                        ||
                        keyIterators.keyIterator(KNOWN_KEYS_NEW_FACTS).reset() > 0;

    }

    public boolean hasDeltaKeys() {
        return isInsertDeltaAvailable();
    }

    @Override
    public boolean isDeleteDeltaAvailable() {
        return keyStorage.hasDeletedKeys();
    }
}
