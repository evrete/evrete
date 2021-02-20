package org.evrete.runtime;

import org.evrete.KnowledgeService;
import org.evrete.api.*;
import org.evrete.runtime.evaluation.AlphaBucketMeta;

import java.util.*;

public class KnowledgeRuntime extends AbstractRuntime<Knowledge> implements Knowledge {
    private final WeakHashMap<KnowledgeSession<?>, Object> sessions = new WeakHashMap<>();
    private final Object VALUE = new Object();
    private Set<EvaluationListener> evaluationListeners = new HashSet<>();

    public KnowledgeRuntime(KnowledgeService service) {
        super(service);
    }

    @Override
    protected void onNewActiveField(ActiveField newField) {
        // Do nothing
    }

    @Override
    protected TypeResolver newTypeResolver() {
        return getService().getTypeResolverProvider().instance(this);
    }

    @Override
    public void onNewAlphaBucket(FieldsKey key, AlphaBucketMeta meta) {
        // Do nothing
    }

    @Override
    public void addListener(EvaluationListener listener) {
        this.evaluationListeners.add(listener);
    }

    @Override
    public Knowledge addImport(String imp) {
        return (Knowledge) super.addImport(imp);
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

    void close(KnowledgeSession<?> session) {
        synchronized (sessions) {
            sessions.remove(session);
        }
    }

    @Override
    public Collection<KnowledgeSession<?>> getSessions() {
        return Collections.unmodifiableCollection(sessions.keySet());
    }

    @Override
    public StatefulSession createSession() {
        StatefulSessionImpl session = new StatefulSessionImpl(this);
        sessions.put(session, VALUE);
        // Copy evaluation listeners to the newly spawned session
        for (EvaluationListener listener : this.evaluationListeners) {
            session.addListener(listener);
        }
        return session;
    }

    @Override
    public void removeListener(EvaluationListener listener) {
        this.evaluationListeners.remove(listener);
    }
}
