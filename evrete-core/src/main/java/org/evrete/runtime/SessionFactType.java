package org.evrete.runtime;

import org.evrete.api.spi.MemoryScope;
import org.evrete.runtime.rete.ConditionMemory;
import org.evrete.util.FlatMapIterator;

import java.util.Iterator;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * A session representation of a fact type declaration.
 */
class SessionFactType extends FactType {
    private static final Logger LOGGER = Logger.getLogger(SessionFactType.class.getName());
    private final SessionMemory memory;

    /**
     * Constructs an {@code SessionFactType} with the specified parameters.
     *
     * @param factType the fact type this node is created from
     * @param memory   an instance of the session memory
     */
    SessionFactType(FactType factType, SessionMemory memory) {
        super(factType);
        this.memory = memory;
    }

    FactHolder getFact(DefaultFactHandle handle) {
        return Objects.requireNonNull(
                memory.getTypeMemory(handle).get(handle),
                () -> "No fact found for " + handle + " at " + SessionFactType.this
        );
    }

    /**
     * Returns alpha-memory for this node. We're not making it final in the constructor because alpha-memories
     * can be rebuilt when a new rule is appended to a {@link org.evrete.api.RuleSession}
     *
     * @return alpha-memory
     */
    private TypeAlphaMemory alphaMemory() {
        return memory.getAlphaMemory(this.getAlphaAddress());
    }

    /**
     * Iterator over fact storage keys in an alpha-memory
     *
     * @param scope memory scope
     * @return key iterator
     * @see org.evrete.api.spi.DeltaGroupedFactStorage
     */
    Iterator<Long> keyIterator(MemoryScope scope) {
        LOGGER.finer(() -> "Requested " + scope + " key iterator for fact type '" + getVarName() + "', alpha address: " + getAlphaAddress() + ", has data: " + alphaMemory().keyIterator(scope).hasNext());
        return alphaMemory().keyIterator(scope);
    }

    /**
     * Iterator over fact storage values in an alpha-memory
     *
     * @param scope memory scope
     * @return fact handle iterator
     * @see org.evrete.api.spi.DeltaGroupedFactStorage
     */
    Iterator<DefaultFactHandle> factIterator(MemoryScope scope) {
        return new FlatMapIterator<>(
                keyIterator(scope),
                values -> alphaMemory().valueIterator(scope, values)
        );
    }

    /**
     * Delegating method,
     *
     * @return fact handle iterator
     * @see org.evrete.api.spi.DeltaGroupedFactStorage
     */
    Iterator<DefaultFactHandle> factIterator(ConditionMemory.ScopedValueId key) {
        return alphaMemory().valueIterator(key.getScope(), key.getValueId());
    }

    @Override
    public String toString() {
        return "{" +
                "var='" + getVarName() + "'" +
                ", type=" + type().getId() +
                ", alphaAddress=" + getAlphaAddress() +
                '}';
    }
}
