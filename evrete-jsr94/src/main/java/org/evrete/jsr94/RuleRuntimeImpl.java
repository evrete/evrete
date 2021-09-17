package org.evrete.jsr94;

import org.evrete.api.StatefulSession;

import javax.rules.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;

class RuleRuntimeImpl implements RuleRuntime {
    private static final long serialVersionUID = -1999541474405198310L;
    private final RuleSetRegistrations registrations;

    RuleRuntimeImpl(RuleSetRegistrations registrations) {
        this.registrations = registrations;
    }

    @Override
    public RuleSession createRuleSession(String s, Map map, int i) throws RuleSessionTypeUnsupportedException, RuleSessionCreateException, RuleExecutionSetNotFoundException {
        RuleExecutionSetImpl set = registrations.getChecked(s);
        StatefulSession delegate;
        RuleExecutionSetMetadataImpl metadata;
        try {
            delegate = set.getKnowledge().newStatefulSession();
            Utils.copyConfiguration(delegate, map);
            metadata = new RuleExecutionSetMetadataImpl(s, delegate);
        } catch (Exception e) {
            throw new RuleSessionCreateException(e.getMessage(), e);
        }
        switch (i) {
            case STATEFUL_SESSION_TYPE:
                return new StatefulSessionImpl(delegate, metadata);
            case STATELESS_SESSION_TYPE:
                return new StatelessSessionImpl(delegate, metadata);
            default:
                throw new RuleSessionTypeUnsupportedException("Session type " + i + " is not supported. Supported values are [" + STATEFUL_SESSION_TYPE + "] (stateful session) and [" + STATELESS_SESSION_TYPE + "] (stateless session)");
        }
    }

    @Override
    public List<?> getRegistrations() {
        return registrations.getKeys();
    }

    private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
        throw new UnsupportedOperationException("Serialization not supported");
    }

}
