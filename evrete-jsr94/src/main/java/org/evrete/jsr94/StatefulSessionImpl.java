package org.evrete.jsr94;

import org.evrete.api.FactHandle;
import org.evrete.api.StatefulSession;

import javax.rules.*;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static javax.rules.RuleRuntime.STATEFUL_SESSION_TYPE;

public class StatefulSessionImpl extends AbstractRuleSession implements StatefulRuleSession {
    private static final long serialVersionUID = 1640888587580047958L;

    StatefulSessionImpl(StatefulSession delegate, RuleExecutionSetMetadataImpl metadata) {
        super(delegate, STATEFUL_SESSION_TYPE, metadata);
    }

    @Override
    public boolean containsObject(Handle handle) throws InvalidRuleSessionException, InvalidHandleException {
        Object o = getObject(handle);
        return o != null;
    }

    @Override
    public Handle addObject(Object o) throws RemoteException, InvalidRuleSessionException {
        FactHandle h = delegate.insert(o);
        if (h == null) {
            throw new InvalidRuleSessionException("Handle not created, inserted object is of unknown type.");
        } else {
            return new HandleImpl(h);
        }
    }

    @Override
    public List<?> addObjects(List list) throws InvalidRuleSessionException {
        if (list == null || list.isEmpty()) return HandleImpl.EMPTY_LIST;
        List<HandleImpl> handles = new ArrayList<>(list.size());
        for (Object o : list) {
            try {
                handles.add(new HandleImpl(delegate.insert(o)));
            } catch (Exception e) {
                throw new InvalidRuleSessionException(e.getMessage(), e);
            }
        }
        return handles;
    }

    @Override
    public void updateObject(Handle handle, Object o) throws InvalidRuleSessionException, InvalidHandleException {
        if (handle instanceof HandleImpl) {
            HandleImpl h = (HandleImpl) handle;
            try {
                delegate.update(h.delegate, o);
            } catch (Exception t) {
                throw new InvalidRuleSessionException(t.getMessage(), t);
            }
        } else {
            throw new InvalidHandleException("Invalid fact handle instance");
        }
    }

    @Override
    public void removeObject(Handle handle) throws InvalidHandleException, InvalidRuleSessionException {
        if (handle instanceof HandleImpl) {
            HandleImpl h = (HandleImpl) handle;
            try {
                delegate.delete(h.delegate);
            } catch (Exception t) {
                throw new InvalidRuleSessionException(t.getMessage(), t);
            }
        } else {
            throw new InvalidHandleException("Invalid fact handle instance");
        }
    }

    @Override
    public List<?> getObjects() throws InvalidRuleSessionException {
        return Utils.sessionObjects(delegate);
    }

    @Override
    public List<Handle> getHandles() {
        List<Handle> list = new LinkedList<>();
        delegate.forEachFact((handle, o) -> list.add(new HandleImpl(handle)));
        return list;
    }

    @Override
    public List<?> getObjects(ObjectFilter objectFilter) throws InvalidRuleSessionException {
        return getObjects()
                .stream()
                .map(objectFilter::filter)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public void executeRules() throws InvalidRuleSessionException {
        try {
            delegate.fire();
        } catch (Exception e) {
            throw new InvalidRuleSessionException(e.getMessage(), e);
        }
    }

    @Override
    public void reset() throws InvalidRuleSessionException {
        try {
            delegate.clear();
        } catch (Exception e) {
            throw new InvalidRuleSessionException(e.getMessage(), e);
        }
    }

    @Override
    public Object getObject(Handle handle) throws InvalidHandleException, InvalidRuleSessionException {
        if (handle instanceof HandleImpl) {
            HandleImpl h = (HandleImpl) handle;
            try {
                return delegate.getFact(h.delegate);
            } catch (Exception t) {
                throw new InvalidRuleSessionException(t.getMessage(), t);
            }
        } else {
            throw new InvalidHandleException("Invalid fact handle");
        }
    }

    private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
        throw new UnsupportedOperationException("Serialization not supported");
    }

}
