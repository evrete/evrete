package org.evrete.runtime;

import org.evrete.KnowledgeService;
import org.evrete.api.ActiveField;
import org.evrete.api.Knowledge;
import org.evrete.api.StatefulSession;
import org.evrete.api.TypeResolver;
import org.evrete.runtime.evaluation.AlphaDelta;

import java.util.WeakHashMap;

public class KnowledgeImpl extends AbstractRuntime<Knowledge> implements Knowledge {
    private final WeakHashMap<StatefulSession, Object> sessions = new WeakHashMap<>();
    private final Object VALUE = new Object();

    public KnowledgeImpl(KnowledgeService service) {
        super(service);
    }

    @Override
    protected TypeResolver newTypeResolver() {
        return getService().getTypeResolverProvider().instance(this);
    }


    @Override
    protected void onNewActiveField(ActiveField newField) {
        // Do nothing
    }

    @Override
    protected void onNewAlphaBucket(AlphaDelta delta) {
        // Do nothing
    }

    @Override
    public Knowledge addImport(String imp) {
        super.addImport(imp);
        return this;
    }

    @Override
    public Knowledge addImport(Class<?> type) {
        super.addImport(type);
        return this;
    }

    @Override
    public final Kind getKind() {
        return Kind.KNOWLEDGE;
    }

    void close(StatefulSession session) {
        sessions.remove(session);
    }

    public WeakHashMap<StatefulSession, Object> getSessions() {
        return sessions;
    }

    @Override
    public StatefulSessionImpl createSession() {
        StatefulSessionImpl session = new StatefulSessionImpl(this);
        sessions.put(session, VALUE);
        return session;
    }

}
