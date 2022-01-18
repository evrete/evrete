package org.evrete.jsr94;

import org.evrete.api.StatefulSession;

import javax.rules.InvalidRuleSessionException;
import javax.rules.ObjectFilter;
import javax.rules.RuleRuntime;
import javax.rules.StatelessRuleSession;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class StatelessRuleSessionImpl extends AbstractRuleSessionBase implements StatelessRuleSession {

    StatelessRuleSessionImpl(StatefulSession delegate, RuleExecutionSetMetadataImpl metadata) {
        super(delegate, RuleRuntime.STATELESS_SESSION_TYPE, metadata);
    }

    @Override
    public List<?> executeRules(List list) throws InvalidRuleSessionException {
        try {
            delegate.insert(list);
            delegate.fire();
            List<?> objects = Utils.sessionObjects(delegate);
            delegate.clear();
            return objects;
        } catch (Exception e) {
            throw new InvalidRuleSessionException("Session in invalid state", e);
        }
    }

    @Override
    public List<?> executeRules(List list, ObjectFilter objectFilter) throws InvalidRuleSessionException, RemoteException {
        return executeRules(list)
                .stream()
                .map(objectFilter::filter)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
