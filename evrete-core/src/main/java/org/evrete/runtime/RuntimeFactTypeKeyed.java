package org.evrete.runtime;

import org.evrete.api.ReIterator;
import org.evrete.api.RuntimeFact;
import org.evrete.api.ValueRow;
import org.evrete.api.spi.SharedBetaFactStorage;
import org.evrete.runtime.memory.SessionMemory;
import org.evrete.runtime.structure.FactType;

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

    public ReIterator<ValueRow[]> mainIterator() {
        return keyStorage.main().keyIterator();
    }

    public ReIterator<ValueRow[]> deltaIterator() {
        return keyStorage.delta().keyIterator();
    }

    public SharedBetaFactStorage getKeyStorage() {
        return keyStorage;
    }

    //@Override
    public ReIterator<RuntimeFact> iterator() {
        throw new UnsupportedOperationException();
    }
}
