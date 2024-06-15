package org.evrete.runtime;

import org.evrete.api.FactHandle;
import org.evrete.api.MapEntry;
import org.evrete.api.StatelessSession;

import java.util.stream.Stream;

//TODO test each method!!!!!!
class StatelessSessionImpl extends AbstractRuleSession<StatelessSession> implements StatelessSession {

    StatelessSessionImpl(KnowledgeRuntime knowledge) {
        super(knowledge);
    }

    @Override
    protected StatelessSession thisInstance() {
        return this;
    }

    @Override
    public Void fire() {
        try {
            fireInner();
            return null;
        } finally {
            closeInner();
        }
    }

    @Override
    public Stream<MapEntry<FactHandle, Object>> streamFactEntries() {
        fireInner();
        return streamFactEntries(true);
    }

    @Override
    public <T> Stream<MapEntry<FactHandle, T>> streamFactEntries(String type) {
        fireInner();
        return streamFactEntries(type, true);
    }

    @Override
    public <T> Stream<MapEntry<FactHandle, T>> streamFactEntries(Class<T> type) {
        fireInner();
        return streamFactEntries(type, true);
    }
}
