package org.evrete.runtime;

import org.evrete.KnowledgeService;
import org.evrete.api.*;
import org.evrete.runtime.evaluation.AlphaBucketMeta;
import org.evrete.util.SearchList;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.WeakHashMap;

public class KnowledgeRuntime extends AbstractRuntime<RuleDescriptor, Knowledge> implements Knowledge {
    private final WeakHashMap<RuleSession<?>, Object> sessions = new WeakHashMap<>();
    private final Object VALUE = new Object();
    private final SearchList<RuleDescriptor> ruleDescriptors = new SearchList<>();

    public KnowledgeRuntime(KnowledgeService service) {
        super(service);
    }

    @Override
    protected void onNewActiveField(TypeMemoryState newState, ActiveField newField) {
        // Do nothing
    }

    @Override
    public void onNewAlphaBucket(TypeMemoryState newState, FieldsKey key, AlphaBucketMeta meta) {
        // Do nothing
    }

    @Override
    public RuleDescriptor compileRule(RuleBuilder<?> builder) {
        RuleDescriptor rd = super.compileRuleBuilder(builder);
        this.ruleDescriptors.add(rd);
        return rd;
    }

    @Override
    public List<RuleDescriptor> getRules() {
        return Collections.unmodifiableList(ruleDescriptors.getList());
    }

    void close(RuleSession<?> session) {
        synchronized (sessions) {
            sessions.remove(session);
        }
    }

    @Override
    public RuleDescriptor getRule(String name) {
        return ruleDescriptors.get(name);
    }

    @Override
    public Collection<RuleSession<?>> getSessions() {
        return Collections.unmodifiableCollection(sessions.keySet());
    }

    @Override
    public StatefulSession createSession() {
        StatefulSessionImpl session = new StatefulSessionImpl(this);
        sessions.put(session, VALUE);
        return session;
    }
}
