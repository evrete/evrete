package org.evrete.runtime;

import org.evrete.api.spi.MemoryScope;
import org.evrete.util.CombinationIterator;
import org.evrete.util.FlatMapIterator;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

class SessionFactGroupPlain extends SessionFactGroup {
    private static final Logger LOGGER = Logger.getLogger(SessionFactGroupPlain.class.getName());
    private final DefaultFactHandle[] currentFactHandles;
    private final MemoryScope[] currentScopes;

    SessionFactGroupPlain(AbstractRuleSessionBase<?> runtime, Plain reteFactGroup) {
        super(runtime, reteFactGroup);
        this.currentScopes = new MemoryScope[factTypes.length];
        this.currentFactHandles = new DefaultFactHandle[factTypes.length];
    }

    @Override
    protected boolean isPlain() {
        return true;
    }

    @Override
    CompletableFuture<Void> commitDeltas() {
        // Nothing to commit
        return CompletableFuture.completedFuture(null);
    }

    @Override
    CompletableFuture<Void> processDeleteDeltaActions(Collection<FactHolder> deletes) {
        // Plain fact groups have no memory, skipping
        return CompletableFuture.completedFuture(null);
    }

    @Override
    CompletableFuture<Void> buildDeltas(DeltaMemoryMode mode) {
        // Nothing to compute
        return CompletableFuture.completedFuture(null);
    }

    @Override
    Iterator<DefaultFactHandle[]> factHandles(MemoryScope scope) {
        // 1.  For each entry node we need alternate scope combinations
        Iterator<MemoryScope[]> scopesIterator = MemoryScope.states(scope, currentScopes);
        // 2.  Flatmap iterator
        return new FlatMapIterator<>(scopesIterator, this::deltaIterator);
    }

    private Iterator<DefaultFactHandle[]> deltaIterator(MemoryScope[] scopes) {
        return new CombinationIterator<>(
                currentFactHandles,
                index -> factTypes[index].factIterator(scopes[index])
        );
    }

}
