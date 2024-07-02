package org.evrete.runtime;

import org.evrete.api.FactHandle;
import org.evrete.api.StatelessSession;

import java.util.Map;
import java.util.stream.Stream;

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
    public Stream<Map.Entry<FactHandle, Object>> streamFactEntries() {
        fireInner();
        try {
            return streamFactEntries(true);
        } finally {
            closeInner();
        }
    }

    @Override
    public <T> Stream<Map.Entry<FactHandle, T>> streamFactEntries(String type) {
        fireInner();
        try {
            return streamFactEntries(type, true);
        } finally {
            closeInner();
        }
    }

    @Override
    public <T> Stream<Map.Entry<FactHandle, T>> streamFactEntries(Class<T> type) {
        fireInner();
        try {
            return streamFactEntries(type, true);
        } finally {
            closeInner();
        }
    }
}
