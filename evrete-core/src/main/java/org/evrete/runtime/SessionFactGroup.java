package org.evrete.runtime;

import org.evrete.api.spi.MemoryScope;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * A session counterpart of the {@link KnowledgeFactGroup}. The goal here is to create them as fast as possible
 * using the information already gathered on the knowledge level.
 */
abstract class SessionFactGroup extends KnowledgeFactGroup {
    protected final Executor executor;
    protected final SessionFactType[] factTypes;


    protected SessionFactGroup(AbstractRuleSessionBase<?> runtime, KnowledgeFactGroup knowledgeFactGroup) {
        super(knowledgeFactGroup);
        this.executor = runtime.getService().getExecutor();
        FactType[] entryNodes = getEntryNodes();
        SessionMemory memory = runtime.getMemory();
        this.factTypes = new SessionFactType[entryNodes.length];
        for (int i = 0; i < entryNodes.length; i++) {
            SessionFactType sessionFactType = new SessionFactType(entryNodes[i], memory);
            this.factTypes[i] = sessionFactType;
        }

    }

    SessionFactType[] getFactTypes() {
        return factTypes;
    }

    abstract CompletableFuture<Void> processDeleteDeltaActions(Collection<FactHolder> deletes);

    protected abstract boolean isPlain();

    abstract Iterator<DefaultFactHandle[]> factHandles(MemoryScope scope);

    abstract CompletableFuture<Void> commitDeltas();

    abstract CompletableFuture<Void> buildDeltas(DeltaMemoryMode mode);

    public static SessionFactGroup factory(AbstractRuleSessionBase<?> runtime, KnowledgeFactGroup knowledgeFactGroup) {
        if (knowledgeFactGroup instanceof Plain) {
            return new SessionFactGroupPlain(runtime, (Plain) knowledgeFactGroup);
        } else if(knowledgeFactGroup instanceof Beta) {
            return new SessionFactGroupBeta(runtime, (Beta) knowledgeFactGroup);
        } else {
            throw new IllegalArgumentException("Unknown ReteFactGroup type: " + knowledgeFactGroup);
        }
    }

    @Override
    public String toString() {
        return "{" +
                "plain=" + isPlain() +
                "facts= " + FactType.toSimpleDebugString(factTypes) +
                '}';
    }
}
