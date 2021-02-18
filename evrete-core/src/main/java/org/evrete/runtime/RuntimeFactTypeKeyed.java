package org.evrete.runtime;

import org.evrete.api.KeyReIterators;
import org.evrete.api.SharedBetaFactStorage;
import org.evrete.api.ValueRow;
import org.evrete.api.spi.InnerFactMemory;
import org.evrete.util.ValueRowToArray;

public class RuntimeFactTypeKeyed extends RuntimeFactType {
    private final SharedBetaFactStorage keyStorage;
    private final KeyReIterators<ValueRow> keyIterators;
    private final KeyReIterators<ValueRow[]> mappedKeyIterators;

    RuntimeFactTypeKeyed(AbstractKnowledgeSession<?> runtime, FactType other) {
        super(runtime, other);
        this.keyStorage = runtime.getMemory().getBetaFactStorage(other);
        this.keyIterators = runtime.getMemory().getBetaFactStorage(other).keyIterators();
        this.mappedKeyIterators = runtime.getMemory().getBetaFactStorage(other).keyIterators(ValueRowToArray.SUPPLIER);
    }

    RuntimeFactTypeKeyed(RuntimeFactTypeKeyed other) {
        super(other.getRuntime(), other);
        this.keyStorage = other.keyStorage;
        this.keyIterators = other.keyIterators;
        this.mappedKeyIterators = other.mappedKeyIterators;
    }

    @Override
    public InnerFactMemory getSource() {
        return keyStorage;
    }

    KeyReIterators<ValueRow> getKeyIterators() {
        return keyIterators;
    }

    KeyReIterators<ValueRow[]> getMappedKeyIterators() {
        return mappedKeyIterators;
    }
}
