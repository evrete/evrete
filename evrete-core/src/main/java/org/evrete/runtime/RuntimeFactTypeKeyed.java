package org.evrete.runtime;

import org.evrete.api.ReIterator;
import org.evrete.api.RuntimeFact;
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
    boolean isBetaNode() {
        return true;
    }

    public ReIterator<ValueRow[]> mainIterator() {
        return keyStorage.main().keyIterator();
    }

    public ReIterator<ValueRow[]> deltaIterator() {
        return keyStorage.delta().keyIterator();
    }

    public SharedBetaFactStorage getKeyStorage() {
        return keyStorage;
    }


    @Override
    public boolean isInsertDeltaAvailable() {
        return keyStorage.delta().keyCount() > 0;
    }

    @Override
    public boolean isDeleteDeltaAvailable() {
        return keyStorage.hasDeletedKeys();
    }

    @Override
    public ReIterator<RuntimeFact> iterator() {
        throw new UnsupportedOperationException();
    }
}
