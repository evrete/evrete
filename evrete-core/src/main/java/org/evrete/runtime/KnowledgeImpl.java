package org.evrete.runtime;

import org.evrete.Configuration;
import org.evrete.api.Knowledge;
import org.evrete.api.RuleBuilder;
import org.evrete.api.StatefulSession;
import org.evrete.runtime.async.ForkJoinExecutor;
import org.evrete.runtime.structure.RuleDescriptor;

import java.util.WeakHashMap;

public class KnowledgeImpl extends AbstractRuntime<Knowledge, RuleDescriptor> implements Knowledge {
    private final WeakHashMap<StatefulSession, Object> sessions = new WeakHashMap<>();
    private final Object VALUE = new Object();

    public KnowledgeImpl(Configuration conf, ForkJoinExecutor executor) {
        super(conf, executor);
    }

    @Override
    public final Kind getKind() {
        return Kind.KNOWLEDGE;
    }

    @Override
    public RuleDescriptor compileRule(RuleBuilder<Knowledge> builder) {
        return buildDescriptor(builder);
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
