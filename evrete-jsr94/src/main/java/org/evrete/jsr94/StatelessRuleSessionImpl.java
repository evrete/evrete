package org.evrete.jsr94;

import org.evrete.api.StatefulSession;

import javax.rules.InvalidRuleSessionException;
import javax.rules.ObjectFilter;
import javax.rules.RuleRuntime;
import javax.rules.StatelessRuleSession;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the {@link StatelessRuleSession} interface.
 */
public class StatelessRuleSessionImpl extends AbstractRuleSessionBase implements StatelessRuleSession {

    StatelessRuleSessionImpl(StatefulSession delegate, RuleExecutionSetMetadataImpl metadata) {
        super(delegate, RuleRuntime.STATELESS_SESSION_TYPE, metadata);
    }

    @Override
    public List<?> executeRules(List list) throws InvalidRuleSessionException {
        try {
            for (Object o : list) {
                delegate.insert0(o, false);
            }
            delegate.fire();
            List<?> objects = Utils.sessionObjects(delegate);
            delegate.clear();
            return objects;
        } catch (Exception e) {
            throw new InvalidRuleSessionException("Session in invalid state", e);
        }
    }

    @Override
    public List<?> executeRules(List list, ObjectFilter objectFilter) throws InvalidRuleSessionException {
        List<?> collected = executeRules(list);
        List<Object> result = new ArrayList<>(collected.size());
        for(Object o : collected) {
            Object filtered = objectFilter.filter(o);
            if(filtered != null) {
                result.add(filtered);
            }
        }
        return result;
    }
}
