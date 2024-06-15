package org.evrete.runtime;

import org.evrete.KnowledgeService;
import org.evrete.api.*;
import org.evrete.api.events.KnowledgeCreatedEvent;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.WeakHashMap;

public class KnowledgeRuntime extends AbstractRuntime<RuleDescriptor, Knowledge> implements Knowledge {
    private final WeakHashMap<RuleSession<?>, Object> sessions = new WeakHashMap<>();
    private final Object VALUE = new Object();
    private final SearchList<KnowledgeRule> ruleDescriptors = new SearchList<>();

    public KnowledgeRuntime(KnowledgeService service, String name) {
        super(service, name);
        // Publish the created event
        broadcast(KnowledgeCreatedEvent.class, () -> KnowledgeRuntime.this);
    }

    @Override
    void addRuleDescriptors(List<KnowledgeRule> descriptors) {
        if(!descriptors.isEmpty()) {
            for(KnowledgeRule rd : descriptors) {
                this.ruleDescriptors.add(rd);
            }
            this.ruleDescriptors.sort(getRuleComparator());
        }
    }

    @Override
    public List<RuleDescriptor> getRules() {
        return Collections.unmodifiableList(ruleDescriptors.getList());
    }

    List<KnowledgeRule> getRuleDescriptors() {
        return Collections.unmodifiableList(ruleDescriptors.getList());
    }

    void close(RuleSession<?> session) {
        synchronized (sessions) {
            sessions.remove(session);
        }
    }

    @Override
    void _assertActive() {

    }

    @Override
    public KnowledgeRule getRule(String name) {
        return ruleDescriptors.get(name);
    }

    @Override
    public Collection<RuleSession<?>> getSessions() {
        return Collections.unmodifiableCollection(sessions.keySet());
    }

    @Override
    public StatefulSession newStatefulSession() {
        return register(new StatefulSessionImpl(this));
    }

    @Override
    public StatelessSession newStatelessSession() {
        return register(new StatelessSessionImpl(this));
    }

    private <S extends RuleSession<S>> S register(S session) {
        sessions.put(session, VALUE);
        return session;
    }
}
