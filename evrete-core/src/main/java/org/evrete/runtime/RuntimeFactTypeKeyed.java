package org.evrete.runtime;

import org.evrete.api.Memory;
import org.evrete.api.ReIterator;
import org.evrete.api.ValueRow;
import org.evrete.api.spi.SharedBetaFactStorage;
import org.evrete.runtime.memory.SessionMemory;

public class RuntimeFactTypeKeyed extends RuntimeFactType {
    private final SharedBetaFactStorage keyStorage;

    public RuntimeFactTypeKeyed(SessionMemory runtime, FactType other) {
        super(runtime, other);
        this.keyStorage = runtime.getBetaFactStorage(other);
    }

    public RuntimeFactTypeKeyed(RuntimeFactTypeKeyed other) {
        super(other.getRuntime(), other);
        this.keyStorage = other.keyStorage;
    }

    @Override
    public Memory getSource() {
        return keyStorage;
    }

    @Override
    boolean isBetaNode() {
        return true;
    }

    public ReIterator<ValueRow[]> mainIterator() {
        return keyStorage.main().keyIterator();
    }

    public ReIterator<ValueRow[]> deltaIterator() {
        return keyStorage.deltaNewKeys().keyIterator();
    }

    public SharedBetaFactStorage getKeyStorage() {
        return keyStorage;
    }

    @Override
    public boolean isInsertDeltaAvailable() {
        return keyStorage.deltaNewKeys().keyCount() > 0 || keyStorage.deltaKnownKeys().keyCount() > 0;
    }

    public boolean hasDeltaKeys() {
        return keyStorage.deltaNewKeys().keyCount() > 0 || keyStorage.deltaKnownKeys().keyCount() > 0;
    }

    @Override
    public boolean isDeleteDeltaAvailable() {
        return keyStorage.hasDeletedKeys();
    }
}
