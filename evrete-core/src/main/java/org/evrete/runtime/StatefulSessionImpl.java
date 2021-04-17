package org.evrete.runtime;

import org.evrete.api.ActivationManager;
import org.evrete.api.Named;
import org.evrete.api.RuntimeRule;
import org.evrete.api.StatefulSession;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.function.BooleanSupplier;

public class StatefulSessionImpl extends AbstractRuleSession<StatefulSession> implements StatefulSession {

    StatefulSessionImpl(KnowledgeRuntime knowledge) {
        super(knowledge);
    }

    @Override
    public StatefulSession setActivationManager(ActivationManager activationManager) {
        applyActivationManager(activationManager);
        return this;
    }

    @Override
    public StatefulSession setFireCriteria(BooleanSupplier fireCriteria) {
        applyFireCriteria(fireCriteria);
        return this;
    }

    @Override
    public StatefulSession appendDslRules(String dsl, InputStream... streams) throws IOException {
        append(dsl, streams);
        return this;
    }

    @Override
    public StatefulSession appendDslRules(String dsl, URL... resources) throws IOException {
        append(dsl, resources);
        return this;
    }

    @Override
    public StatefulSession appendDslRules(String dsl, Reader... readers) throws IOException {
        append(dsl, readers);
        return this;
    }

    @Override
    public StatefulSession appendDslRules(String dsl, Class<?> classes) throws IOException {
        append(dsl, classes);
        return this;
    }

    @Override
    public RuntimeRule getRule(String name) {
        //TODO !!! create a map for that
        return Named.find(getRules(), name);
    }
}
