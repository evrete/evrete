package org.evrete.runtime;

import org.evrete.api.FactHandle;
import org.evrete.api.MapEntry;
import org.evrete.api.StatefulSession;

import java.util.stream.Stream;

public class StatefulSessionImpl extends AbstractRuleSession<StatefulSession> implements StatefulSession {

    StatefulSessionImpl(KnowledgeRuntime knowledge) {
        super(knowledge);
    }

    @Override
    protected StatefulSession thisInstance() {
        return this;
    }

    @Override
    public Stream<MapEntry<FactHandle, Object>> streamFactEntries() {
        return streamFactEntries(false);
    }

    @Override
    public <T> Stream<MapEntry<FactHandle, T>> streamFactEntries(Class<T> type) {
        return this.streamFactEntries(type, false);
    }

    @Override
    public <T> Stream<MapEntry<FactHandle, T>> streamFactEntries(String type) {
        return streamFactEntries(type, false);
    }


    @Override
    public void close() {
        closeInner();
    }

    @Override
    public StatefulSession fire() {
        fireInner();
        return this;
    }

    @Override
    public void clear() {
        clearInner();
    }

}
